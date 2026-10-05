import Foundation
import ProductivitySuiteCore
import Testing
@testable import TodayList

struct SuiteHandoffSourceTests {
    @Test
    func waitingForPayloadMapsTheSourceTaskUsingV1() {
        let task = makeTask(id: "task-123", title: "Call vendor", notes: "Waiting on estimate")
        let createdAt = Date(timeIntervalSince1970: 1_700_000_000)

        let payload = SuiteHandoffStore.waitingForPayload(for: task, createdAt: createdAt)

        #expect(payload.version == 1)
        #expect(payload.sourceApp == .todayList)
        #expect(payload.destinationApp == .waitingFor)
        #expect(payload.createdAt == createdAt)
        #expect(payload.title == task.title)
        #expect(payload.notes == task.notes)
        #expect(payload.metadata["sourceTaskId"] == task.id)
        #expect(payload.metadata["sourceLocation"] == "today")
        #expect(payload.metadata["requestedDate"] == createdAt.ISO8601Format())
    }

    @Test
    func waitingForHandoffIDUsesTheSuiteDeterministicIdentityRule() {
        let first = SuiteHandoffStore.waitingForHandoffID(sourceTaskID: "task-123")

        #expect(
            first == SuiteHandoffID.generate(
                sourceApp: .todayList,
                sourceEntityID: "task-123",
                destinationApp: .waitingFor
            )
        )
        #expect(first == SuiteHandoffStore.waitingForHandoffID(sourceTaskID: "task-123"))
    }

    @Test
    func waitingForURLContainsOnlyTheOpaqueHandoffID() {
        let task = makeTask(id: "private-task-id", title: "Secret title", notes: "Secret notes")
        let payload = SuiteHandoffStore.waitingForPayload(for: task)

        let url = SuiteHandoffStore.waitingForURL(for: payload.id)

        #expect(url.absoluteString == "waitingfor://v1/handoff/\(payload.id.uuidString)")
        #expect(!url.absoluteString.contains(task.id))
        #expect(!url.absoluteString.contains(task.title))
        #expect(!url.absoluteString.contains("Secret notes"))
    }

    @Test
    func waitingForPendingPayloadIsWrittenUsingCoreTransport() throws {
        let containerURL = temporaryContainerURL()
        defer { try? FileManager.default.removeItem(at: containerURL) }
        let payload = SuiteHandoffStore.waitingForPayload(
            for: makeTask(id: "task-123", title: "Review contract", notes: nil),
            createdAt: Date(timeIntervalSince1970: 1_700_000_000)
        )

        try SuiteHandoffStore.writePending(payload, containerURL: containerURL)

        #expect(try SuiteHandoffStore.loadPending(id: payload.id, containerURL: containerURL) == payload)
    }

    @Test @MainActor
    func failedDestinationOpenLeavesTaskUnchangedAndDoesNotQueueDisposition() async throws {
        let defaults = makeDefaults()
        let store = WaitingForSourceDispositionStore(defaults: defaults)
        let coordinator = WaitingForSourceHandoffCoordinator(dispositionStore: store)
        let containerURL = temporaryContainerURL()
        defer { try? FileManager.default.removeItem(at: containerURL) }
        let environment = AppEnvironment(inMemory: true)
        let task = try await environment.taskRepository.createTask(
            title: "Call supplier",
            notes: "Ask about parts",
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )

        let prepared = try coordinator.prepare(task: task, containerURL: containerURL)
        coordinator.recordDestinationOpenResult(false, handoffID: prepared.payload.id)

        let unchanged = await environment.taskRepository.getTask(id: task.id)
        #expect(unchanged?.title == task.title)
        #expect(unchanged?.notes == task.notes)
        #expect(store.pendingRecords().isEmpty)
        #expect(store.contains(handoffID: prepared.payload.id))
        #expect(try coordinator.prepare(task: task, containerURL: containerURL).payload.id == prepared.payload.id)
    }

    @Test
    func resolvedAttemptDoesNotReplayAndLaterHandoffUsesANewDeterministicID() {
        let store = WaitingForSourceDispositionStore(defaults: makeDefaults())
        let first = store.prepareAttempt(sourceTaskID: "task-123")
        store.markDestinationOpenAccepted(handoffID: first.handoffID)

        store.resolve(handoffID: first.handoffID)
        store.resolve(handoffID: first.handoffID)
        let second = store.prepareAttempt(sourceTaskID: "task-123")

        #expect(store.pendingRecords().isEmpty)
        #expect(second.attempt == 2)
        #expect(second.handoffID != first.handoffID)
        #expect(
            second.handoffID
                == SuiteHandoffStore.waitingForHandoffID(sourceTaskID: "task-123", attempt: 2)
        )
    }

    @Test
    func handoffIDIsExactlyCompatibleWithTheExistingAlgorithm() throws {
        let id = SuiteHandoffStore.top3HandoffID(sourceTaskID: "task-123")
        let expected = try #require(
            UUID(uuidString: "3F6E0ED9-4EDF-5869-A989-254AF5979C30")
        )

        #expect(id == expected)
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

        #expect(payload.version == SuiteHandoffConstants.schemaVersion)
        #expect(payload.sourceApp == .todayList)
        #expect(payload.destinationApp == .top3)
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

    private func makeDefaults() -> UserDefaults {
        UserDefaults(suiteName: "todaylist.waiting-for-tests.\(UUID().uuidString)")!
    }

    private func temporaryContainerURL() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
    }
}

