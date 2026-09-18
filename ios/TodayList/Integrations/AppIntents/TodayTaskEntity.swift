import AppIntents

struct TodayTaskEntity: AppEntity, Identifiable {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "Today Task")
    static let defaultQuery = TodayTaskEntityQuery()

    let id: String
    let title: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: LocalizedStringResource(stringLiteral: title))
    }
}

struct TodayTaskEntityQuery: EntityStringQuery {
    @MainActor
    func entities(for identifiers: [TodayTaskEntity.ID]) async throws -> [TodayTaskEntity] {
        let env = AppEnvironment()
        var result: [TodayTaskEntity] = []
        for id in identifiers {
            if let task = await env.taskRepository.getTask(id: id) {
                result.append(TodayTaskEntity(id: task.id, title: task.title))
            }
        }
        return result
    }

    @MainActor
    func entities(matching string: String) async throws -> [TodayTaskEntity] {
        let needle = string.trimmingCharacters(in: .whitespacesAndNewlines)
        return await activeTasks()
            .filter { needle.isEmpty || $0.title.localizedCaseInsensitiveContains(needle) }
            .map { TodayTaskEntity(id: $0.id, title: $0.title) }
    }

    @MainActor
    func suggestedEntities() async throws -> [TodayTaskEntity] {
        await activeTasks().map { TodayTaskEntity(id: $0.id, title: $0.title) }
    }

    @MainActor
    private func activeTasks() async -> [TaskItem] {
        let env = AppEnvironment()
        var today: [TaskItem] = []
        var later: [TaskItem] = []

        for await tasks in env.taskRepository.observeTasks(location: .today) {
            today = tasks
            break
        }
        for await tasks in env.taskRepository.observeTasks(location: .later) {
            later = tasks
            break
        }
        return today + later
    }
}
