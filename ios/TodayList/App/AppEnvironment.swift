import Foundation
import ProductivitySuiteCore
import SwiftData
import SwiftUI

@MainActor
@Observable
final class AppEnvironment {
    let modelContainer: ModelContainer
    let taskRepository: SwiftDataTaskRepository
    let historyRepository: SwiftDataHistoryRepository
    let settingsRepository: UserDefaultsSettingsRepository
    let notificationScheduler: NotificationScheduler
    let rolloverManager: RolloverManager
    let recreateFromHistory: RecreateTaskFromHistoryUseCase

    var settings: AppSettings
    var deepLinkTaskId: String?
    var requestedQuickAddLocation: TaskLocation?
    var endMyDayRequested = false
    var pendingUndo: UndoState?
    var rolloverReview: RolloverOutcome?
    var suiteHandoffError: String?
    var waitingForDisposition: WaitingForSourceDisposition?
    /// Completions during this app session on Today (used for the progress label).
    var sessionCompletedCount = 0

    struct UndoState: Equatable {
        let completionEventId: String
        let title: String
        let restoreTo: TaskLocation
    }

    init(inMemory: Bool = false) {
        let schema = Schema([
            PersistedTask.self,
            PersistedRecurrence.self,
            PersistedOccurrence.self,
            PersistedCompletion.self,
        ])
        let config = ModelConfiguration(isStoredInMemoryOnly: inMemory)
        let container = try! ModelContainer(for: schema, configurations: config)
        self.modelContainer = container
        let context = ModelContext(container)
        let tasks = SwiftDataTaskRepository(modelContext: context)
        let history = SwiftDataHistoryRepository(modelContext: context)
        let settingsRepo = UserDefaultsSettingsRepository(
            defaults: inMemory
                ? UserDefaults(suiteName: "todaylist.tests.\(UUID().uuidString)")!
                : .standard
        )
        self.taskRepository = tasks
        self.historyRepository = history
        self.settingsRepository = settingsRepo
        self.notificationScheduler = UserNotificationScheduler()
        self.rolloverManager = RolloverManager(
            taskRepository: tasks,
            settingsRepository: settingsRepo
        )
        self.recreateFromHistory = RecreateTaskFromHistoryUseCase(
            historyRepository: history,
            taskRepository: tasks
        )
        self.settings = settingsRepo.currentSettings()
    }

    func bootstrap() async {
        PhoneWatchBridge.shared.configure(
            repository: taskRepository,
            historyRepository: historyRepository,
            notificationScheduler: notificationScheduler
        )
        settings = settingsRepository.currentSettings()
        if ProcessInfo.processInfo.arguments.contains("-ScreenshotDemo") {
            await seedScreenshotDemo()
        }
        let outcome = await rolloverManager.evaluate()
        if case .needsReview = outcome {
            rolloverReview = outcome
        }
        await refreshWaitingForDisposition()
        Task {
            for await s in settingsRepository.observeSettings() {
                await MainActor.run { self.settings = s }
            }
        }
    }

    /// Populates deterministic demo content for App Store simulator screenshots.
    func seedScreenshotDemo() async {
        try? await taskRepository.deleteAllTasks()
        await settingsRepository.updateSettings {
            $0.lastRolloverDate = CalendarHelpers.today()
            $0.themeMode = .light
            $0.rolloverMode = .ask
            $0.hapticsEnabled = false
        }
        settings = settingsRepository.currentSettings()

        do {
            let finished = try await taskRepository.createTask(
                title: "Morning stretch",
                notes: nil,
                location: .today,
                reminderAt: nil,
                scheduledDate: CalendarHelpers.today(),
                recurrence: nil
            )
            _ = try await taskRepository.completeTask(taskId: finished.id)
            sessionCompletedCount = 1

            _ = try await taskRepository.createTask(
                title: "Buy groceries",
                notes: "Milk, eggs, sourdough",
                location: .today,
                reminderAt: nil,
                scheduledDate: CalendarHelpers.today(),
                recurrence: nil
            )
            _ = try await taskRepository.createTask(
                title: "Call dentist",
                notes: nil,
                location: .today,
                reminderAt: nil,
                scheduledDate: CalendarHelpers.today(),
                recurrence: nil
            )
            _ = try await taskRepository.createTask(
                title: "Review budget",
                notes: "https://www.4ctech.io/today-list/",
                location: .today,
                reminderAt: nil,
                scheduledDate: CalendarHelpers.today(),
                recurrence: RecurrenceRule.from(option: .weekdays)
            )
            _ = try await taskRepository.createTask(
                title: "Plan weekend trip",
                notes: "Look at train times",
                location: .later,
                reminderAt: nil,
                scheduledDate: nil,
                recurrence: nil
            )
            _ = try await taskRepository.createTask(
                title: "Read design notes",
                notes: nil,
                location: .later,
                reminderAt: nil,
                scheduledDate: nil,
                recurrence: nil
            )
            historyRepository.notifyCompletionsChanged()
        } catch {
            Analytics.log("screenshot_seed_failed", parameters: ["error": error.localizedDescription])
        }
    }

