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
    let billing: StoreKitBilling

    var settings: AppSettings
    var deepLinkTaskId: String?
    var pendingUndo: UndoState?
    var rolloverReview: RolloverOutcome?

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
        self.billing = StoreKitBilling(settingsRepository: settingsRepo)
        self.settings = settingsRepo.currentSettings()
    }

    func bootstrap() async {
        settings = settingsRepository.currentSettings()
        _ = billing.listenForTransactions()
        await billing.loadProducts()
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

    func completeTask(_ task: TaskItem) async {
        do {
            if let eventId = try await taskRepository.completeTask(taskId: task.id) {
                await notificationScheduler.cancelReminder(taskId: task.id)
                pendingUndo = UndoState(
                    completionEventId: eventId,
                    title: task.title,
                    restoreTo: task.location
                )
                historyRepository.notifyCompletionsChanged()
                await self.refreshWidget()
                if settings.hapticsEnabled {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                }
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
            historyRepository.notifyCompletionsChanged()
            await refreshWidget()
            pendingUndo = nil
        } catch {
            Analytics.log("undo_failed")
        }
    }

    func refreshWidget() async {
        for await tasks in taskRepository.observeTasks(location: .today) {
            WidgetSnapshot.publish(todayTitles: tasks.map(\.title))
            break
        }
    }
}

import UIKit

private struct AppEnvironmentKey: EnvironmentKey {
    @MainActor static let defaultValue = AppEnvironment(inMemory: true)
}

extension EnvironmentValues {
    @MainActor
    var appEnvironment: AppEnvironment {
        get { self[AppEnvironmentKey.self] }
        set { self[AppEnvironmentKey.self] = newValue }
    }
}
