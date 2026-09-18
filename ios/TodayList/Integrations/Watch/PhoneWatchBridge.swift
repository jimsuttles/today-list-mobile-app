import Foundation
import WatchConnectivity

struct TodayWatchItem: Codable, Identifiable, Equatable {
    let id: String
    let title: String
    let isCompleted: Bool
    let completionEventId: String?
}

struct TodayWatchSnapshot: Codable, Equatable {
    let generatedAt: Date
    let items: [TodayWatchItem]
}

struct TodayWatchOperation: Codable {
    enum Kind: String, Codable {
        case addToday
        case complete
        case uncomplete
    }

    let id: UUID
    let kind: Kind
    let taskId: String?
    let completionEventId: String?
    let title: String?
    let createdAt: Date
}

@MainActor
final class PhoneWatchBridge: NSObject {
    static let shared = PhoneWatchBridge()

    private var repository: SwiftDataTaskRepository?
    private var historyRepository: SwiftDataHistoryRepository?
    private var notificationScheduler: NotificationScheduler?
    private var handledOperationIDs = Set<UUID>()

    private override init() {
        super.init()
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }

    func configure(
        repository: SwiftDataTaskRepository,
        historyRepository: SwiftDataHistoryRepository,
        notificationScheduler: NotificationScheduler
    ) {
        self.repository = repository
        self.historyRepository = historyRepository
        self.notificationScheduler = notificationScheduler
    }

    func publishSnapshot(activeTasks: [TaskItem], completedToday: [CompletionRecord]) {
        guard WCSession.isSupported() else { return }

        let active = activeTasks.map {
            TodayWatchItem(
                id: $0.id,
                title: $0.title,
                isCompleted: false,
                completionEventId: nil
            )
        }

        let completed = completedToday.compactMap { completion -> TodayWatchItem? in
            guard let taskId = completion.taskId else { return nil }
            return TodayWatchItem(
                id: taskId,
                title: completion.titleSnapshot,
                isCompleted: true,
                completionEventId: completion.id
            )
        }

        let snapshot = TodayWatchSnapshot(
            generatedAt: Date(),
            items: active + completed
        )

        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        try? WCSession.default.updateApplicationContext(["todaySnapshot": data])
    }

    private func process(_ operation: TodayWatchOperation) async {
        guard !handledOperationIDs.contains(operation.id),
              let repository else {
            acknowledge(operation.id)
            return
        }

        do {
            switch operation.kind {
            case .addToday:
                guard let title = operation.title?.trimmingCharacters(in: .whitespacesAndNewlines),
                      !title.isEmpty else {
                    acknowledge(operation.id)
                    return
                }
                _ = try await repository.createTask(
                    title: title,
                    notes: nil,
                    location: .today,
                    reminderAt: nil,
                    scheduledDate: nil,
                    recurrence: nil
                )

            case .complete:
                guard let taskId = operation.taskId,
                      let task = await repository.getTask(id: taskId) else {
                    acknowledge(operation.id)
                    return
                }
                _ = try await repository.completeTask(taskId: task.id)
                historyRepository?.notifyCompletionsChanged()
                await notificationScheduler?.cancelReminder(taskId: task.id)

            case .uncomplete:
                guard let completionEventId = operation.completionEventId else {
                    acknowledge(operation.id)
                    return
                }
                try await repository.uncompleteTask(
                    completionEventId: completionEventId,
                    restoreTo: .today
                )
                historyRepository?.notifyCompletionsChanged()
            }

            handledOperationIDs.insert(operation.id)
            acknowledge(operation.id)
            await publishCurrentSnapshot()
        } catch {
            Analytics.log("watch_operation_failed", parameters: ["error": error.localizedDescription])
        }
    }

    private func publishCurrentSnapshot() async {
        guard let repository, let historyRepository else { return }

        var active: [TaskItem] = []
        for await tasks in repository.observeTasks(location: .today) {
            active = tasks
            break
        }

        var completed: [CompletionRecord] = []
        for await records in historyRepository.observeCompletions() {
            completed = records.filter {
                Calendar.current.isDate($0.completionDate, inSameDayAs: Date())
            }
            break
        }

        publishSnapshot(activeTasks: active, completedToday: completed)
        WidgetSnapshot.publish(todayTitles: active.map(\.title))
    }

    private func acknowledge(_ id: UUID) {
        guard let data = try? JSONEncoder().encode(id) else { return }
        WCSession.default.transferUserInfo(["todayAck": data])
    }
}

extension PhoneWatchBridge: WCSessionDelegate {
    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {}

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        guard let data = userInfo["todayOperation"] as? Data,
              let operation = try? JSONDecoder().decode(TodayWatchOperation.self, from: data) else {
            return
        }

        Task { @MainActor in
            await self.process(operation)
        }
    }
}
