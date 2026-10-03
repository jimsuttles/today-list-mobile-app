import Foundation

struct WaitingForSourceDisposition: Identifiable, Equatable {
    let handoffID: UUID
    let sourceTaskID: String

    var id: UUID { handoffID }
}

struct WaitingForSourceDispositionStore {
    private static let key = "suite.handoff.waitingFor.pendingSourceDisposition.v1"
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func recordPending(handoffID: UUID, sourceTaskID: String) {
        var records = storedRecords()
        records[handoffID.uuidString] = sourceTaskID
        defaults.set(records, forKey: Self.key)
    }

    func pendingRecords() -> [WaitingForSourceDisposition] {
        storedRecords()
            .compactMap { key, value in
                guard let id = UUID(uuidString: key) else { return nil }
                return WaitingForSourceDisposition(handoffID: id, sourceTaskID: value)
            }
            .sorted { $0.handoffID.uuidString < $1.handoffID.uuidString }
    }

    func resolve(handoffID: UUID) {
        var records = storedRecords()
        records.removeValue(forKey: handoffID.uuidString)
        defaults.set(records, forKey: Self.key)
    }

    func contains(handoffID: UUID) -> Bool {
        storedRecords()[handoffID.uuidString] != nil
    }

    private func storedRecords() -> [String: String] {
        defaults.dictionary(forKey: Self.key)?
            .compactMapValues { $0 as? String } ?? [:]
    }
}