@MainActor
struct WaitingForSourceDispositionTests {
    @Test
    func sourceRemainsUnchangedBeforeDestinationCompletion() async throws {
        let store = WaitingForSourceDispositionStore(defaults: makeDefaults())
        let environment = AppEnvironment(
            inMemory: true,
            waitingForDispositionStore: store,
            waitingForCompletionStatus: { _ in false }
        )
        let task = try await environment.taskRepository.createTask(
            title: "Still mine",
            notes: "Unchanged",
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        _ = acceptedRecord(store: store, taskID: task.id)

        await environment.refreshWaitingForDisposition()

        #expect(environment.waitingForDisposition == nil)
        #expect(await environment.taskRepository.getTask(id: task.id) == task)
    }

    @Test
    func completedHandoffIsDetectedAndRepeatedRefreshDoesNotDuplicatePrompt() async throws {
        let fixture = try await makeFixture(taskTitles: ["First"])
        let record = fixture.records[0]

        await fixture.environment.refreshWaitingForDisposition()
        await fixture.environment.refreshWaitingForDisposition()

        #expect(fixture.environment.waitingForDisposition == record)
        #expect(fixture.store.pendingRecords().count == 1)
    }

    @Test
    func keepLeavesTaskUnchangedAndResolvedStateDoesNotReprompt() async throws {
        let fixture = try await makeFixture(taskTitles: ["Keep me"])
        let task = fixture.tasks[0]
        await fixture.environment.refreshWaitingForDisposition()

        await fixture.environment.resolveWaitingForDisposition(fixture.records[0], action: .keep)
        await fixture.environment.refreshWaitingForDisposition()

        #expect(await fixture.environment.taskRepository.getTask(id: task.id) != nil)
        #expect(fixture.environment.waitingForDisposition == nil)
        #expect(!fixture.store.contains(handoffID: fixture.records[0].handoffID))
    }

    @Test
    func markDoneUsesNormalCompletionBehaviorAndIsIdempotent() async throws {
        let fixture = try await makeFixture(taskTitles: ["Finish me"])
        let task = fixture.tasks[0]
        await fixture.environment.refreshWaitingForDisposition()

        await fixture.environment.resolveWaitingForDisposition(fixture.records[0], action: .markDone)
        await fixture.environment.resolveWaitingForDisposition(fixture.records[0], action: .markDone)

        #expect(await fixture.environment.taskRepository.getTask(id: task.id) == nil)
        var completions: [CompletionRecord] = []
        for await values in fixture.environment.historyRepository.observeCompletions() {
            completions = values
            break
        }
        #expect(completions.filter { $0.taskId == task.id }.count == 1)
        #expect(fixture.environment.pendingUndo?.title == task.title)
    }

    @Test
    func markDonePreservesRecurringCompletionSemantics() async throws {
        let defaults = makeDefaults()
        let store = WaitingForSourceDispositionStore(defaults: defaults)
        var completed = Set<UUID>()
        let environment = AppEnvironment(
            inMemory: true,
            waitingForDispositionStore: store,
            waitingForCompletionStatus: { completed.contains($0) }
        )
        let start = CalendarHelpers.today()
        let task = try await environment.taskRepository.createTask(
            title: "Daily follow-up",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: start,
            recurrence: RecurrenceRule(type: .daily, startDate: start)
        )
        let record = acceptedRecord(store: store, taskID: task.id)
        completed.insert(record.handoffID)
        await environment.refreshWaitingForDisposition()

        await environment.resolveWaitingForDisposition(record, action: .markDone)

        let next = await environment.taskRepository.getTask(id: task.id)
        #expect(next?.scheduledDate.map(CalendarHelpers.startOfDay) == CalendarHelpers.addingDays(1, to: start))
    }

    @Test
    func removeUsesNativeThisTaskDeletionWithoutCreatingCompletion() async throws {
        let fixture = try await makeFixture(taskTitles: ["Remove me"], recurring: true)
        let task = fixture.tasks[0]
        await fixture.environment.refreshWaitingForDisposition()

        await fixture.environment.resolveWaitingForDisposition(fixture.records[0], action: .remove)

        #expect(await fixture.environment.taskRepository.getTask(id: task.id) == nil)
        var completions: [CompletionRecord] = []
        for await values in fixture.environment.historyRepository.observeCompletions() {
            completions = values
            break
        }
        #expect(completions.allSatisfy { $0.taskId != task.id })
    }

    @Test
    func missingSourceTaskResolvesWithoutPrompting() async throws {
        let fixture = try await makeFixture(taskTitles: ["Gone"])
        let task = fixture.tasks[0]
        try await fixture.environment.taskRepository.deleteTask(taskId: task.id, scope: .thisTask)

        await fixture.environment.refreshWaitingForDisposition()

        #expect(fixture.environment.waitingForDisposition == nil)
        #expect(!fixture.store.contains(handoffID: fixture.records[0].handoffID))
    }

    @Test
    func multipleCompletedDispositionsArePresentedInCreationOrder() async throws {
        let fixture = try await makeFixture(taskTitles: ["First", "Second"])

        await fixture.environment.refreshWaitingForDisposition()
        #expect(fixture.environment.waitingForDisposition == fixture.records[0])

        await fixture.environment.resolveWaitingForDisposition(fixture.records[0], action: .keep)
        #expect(fixture.environment.waitingForDisposition == fixture.records[1])

        await fixture.environment.resolveWaitingForDisposition(fixture.records[1], action: .keep)
        #expect(fixture.environment.waitingForDisposition == nil)
    }

    private func makeFixture(
        taskTitles: [String],
        recurring: Bool = false
    ) async throws -> Fixture {
        let store = WaitingForSourceDispositionStore(defaults: makeDefaults())
        var completed = Set<UUID>()
        let environment = AppEnvironment(
            inMemory: true,
            waitingForDispositionStore: store,
            waitingForCompletionStatus: { completed.contains($0) }
        )
        var tasks: [TaskItem] = []
        var records: [WaitingForSourceDisposition] = []
        for (index, title) in taskTitles.enumerated() {
            let task = try await environment.taskRepository.createTask(
                title: title,
                notes: "Notes",
                location: .today,
                reminderAt: nil,
                scheduledDate: CalendarHelpers.today(),
                recurrence: recurring
                    ? RecurrenceRule(type: .daily, startDate: CalendarHelpers.today())
                    : nil
            )
            let record = store.prepareAttempt(
                sourceTaskID: task.id,
                createdAt: Date(timeIntervalSince1970: TimeInterval(index))
            )
            store.markDestinationOpenAccepted(handoffID: record.handoffID)
            completed.insert(record.handoffID)
            tasks.append(task)
            records.append(
                WaitingForSourceDisposition(
                    handoffID: record.handoffID,
                    sourceTaskID: record.sourceTaskID,
                    attempt: record.attempt,
                    createdAt: record.createdAt,
                    destinationOpenAccepted: true
                )
            )
        }
        return Fixture(environment: environment, store: store, tasks: tasks, records: records)
    }

    private func acceptedRecord(
        store: WaitingForSourceDispositionStore,
        taskID: String
    ) -> WaitingForSourceDisposition {
        let record = store.prepareAttempt(sourceTaskID: taskID)
        store.markDestinationOpenAccepted(handoffID: record.handoffID)
        return WaitingForSourceDisposition(
            handoffID: record.handoffID,
            sourceTaskID: record.sourceTaskID,
            attempt: record.attempt,
            createdAt: record.createdAt,
            destinationOpenAccepted: true
        )
    }

    private func makeDefaults() -> UserDefaults {
        UserDefaults(suiteName: "todaylist.waiting-for-disposition-tests.\(UUID().uuidString)")!
    }

    private struct Fixture {
        let environment: AppEnvironment
        let store: WaitingForSourceDispositionStore
        let tasks: [TaskItem]
        let records: [WaitingForSourceDisposition]
    }
}
