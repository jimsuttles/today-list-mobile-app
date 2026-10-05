import Foundation

struct WaitingForSourceDisposition: Codable, Identifiable, Equatable {
    let handoffID: UUID
    let sourceTaskID: String
    let attempt: Int
    let createdAt: Date
    var destinationOpenAccepted: Bool

    var id: UUID { handoffID }
}

struct WaitingForSourceDispositionStore {
    private static let recordsKey = "suite.handoff.waitingFor.sourceDispositions.v2"
    private static let attemptsKey = "suite.handoff.waitingFor.nextAttempts.v1"
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func prepareAttempt(sourceTaskID: String, createdAt: Date = Date()) -> WaitingForSourceDisposition {
        if let existing = storedRecords().first(where: { $0.sourceTaskID == sourceTaskID }) {
            return existing
        }

        let attempt = nextAttempts()[sourceTaskID] ?? 1
        let record = WaitingForSourceDisposition(
            handoffID: SuiteHandoffStore.waitingForHandoffID(
                sourceTaskID: sourceTaskID,
                attempt: attempt
            ),
            sourceTaskID: sourceTaskID,
            attempt: attempt,
            createdAt: createdAt,
            destinationOpenAccepted: false
        )
        var records = storedRecords()
        records.append(record)
        save(records)
        return record
    }

    func markDestinationOpenAccepted(handoffID: UUID) {
        var records = storedRecords()
        guard let index = records.firstIndex(where: { $0.handoffID == handoffID }) else { return }
        records[index].destinationOpenAccepted = true
        save(records)
    }

    func pendingRecords() -> [WaitingForSourceDisposition] {
        storedRecords()
            .filter(\.destinationOpenAccepted)
            .sorted {
                if $0.createdAt == $1.createdAt {
                    return $0.handoffID.uuidString < $1.handoffID.uuidString
                }
                return $0.createdAt < $1.createdAt
            }
    }

    func resolve(handoffID: UUID) {
        var records = storedRecords()
        guard let record = records.first(where: { $0.handoffID == handoffID }) else { return }
        records.removeAll { $0.handoffID == handoffID }
        save(records)

        var attempts = nextAttempts()
        attempts[record.sourceTaskID] = record.attempt + 1
        defaults.set(attempts, forKey: Self.attemptsKey)
    }

    func contains(handoffID: UUID) -> Bool {
        storedRecords().contains { $0.handoffID == handoffID }
    }

    private func storedRecords() -> [WaitingForSourceDisposition] {
        guard let data = defaults.data(forKey: Self.recordsKey) else { return [] }
        return (try? JSONDecoder().decode([WaitingForSourceDisposition].self, from: data)) ?? []
    }

    private func nextAttempts() -> [String: Int] {
        defaults.dictionary(forKey: Self.attemptsKey)?
            .compactMapValues { $0 as? Int } ?? [:]
    }

    private func save(_ records: [WaitingForSourceDisposition]) {
        guard let data = try? JSONEncoder().encode(records) else { return }
        defaults.set(data, forKey: Self.recordsKey)
    }
}
