import XCTest
@testable import TodayList

final class RecurrenceEngineTests: XCTestCase {
    private let engine = DefaultRecurrenceEngine()
    private let cal = CalendarHelpers.calendar

    func testDailyNextDay() {
        let start = date(2026, 1, 1)
        let rule = RecurrenceRule(type: .daily, startDate: start)
        let next = engine.nextOccurrence(rule: rule, after: start)
        XCTAssertEqual(next.map { CalendarHelpers.startOfDay($0) }, date(2026, 1, 2))
    }

    func testWeekdaysSkipsWeekend() {
        let friday = date(2026, 1, 2) // Friday
        let rule = RecurrenceRule(type: .weekdays, startDate: friday)
        let next = engine.nextOccurrence(rule: rule, after: friday)
        XCTAssertEqual(next.map { CalendarHelpers.startOfDay($0) }, date(2026, 1, 5)) // Monday
    }

    func testWeeklySameWeekday() {
        let start = date(2026, 1, 7) // Wednesday
        let wd = CalendarHelpers.weekday(of: start)
        let rule = RecurrenceRule(type: .weekly, startDate: start, weekdays: [wd])
        let next = engine.nextOccurrence(rule: rule, after: start)
        XCTAssertEqual(next.map { CalendarHelpers.startOfDay($0) }, date(2026, 1, 14))
    }

    func testMonthlyClampsDay() {
        let start = date(2026, 1, 31)
        let rule = RecurrenceRule(type: .monthly, startDate: start, dayOfMonth: 31)
        let next = engine.nextOccurrence(rule: rule, after: start)
        // Feb 2026 has 28 days
        XCTAssertEqual(next.map { CalendarHelpers.startOfDay($0) }, date(2026, 2, 28))
    }

    private func date(_ y: Int, _ m: Int, _ d: Int) -> Date {
        CalendarHelpers.date(year: y, month: m, day: d)!
    }
}
