import Foundation
import Testing
@testable import TodayList

struct SuiteHandoffSourceTests {
    @Test
    func handoffIDIsStableForTheSameTask() {
        #expect(
            SuiteHandoffStore.top3HandoffID(sourceTaskID: "task-123")
                == SuiteHandoffStore.top3HandoffID(sourceTaskID: "task-123")
        )
    }

    @Test
    func handoffIDDiffersForDifferentTasks() {
        #expect(
            SuiteHandoffStore.top3HandoffID(sourceTaskID: "task-123")
                != SuiteHandoffStore.top3HandoffID(sourceTaskID: "task-456")
        )
    }

    @Test
    func top3PayloadMapsTheSourceTask() {
        let task = makeTask(id: "task-123", title: "Call dentist", notes: "Ask about Thursday")
        let createdAt = Date(timeIntervalSince1970: 1_700_000_000)

        let payload = SuiteHandoffStore.top3Payload(for: task, createdAt: createdAt)

        #expect(payload.version == 1)
        #expect(payload.sourceApp == "todaylist")
        #expect(payload.destinationApp == "top3")
        #expect(payload.createdAt == createdAt)
        #expect(payload.title == task.title)
        #expect(payload.notes == task.notes)
        #expect(payload.metadata["sourceTaskId"] == task.id)
    }

    @Test
    func pendingPayloadRoundTripsUsingV1ISO8601Encoding() throws {
        let containerURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: containerURL) }
        let payload = SuiteHandoffStore.top3Payload(
            for: makeTask(id: "task-123", title: "Review budget", notes: nil),
            createdAt: Date(timeIntervalSince1970: 1_700_000_000)
        )

        try SuiteHandoffStore.writePending(payload, containerURL: containerURL)
        let decoded = try SuiteHandoffStore.loadPending(id: payload.id, containerURL: containerURL)

        #expect(decoded == payload)
    }

    @Test
    func completedHandoffCannotBeWrittenAgain() throws {
        let containerURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: containerURL) }
        let payload = SuiteHandoffStore.top3Payload(
            for: makeTask(id: "task-123", title: "Review budget", notes: nil)
        )
        let completedDirectory = containerURL
            .appendingPathComponent("Handoffs", isDirectory: true)
            .appendingPathComponent("Completed", isDirectory: true)
        try FileManager.default.createDirectory(at: completedDirectory, withIntermediateDirectories: true)
        try Data().write(to: completedDirectory.appendingPathComponent("\(payload.id.uuidString).json"))

        #expect(throws: SuiteHandoffError.self) {
            try SuiteHandoffStore.writePending(payload, containerURL: containerURL)
        }
    }

    @Test
    func top3DeepLinkContainsOnlyTheOpaqueHandoffID() {
        let task = makeTask(id: "private-task-id", title: "Secret task title", notes: "Secret notes")
        let payload = SuiteHandoffStore.top3Payload(for: task)

        let url = SuiteHandoffStore.top3URL(for: payload.id)

        #expect(url.absoluteString == "top3://v1/handoff/\(payload.id.uuidString)")
        #expect(!url.absoluteString.contains(task.id))
        #expect(!url.absoluteString.contains(task.title))
        #expect(!url.absoluteString.contains("Secret notes"))
    }

    private func makeTask(id: String, title: String, notes: String?) -> TaskItem {
        TaskItem(
            id: id,
            title: title,
            notes: notes,
            location: .today,
            sortOrder: 0,
            createdAt: .now,
            updatedAt: .now,
            scheduledDate: .now,
            reminderAt: nil,
            recurrence: nil
        )
    }
}
