import AppIntents
import Foundation

struct AddTodayItemIntent: AppIntent {
    static let title: LocalizedStringResource = "Add Today Item"
    static let description = IntentDescription("Add a task to Today List for today.")

    @Parameter(title: "Task")
    var task: String

    static var parameterSummary: some ParameterSummary {
        Summary("Add \\(.$task) to Today")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let title = task.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else {
            return .result(dialog: "Enter a task to add.")
        }

        let env = AppEnvironment()
        _ = try await env.taskRepository.createTask(
            title: title,
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        await env.refreshWidget()
        return .result(dialog: "Added to Today.")
    }
}

struct AddLaterItemIntent: AppIntent {
    static let title: LocalizedStringResource = "Add Later Item"
    static let description = IntentDescription("Add a task to Today List for later.")

    @Parameter(title: "Task")
    var task: String

    static var parameterSummary: some ParameterSummary {
        Summary("Add \\(.$task) to Later")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let title = task.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else {
            return .result(dialog: "Enter a task to add.")
        }

        let env = AppEnvironment()
        _ = try await env.taskRepository.createTask(
            title: title,
            notes: nil,
            location: .later,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        return .result(dialog: "Added to Later.")
    }
}


struct CompleteItemIntent: AppIntent {
    static let title: LocalizedStringResource = "Complete Item"
    static let description = IntentDescription("Complete an active Today List task.")

    @Parameter(title: "Task")
    var item: TodayTaskEntity

    static var parameterSummary: some ParameterSummary {
        Summary("Complete \\(.$item)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let env = AppEnvironment()
        guard let task = await env.taskRepository.getTask(id: item.id) else {
            return .result(dialog: "That task is no longer available.")
        }
        await env.completeTask(task)
        return .result(dialog: "Completed \(task.title).")
    }
}

struct ShowTodayListIntent: AppIntent {
    static let title: LocalizedStringResource = "Show Today List"
    static let description = IntentDescription("Open Today List on today's tasks.")
    static let openAppWhenRun = true

    func perform() async throws -> some IntentResult {
        IntentRouteRequest.store("todaylist://v1/today")
        return .result()
    }
}

struct StartEndMyDayIntent: AppIntent {
    static let title: LocalizedStringResource = "Start End My Day"
    static let description = IntentDescription("Open Today List's end-of-day review.")
    static let openAppWhenRun = true

    func perform() async throws -> some IntentResult {
        IntentRouteRequest.store("todaylist://v1/end-my-day")
        return .result()
    }
}

struct TodayListShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: AddTodayItemIntent(),
            phrases: [
                "Add something to Today in \(.applicationName)",
                "Add a task to \(.applicationName)"
            ],
            shortTitle: "Add Today Item",
            systemImageName: "plus.circle"
        )

        AppShortcut(
            intent: AddLaterItemIntent(),
            phrases: [
                "Add something to Later in \(.applicationName)"
            ],
            shortTitle: "Add Later Item",
            systemImageName: "tray"
        )

        AppShortcut(
            intent: CompleteItemIntent(),
            phrases: [
                "Complete a task in \(.applicationName)"
            ],
            shortTitle: "Complete Item",
            systemImageName: "checkmark.circle"
        )

        AppShortcut(
            intent: ShowTodayListIntent(),
            phrases: [
                "Show Today in \(.applicationName)"
            ],
            shortTitle: "Show Today",
            systemImageName: "sun.max"
        )

        AppShortcut(
            intent: StartEndMyDayIntent(),
            phrases: [
                "End my day in \(.applicationName)"
            ],
            shortTitle: "End My Day",
            systemImageName: "moon.stars"
        )
    }
}
