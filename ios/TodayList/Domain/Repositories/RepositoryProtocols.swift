import Foundation

@MainActor
protocol TaskRepository: AnyObject {
    func observeTasks(location: TaskLocation) -> AsyncStream<[TaskItem]>
    func getTask(id: String) async -> TaskItem?
    func createTask(
        title: String,
        notes: String?,
        location: TaskLocation,
        reminderAt: Date?,
        scheduledDate: Date?,
        recurrence: RecurrenceRule?
    ) async throws -> TaskItem
    func updateTask(_ task: TaskItem) async throws
    func moveToToday(taskId: String) async throws
    func moveToLater(taskId: String) async throws
    func reorderTasks(location: TaskLocation, orderedTaskIds: [String]) async throws
    func completeTask(taskId: String) async throws -> String?
    func uncompleteTask(completionEventId: String, restoreTo: TaskLocation) async throws
    func deleteTask(taskId: String, scope: DeleteScope) async throws
    func keepOnTodayForNewDay(taskIds: [String], today: Date) async throws
    func deleteAllTasks() async throws
}

@MainActor
protocol HistoryRepository: AnyObject {
    func observeCompletions() -> AsyncStream<[CompletionRecord]>
    func getCompletion(id: String) async -> CompletionRecord?
    func clearHistory() async throws
    func notifyCompletionsChanged()
}

@MainActor
protocol SettingsRepository: AnyObject {
    func observeSettings() -> AsyncStream<AppSettings>
    func currentSettings() -> AppSettings
    func updateSettings(_ transform: (inout AppSettings) -> Void) async
    func resetPreferencesKeepingEntitlement() async
}

protocol NotificationScheduler: AnyObject {
    func scheduleReminder(taskId: String, title: String, at date: Date) async
    func cancelReminder(taskId: String) async
    func requestPermissionIfNeeded() async -> Bool
}
