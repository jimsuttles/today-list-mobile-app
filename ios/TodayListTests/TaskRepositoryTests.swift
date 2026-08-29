import XCTest
import SwiftData
@testable import TodayList

@MainActor
final class TaskRepositoryTests: XCTestCase {
    private var env: AppEnvironment!

    override func setUp() async throws {
        env = AppEnvironment(inMemory: true)
    }

    func testCreateAndListToday() async throws {
        _ = try await env.taskRepository.createTask(
            title: "Buy milk",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        var listed: [TaskItem] = []
        for await tasks in env.taskRepository.observeTasks(location: .today) {
            listed = tasks
            break
        }
        XCTAssertEqual(listed.count, 1)
        XCTAssertEqual(listed.first?.title, "Buy milk")
    }

    func testMoveTodayToLater() async throws {
        let task = try await env.taskRepository.createTask(
            title: "Park me",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        try await env.taskRepository.moveToLater(taskId: task.id)
        var today: [TaskItem] = []
        var later: [TaskItem] = []
        for await t in env.taskRepository.observeTasks(location: .today) { today = t; break }
        for await t in env.taskRepository.observeTasks(location: .later) { later = t; break }
        XCTAssertTrue(today.isEmpty)
        XCTAssertEqual(later.map(\.title), ["Park me"])
    }

    func testCompleteWritesHistoryAndUndo() async throws {
        let task = try await env.taskRepository.createTask(
            title: "Done soon",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        let eventId = try await env.taskRepository.completeTask(taskId: task.id)
        XCTAssertNotNil(eventId)
        var today: [TaskItem] = []
        for await t in env.taskRepository.observeTasks(location: .today) { today = t; break }
        XCTAssertTrue(today.isEmpty)

        var history: [CompletionRecord] = []
        for await h in env.historyRepository.observeCompletions() { history = h; break }
        XCTAssertEqual(history.first?.titleSnapshot, "Done soon")

        try await env.taskRepository.uncompleteTask(completionEventId: eventId!, restoreTo: .today)
        for await t in env.taskRepository.observeTasks(location: .today) { today = t; break }
        XCTAssertEqual(today.map(\.title), ["Done soon"])
    }

    func testRecurringCompleteSpawnsNext() async throws {
        let start = CalendarHelpers.today()
        let rule = RecurrenceRule(type: .daily, startDate: start)
        let task = try await env.taskRepository.createTask(
            title: "Daily",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: start,
            recurrence: rule
        )
        _ = try await env.taskRepository.completeTask(taskId: task.id)
        var today: [TaskItem] = []
        for await t in env.taskRepository.observeTasks(location: .today) { today = t; break }
        // Next occurrence is tomorrow — not due on Today yet.
        XCTAssertTrue(today.isEmpty)
        let updated = await env.taskRepository.getTask(id: task.id)
        XCTAssertEqual(updated?.location, .today)
        XCTAssertEqual(
            updated?.scheduledDate.map { CalendarHelpers.startOfDay($0) },
            CalendarHelpers.addingDays(1, to: start)
        )
        XCTAssertNil(updated?.reminderAt)
    }

    func testWeekdayCompleteHidesUntilNextWeekday() async throws {
        let start = CalendarHelpers.today()
        let rule = RecurrenceRule(type: .weekdays, startDate: start)
        let task = try await env.taskRepository.createTask(
            title: "Weekday chore",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: start,
            recurrence: rule
        )
        _ = try await env.taskRepository.completeTask(taskId: task.id)
        var today: [TaskItem] = []
        for await t in env.taskRepository.observeTasks(location: .today) { today = t; break }
        XCTAssertTrue(today.isEmpty)
        let updated = await env.taskRepository.getTask(id: task.id)
        let expectedNext = DefaultRecurrenceEngine().nextOccurrence(rule: rule, after: start)
        XCTAssertEqual(
            updated?.scheduledDate.map { CalendarHelpers.startOfDay($0) },
            expectedNext.map { CalendarHelpers.startOfDay($0) }
        )
    }

    func testSettingsRoundTrip() async {
        await env.settingsRepository.updateSettings {
            $0.themeMode = .dark
            $0.rolloverMode = .autoLater
            $0.weekStart = .monday
            $0.hapticsEnabled = false
        }
        let s = env.settingsRepository.currentSettings()
        XCTAssertEqual(s.themeMode, .dark)
        XCTAssertEqual(s.rolloverMode, .autoLater)
        XCTAssertEqual(s.weekStart, .monday)
        XCTAssertFalse(s.hapticsEnabled)
    }

    func testReorder() async throws {
        let a = try await env.taskRepository.createTask(title: "A", notes: nil, location: .today, reminderAt: nil, scheduledDate: nil, recurrence: nil)
        let b = try await env.taskRepository.createTask(title: "B", notes: nil, location: .today, reminderAt: nil, scheduledDate: nil, recurrence: nil)
        try await env.taskRepository.reorderTasks(location: .today, orderedTaskIds: [b.id, a.id])
        var today: [TaskItem] = []
        for await t in env.taskRepository.observeTasks(location: .today) { today = t; break }
        XCTAssertEqual(today.map(\.title), ["B", "A"])
    }
}
