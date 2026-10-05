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
    @ObservationIgnored private let waitingForDispositionStore: WaitingForSourceDispositionStore
    @ObservationIgnored private let waitingForCompletionStatus: (UUID) -> Bool

    var settings: AppSettings
    var deepLinkTaskId: String?
    var requestedQuickAddLocation: TaskLocation?
    var endMyDayRequested = false
    var pendingUndo: UndoState?
    var rolloverReview: RolloverOutcome?
    var suiteHandoffError: String?
    var sourceDispositionError: String?
    var waitingForDisposition: WaitingForSourceDisposition?
    /// Completions during this app session on Today (used for the progress label).
    var sessionCompletedCount = 0

    struct UndoState: Equatable {
        let completionEventId: String
        let title: String
        let restoreTo: TaskLocation
    }

    init(
        inMemory: Bool = false,
        waitingForDispositionStore: WaitingForSourceDispositionStore? = nil,
        waitingForCompletionStatus: ((UUID) -> Bool)? = nil
    ) {
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
        let defaults = inMemory
            ? UserDefaults(suiteName: "todaylist.tests.\(UUID().uuidString)")!
            : .standard
        let settingsRepo = UserDefaultsSettingsRepository(defaults: defaults)
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
        self.waitingForDispositionStore = waitingForDispositionStore
            ?? WaitingForSourceDispositionStore(defaults: defaults)
        self.waitingForCompletionStatus = waitingForCompletionStatus
            ?? SuiteHandoffStore.isCompleted
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
            let importItem = try SuiteHandoffStore.todayListImport(from: payload)

            _ = try await taskRepository.createTaskFromHandoff(
                handoffID: id,
                title: importItem.title,
                notes: importItem.notes,
                location: importItem.location
            )
            UserDefaults.standard.set(true, forKey: key)
            try? SuiteHandoffStore.markCompleted(id: id)
            await refreshWidget()
            return importItem.location
        } catch {
            suiteHandoffError = error.localizedDescription
            return nil
        }
    }

    func refreshWaitingForDisposition() async {
        if let current = waitingForDisposition,
           waitingForDispositionStore.contains(handoffID: current.handoffID) {
            return
        }

        for record in waitingForDispositionStore.pendingRecords() {
            guard waitingForCompletionStatus(record.handoffID) else { continue }
            if await taskRepository.getTask(id: record.sourceTaskID) == nil {
                waitingForDispositionStore.resolve(handoffID: record.handoffID)
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
        guard waitingForDispositionStore.contains(handoffID: disposition.handoffID) else {
            waitingForDisposition = nil
            return
        }
        guard let task = await taskRepository.getTask(id: disposition.sourceTaskID) else {
            await finishWaitingForDisposition(disposition)
            return
        }

        switch action {
        case .keep:
            await finishWaitingForDisposition(disposition)
        case .markDone:
            if await completeTask(task) {
                await finishWaitingForDisposition(disposition)
            }
        case .remove:
            do {
                await notificationScheduler.cancelReminder(taskId: task.id)
                try await taskRepository.deleteTask(taskId: task.id, scope: .thisTask)
                await refreshWidget()
                await finishWaitingForDisposition(disposition)
            } catch {
                sourceDispositionError = "The Today List task could not be removed. Please try again."
            }
        }
    }

    @discardableResult
    func completeTask(_ task: TaskItem) async -> Bool {
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
                return true
            }
        } catch {
            Analytics.log("complete_failed", parameters: ["error": error.localizedDescription])
        }
        return false
    }

    private func finishWaitingForDisposition(_ disposition: WaitingForSourceDisposition) async {
        waitingForDispositionStore.resolve(handoffID: disposition.handoffID)
        waitingForDisposition = nil
        await Task.yield()
        await refreshWaitingForDisposition()
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
