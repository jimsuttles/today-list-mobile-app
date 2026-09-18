import Foundation
import WatchConnectivity
import WidgetKit

struct WatchTodayItem: Codable, Identifiable, Equatable {
    let id: String
    let title: String
    let isCompleted: Bool
    let completionEventId: String?
}

private struct WatchTodaySnapshot: Codable {
    let generatedAt: Date
    let items: [WatchTodayItem]
}

private struct WatchTodayOperation: Codable, Identifiable {
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
final class WatchTodayStore: NSObject, ObservableObject {
    static let shared = WatchTodayStore()

    @Published private(set) var items: [WatchTodayItem] = []
    @Published private(set) var pendingCount = 0

    private let pendingKey = "today_watch_pending_operations"
    private let snapshotKey = "today_watch_snapshot"
    private let suiteName = "group.com.fourctech.todaylist"

    private override init() {
        super.init()
        restoreSnapshot()
        pendingCount = pendingOperations().count

        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }

    var remainingCount: Int {
        items.filter { !$0.isCompleted }.count
    }

    func addToday(title: String) {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        enqueue(
            WatchTodayOperation(
                id: UUID(),
                kind: .addToday,
                taskId: nil,
                completionEventId: nil,
                title: trimmed,
                createdAt: Date()
            )
        )
    }

    func toggle(_ item: WatchTodayItem) {
        let operation = WatchTodayOperation(
            id: UUID(),
            kind: item.isCompleted ? .uncomplete : .complete,
            taskId: item.id,
            completionEventId: item.completionEventId,
            title: nil,
            createdAt: Date()
        )
        enqueue(operation)
    }

    private func enqueue(_ operation: WatchTodayOperation) {
        var pending = pendingOperations()
        pending.append(operation)
        savePending(pending)
        transfer(operation)
    }

    private func transfer(_ operation: WatchTodayOperation) {
        guard WCSession.isSupported(),
              let data = try? JSONEncoder().encode(operation) else { return }
        WCSession.default.transferUserInfo(["todayOperation": data])
    }

    private func retryPending() {
        for operation in pendingOperations() {
            transfer(operation)
        }
    }

    private func apply(snapshot: WatchTodaySnapshot) {
        items = snapshot.items
        if let data = try? JSONEncoder().encode(snapshot) {
            UserDefaults.standard.set(data, forKey: snapshotKey)
        }

        let defaults = UserDefaults(suiteName: suiteName)
        defaults?.set(remainingCount, forKey: "watch_remaining_count")
        WidgetCenter.shared.reloadAllTimelines()
    }

    private func acknowledge(_ id: UUID) {
        let pending = pendingOperations().filter { $0.id != id }
        savePending(pending)
    }

    private func pendingOperations() -> [WatchTodayOperation] {
        guard let data = UserDefaults.standard.data(forKey: pendingKey),
              let operations = try? JSONDecoder().decode([WatchTodayOperation].self, from: data) else {
            return []
        }
        return operations
    }

    private func savePending(_ operations: [WatchTodayOperation]) {
        if let data = try? JSONEncoder().encode(operations) {
            UserDefaults.standard.set(data, forKey: pendingKey)
        }
        pendingCount = operations.count
    }

    private func restoreSnapshot() {
        guard let data = UserDefaults.standard.data(forKey: snapshotKey),
              let snapshot = try? JSONDecoder().decode(WatchTodaySnapshot.self, from: data) else {
            return
        }
        items = snapshot.items
    }
}

extension WatchTodayStore: WCSessionDelegate {
    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        guard activationState == .activated else { return }
        Task { @MainActor in
            self.retryPending()
        }
    }

    nonisolated func session(
        _ session: WCSession,
        didReceiveApplicationContext applicationContext: [String: Any]
    ) {
        guard let data = applicationContext["todaySnapshot"] as? Data,
              let snapshot = try? JSONDecoder().decode(WatchTodaySnapshot.self, from: data) else {
            return
        }

        Task { @MainActor in
            self.apply(snapshot: snapshot)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        guard let data = userInfo["todayAck"] as? Data,
              let id = try? JSONDecoder().decode(UUID.self, from: data) else {
            return
        }

        Task { @MainActor in
            self.acknowledge(id)
        }
    }
}
