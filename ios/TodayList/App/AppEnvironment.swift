import Foundation
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
        let outcome = await rolloverManager.evaluate()
        if case .needsReview = outcome {
            rolloverReview = outcome
        }
        Task {
            for await s in settingsRepository.observeSettings() {
                await MainActor.run { self.settings = s }
            }
        }
    }

    func importSuiteHandoff(id: UUID) async -> TaskLocation? {
        let key = "suite.handoff.processed.\(id.uuidString)"
        if UserDefaults.standard.bool(forKey: key) {
            return nil
        }

        do {
            let payload = try SuiteHandoffStore.loadPending(id: id)
            guard payload.version == 1 else { throw SuiteHandoffError.unsupportedVersion }
            guard payload.sourceApp == "quickCapture",
                  payload.destinationApp == "todayList",
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
            try SuiteHandoffStore.markCompleted(id: id)
            await refreshWidget()
            return location
        } catch {
            suiteHandoffError = error.localizedDescription
            return nil
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