    func importSuiteHandoff(id: UUID) async -> TaskLocation? {
        let key = "suite.handoff.processed.\(id.uuidString)"
        if UserDefaults.standard.bool(forKey: key) {
            try? SuiteHandoffStore.markCompleted(id: id)
            return nil
        }

        do {
            let payload = try SuiteHandoffStore.loadPending(id: id)
            guard payload.version == SuiteHandoffConstants.schemaVersion else {
                throw SuiteHandoffError.unsupportedVersion
            }
            guard payload.sourceApp == .quickCapture,
                  payload.destinationApp == .todayList,
                  !payload.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                throw SuiteHandoffError.invalidPayload
            }

            let location: TaskLocation = payload.metadata["destination"] == "later" ? .later : .today
            _ = try await taskRepository.createTask(
                title: payload.title,
                notes: payload.notes,
                location: location,
                reminderAt: nil,
                scheduledDate: location == .today ? CalendarHelpers.today() : nil,
                recurrence: nil
            )
            UserDefaults.standard.set(true, forKey: key)
            try? SuiteHandoffStore.markCompleted(id: id)
            await refreshWidget()
            return location
        } catch {
            suiteHandoffError = error.localizedDescription
            return nil
        }
    }

    func refreshWaitingForDisposition() async {
        let store = WaitingForSourceDispositionStore()
        for record in store.pendingRecords() {
            guard SuiteHandoffStore.isCompleted(id: record.handoffID) else { continue }
            if await taskRepository.getTask(id: record.sourceTaskID) == nil {
                store.resolve(handoffID: record.handoffID)
                continue
            }
            waitingForDisposition = record
            return
        }
        waitingForDisposition = nil
    }

    func resolveWaitingForDisposition(
        _ disposition: WaitingForSourceDisposition,
        action: WaitingForSourceAction
    ) async {
        let store = WaitingForSourceDispositionStore()
        defer {
            store.resolve(handoffID: disposition.handoffID)
            waitingForDisposition = nil
        }

        guard let task = await taskRepository.getTask(id: disposition.sourceTaskID) else { return }

        switch action {
        case .keep:
            return
        case .markDone:
            await completeTask(task)
        case .remove:
            await notificationScheduler.cancelReminder(taskId: task.id)
            try? await taskRepository.deleteTask(taskId: task.id, scope: .thisTask)
            await refreshWidget()
        }
    }

    func completeTask(_ task: TaskItem) async {
        do {
            if let eventId = try await taskRepository.completeTask(taskId: task.id) {
                pendingUndo = UndoState(
                    completionEventId: eventId,
                    title: task.title,
                    restoreTo: task.location
                )
                if task.location == .today {
                    sessionCompletedCount += 1
                }
                if settings.hapticsEnabled {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                }
                historyRepository.notifyCompletionsChanged()
                await notificationScheduler.cancelReminder(taskId: task.id)
                await refreshWidget()
            }
        } catch {
            Analytics.log("complete_failed", parameters: ["error": error.localizedDescription])
        }
    }

    func undoPending() async {
        guard let pending = pendingUndo else { return }
        do {
            try await taskRepository.uncompleteTask(
                completionEventId: pending.completionEventId,
                restoreTo: pending.restoreTo
            )
            if pending.restoreTo == .today {
                sessionCompletedCount = max(0, sessionCompletedCount - 1)
            }
            historyRepository.notifyCompletionsChanged()
            await refreshWidget()
            pendingUndo = nil
        } catch {
            Analytics.log("undo_failed")
        }
    }

    func refreshWidget() async {
        var activeTasks: [TaskItem] = []
        for await tasks in taskRepository.observeTasks(location: .today) {
            activeTasks = tasks
            WidgetSnapshot.publish(todayTitles: tasks.map(\.title))
            break
        }

        var completedToday: [CompletionRecord] = []
        for await records in historyRepository.observeCompletions() {
            completedToday = records.filter {
                Calendar.current.isDate($0.completionDate, inSameDayAs: Date())
            }
            break
        }

        PhoneWatchBridge.shared.publishSnapshot(
            activeTasks: activeTasks,
            completedToday: completedToday
        )
    }
}

import UIKit
