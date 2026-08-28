import Foundation

@MainActor
final class RecreateTaskFromHistoryUseCase {
    private let historyRepository: HistoryRepository
    private let taskRepository: TaskRepository

    init(historyRepository: HistoryRepository, taskRepository: TaskRepository) {
        self.historyRepository = historyRepository
        self.taskRepository = taskRepository
    }

    func execute(completionId: String, location: TaskLocation) async throws -> TaskItem? {
        guard let record = await historyRepository.getCompletion(id: completionId) else { return nil }
        return try await taskRepository.createTask(
            title: record.titleSnapshot,
            notes: nil,
            location: location,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
    }
}
