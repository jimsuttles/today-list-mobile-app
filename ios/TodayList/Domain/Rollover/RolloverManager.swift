import Foundation

enum RolloverOutcome: Equatable, Sendable {
    case upToDate
    case appliedSilently(keptOnToday: Int = 0, movedToLater: Int = 0)
    case needsReview(unfinished: [TaskItem], missedDays: Int, lastRollover: Date?)
}

@MainActor
final class RolloverManager {
    private let taskRepository: TaskRepository
    private let settingsRepository: SettingsRepository

    init(taskRepository: TaskRepository, settingsRepository: SettingsRepository) {
        self.taskRepository = taskRepository
        self.settingsRepository = settingsRepository
    }

    func evaluate(today: Date = CalendarHelpers.today()) async -> RolloverOutcome {
        let settings = settingsRepository.currentSettings()
        if let last = settings.lastRolloverDate,
           CalendarHelpers.startOfDay(last) >= CalendarHelpers.startOfDay(today) {
            return .upToDate
        }
        let missed: Int
        if let last = settings.lastRolloverDate {
            missed = max(1, CalendarHelpers.daysBetween(last, today))
        } else {
            missed = 1
        }

        // Snapshot today tasks via one-shot from stream
        let unfinished = await snapshotTasks(location: .today)

        switch settings.rolloverMode {
        case .ask:
            if unfinished.isEmpty {
                await markComplete(today: today)
                return .appliedSilently()
            }
            return .needsReview(
                unfinished: unfinished,
                missedDays: missed,
                lastRollover: settings.lastRolloverDate
            )
        case .autoToday:
            try? await taskRepository.keepOnTodayForNewDay(
                taskIds: unfinished.map(\.id),
                today: today
            )
            await markComplete(today: today)
            return .appliedSilently(keptOnToday: unfinished.count)
        case .autoLater:
            for task in unfinished {
                try? await taskRepository.moveToLater(taskId: task.id)
            }
            await markComplete(today: today)
            return .appliedSilently(movedToLater: unfinished.count)
        }
    }

    func applyDecisions(_ decisions: [String: TaskLocation], today: Date = CalendarHelpers.today()) async {
        for (id, location) in decisions {
            switch location {
            case .today:
                try? await taskRepository.keepOnTodayForNewDay(taskIds: [id], today: today)
            case .later:
                try? await taskRepository.moveToLater(taskId: id)
            }
        }
        await markComplete(today: today)
    }

    private func markComplete(today: Date) async {
        await settingsRepository.updateSettings { $0.lastRolloverDate = CalendarHelpers.startOfDay(today) }
    }

    private func snapshotTasks(location: TaskLocation) async -> [TaskItem] {
        for await tasks in taskRepository.observeTasks(location: location) {
            return tasks
        }
        return []
    }
}
