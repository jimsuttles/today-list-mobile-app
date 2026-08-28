import XCTest
@testable import TodayList

@MainActor
final class RolloverManagerTests: XCTestCase {
    func testAskNeedsReviewWhenUnfinished() async throws {
        let env = AppEnvironment(inMemory: true)
        await env.settingsRepository.updateSettings {
            $0.rolloverMode = .ask
            $0.lastRolloverDate = CalendarHelpers.addingDays(-1, to: CalendarHelpers.today())
        }
        _ = try await env.taskRepository.createTask(
            title: "Leftover",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        let outcome = await env.rolloverManager.evaluate()
        if case .needsReview(let unfinished, _, _) = outcome {
            XCTAssertEqual(unfinished.map(\.title), ["Leftover"])
        } else {
            XCTFail("Expected needsReview, got \(outcome)")
        }
    }

    func testAutoLaterMovesTasks() async throws {
        let env = AppEnvironment(inMemory: true)
        await env.settingsRepository.updateSettings {
            $0.rolloverMode = .autoLater
            $0.lastRolloverDate = CalendarHelpers.addingDays(-1, to: CalendarHelpers.today())
        }
        _ = try await env.taskRepository.createTask(
            title: "Move me",
            notes: nil,
            location: .today,
            reminderAt: nil,
            scheduledDate: nil,
            recurrence: nil
        )
        let outcome = await env.rolloverManager.evaluate()
        XCTAssertEqual(outcome, .appliedSilently(keptOnToday: 0, movedToLater: 1))
        var later: [TaskItem] = []
        for await t in env.taskRepository.observeTasks(location: .later) { later = t; break }
        XCTAssertEqual(later.map(\.title), ["Move me"])
    }
}
