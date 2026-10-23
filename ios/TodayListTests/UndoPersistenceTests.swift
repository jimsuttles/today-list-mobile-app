import XCTest
import SwiftData
@testable import TodayList

/// Issue #14: Undo durability against a real on-disk SwiftData store.
///
/// Each test mirrors `AppEnvironment` (a `ModelContext(container)` over the app's
/// schema), performs the action, then drops the context and container without any
/// further save, as if the app were terminated. A fresh container on the same file
/// then reads what was actually persisted. An in-memory store with a single context
/// cannot detect a pending, unsaved delete, because fetches in that context already
/// exclude it.
@MainActor
final class UndoPersistenceTests: XCTestCase {
    private var storeURL: URL!

    override func setUp() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("UndoPersistenceTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        storeURL = directory.appendingPathComponent("TodayList.store")
    }

    override func tearDown() async throws {
        try? FileManager.default.removeItem(at: storeURL.deletingLastPathComponent())
    }

    // MARK: - Non-repeating task

    func testUndoOfNonRepeatingTaskIsDurableAcrossStoreReload() async throws {
        var taskId = ""
        try await withSession { repository, _ in
            let task = try await repository.createTask(
                title: "One-off", notes: "keep", location: .today,
                reminderAt: nil, scheduledDate: nil, recurrence: nil
            )
            taskId = task.id
            let completion = try await repository.completeTask(taskId: task.id)
            let eventId = try XCTUnwrap(completion)
            try await repository.uncompleteTask(completionEventId: eventId, restoreTo: .today)
        }

        try reload { context in
            let task = try XCTUnwrap(try self.task(id: taskId, in: context))
            XCTAssertEqual(task.statusRaw, TaskStatus.today.rawValue, "Undo must restore the task durably")
            XCTAssertEqual(
                try self.completions(taskId: taskId, in: context).count, 0,
                "Undo must durably remove the completion, or History shows it after relaunch"
            )
        }
    }

    func testRepeatedCompleteUndoCyclesLeaveNoStaleCompletions() async throws {
        var taskId = ""
        try await withSession { repository, _ in
            let task = try await repository.createTask(
                title: "Cycle", notes: nil, location: .today,
                reminderAt: nil, scheduledDate: nil, recurrence: nil
            )
            taskId = task.id
            for _ in 0..<3 {
                let completion = try await repository.completeTask(taskId: task.id)
                let eventId = try XCTUnwrap(completion)
                try await repository.uncompleteTask(completionEventId: eventId, restoreTo: .today)
            }
        }

        try reload { context in
            XCTAssertEqual(try self.task(id: taskId, in: context)?.statusRaw, TaskStatus.today.rawValue)
            XCTAssertEqual(try self.completions(taskId: taskId, in: context).count, 0)
        }
    }

    func testUndoRemovesOnlyItsOwnCompletion() async throws {
        var keptTaskId = ""
        var undoneTaskId = ""
        var keptEventId = ""
        try await withSession { repository, _ in
            let kept = try await repository.createTask(
                title: "Stays done", notes: nil, location: .today,
                reminderAt: nil, scheduledDate: nil, recurrence: nil
            )
            let undone = try await repository.createTask(
                title: "Undone", notes: nil, location: .today,
                reminderAt: nil, scheduledDate: nil, recurrence: nil
            )
            keptTaskId = kept.id
            undoneTaskId = undone.id
            let keptCompletion = try await repository.completeTask(taskId: kept.id)
            keptEventId = try XCTUnwrap(keptCompletion)
            let undoneCompletion = try await repository.completeTask(taskId: undone.id)
            let undoneEventId = try XCTUnwrap(undoneCompletion)
            try await repository.uncompleteTask(completionEventId: undoneEventId, restoreTo: .today)
        }

        try reload { context in
            XCTAssertEqual(try self.completions(taskId: keptTaskId, in: context).map(\.id), [keptEventId])
            XCTAssertEqual(try self.task(id: keptTaskId, in: context)?.statusRaw, TaskStatus.deleted.rawValue)
            XCTAssertEqual(try self.completions(taskId: undoneTaskId, in: context).count, 0)
            XCTAssertEqual(try self.task(id: undoneTaskId, in: context)?.statusRaw, TaskStatus.today.rawValue)
        }
    }

    // MARK: - Repeating task (control for the invalid earlier report)

    func testCompletingRepeatingTaskLegitimatelyKeepsItActiveWithAHistoryRecord() async throws {
        var taskId = ""
        try await withSession { repository, _ in
            let task = try await repository.createTask(
                title: "Weekday chore", notes: nil, location: .today,
                reminderAt: nil, scheduledDate: CalendarHelpers.today(),
                recurrence: RecurrenceRule.from(option: .weekdays)
            )
            taskId = task.id
            let completion = try await repository.completeTask(taskId: task.id)
            _ = try XCTUnwrap(completion)
        }

        try reload { context in
            XCTAssertEqual(try self.task(id: taskId, in: context)?.statusRaw, TaskStatus.today.rawValue)
            XCTAssertEqual(try self.completions(taskId: taskId, in: context).count, 1)
        }
    }

    func testUndoOfRepeatingTaskIsDurableAcrossStoreReload() async throws {
        var taskId = ""
        try await withSession { repository, _ in
            let task = try await repository.createTask(
                title: "Weekday chore", notes: nil, location: .today,
                reminderAt: nil, scheduledDate: CalendarHelpers.today(),
                recurrence: RecurrenceRule.from(option: .weekdays)
            )
            taskId = task.id
            let completion = try await repository.completeTask(taskId: task.id)
            let eventId = try XCTUnwrap(completion)
            try await repository.uncompleteTask(completionEventId: eventId, restoreTo: .today)
        }

        try reload { context in
            XCTAssertEqual(try self.task(id: taskId, in: context)?.statusRaw, TaskStatus.today.rawValue)
            XCTAssertEqual(try self.completions(taskId: taskId, in: context).count, 0)
        }
    }

    // MARK: - Helpers

    private var schema: Schema {
        Schema([
            PersistedTask.self,
            PersistedRecurrence.self,
            PersistedOccurrence.self,
            PersistedCompletion.self,
        ])
    }

    /// One "app session": built like AppEnvironment, then torn down with no further save.
    private func withSession(
        _ body: (SwiftDataTaskRepository, ModelContext) async throws -> Void
    ) async throws {
        let container = try ModelContainer(for: schema, configurations: ModelConfiguration(url: storeURL))
        let context = ModelContext(container)
        try await body(SwiftDataTaskRepository(modelContext: context), context)
    }

    /// A fresh container and context on the same store file, like a relaunch.
    private func reload(_ body: (ModelContext) throws -> Void) throws {
        let container = try ModelContainer(for: schema, configurations: ModelConfiguration(url: storeURL))
        try body(ModelContext(container))
    }

    private func task(id: String, in context: ModelContext) throws -> PersistedTask? {
        try context.fetch(FetchDescriptor<PersistedTask>(predicate: #Predicate { $0.id == id })).first
    }

    private func completions(taskId: String, in context: ModelContext) throws -> [PersistedCompletion] {
        try context.fetch(FetchDescriptor<PersistedCompletion>(predicate: #Predicate { $0.taskId == taskId }))
    }
}
