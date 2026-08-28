import Foundation
import SwiftData

@MainActor
final class SwiftDataHistoryRepository: HistoryRepository {
    private let modelContext: ModelContext
    private var continuations: [UUID: AsyncStream<[CompletionRecord]>.Continuation] = [:]

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    func observeCompletions() -> AsyncStream<[CompletionRecord]> {
        AsyncStream { [weak self] continuation in
            guard let self else {
                continuation.finish()
                return
            }
            let id = UUID()
            self.continuations[id] = continuation
            continuation.yield(self.fetchAll())
            continuation.onTermination = { _ in
                Task { @MainActor in
                    self.continuations[id] = nil
                }
            }
        }
    }

    func getCompletion(id: String) async -> CompletionRecord? {
        let descriptor = FetchDescriptor<PersistedCompletion>(predicate: #Predicate { $0.id == id })
        return try? modelContext.fetch(descriptor).first?.toDomain()
    }

    func clearHistory() async throws {
        try modelContext.delete(model: PersistedCompletion.self)
        try modelContext.save()
        notify()
    }

    func notifyCompletionsChanged() {
        notify()
    }

    private func notify() {
        let items = fetchAll()
        for c in continuations.values { c.yield(items) }
    }

    private func fetchAll() -> [CompletionRecord] {
        let descriptor = FetchDescriptor<PersistedCompletion>(
            sortBy: [SortDescriptor(\.completedAt, order: .reverse)]
        )
        return ((try? modelContext.fetch(descriptor)) ?? []).map { $0.toDomain() }
    }
}
