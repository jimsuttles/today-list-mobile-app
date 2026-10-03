import Foundation
import XCTest
@testable import TodayList

@MainActor
final class UserDefaultsSettingsRepositoryTests: XCTestCase {
    func testRolloverDateRoundTripsAsSameLocalCalendarDay() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/New_York")!

        let original = calendar.date(
            from: DateComponents(
                timeZone: calendar.timeZone,
                year: 2026,
                month: 10,
                day: 3
            )
        )!

        let stored = UserDefaultsSettingsRepository.encodeStoredDay(
            original,
            calendar: calendar
        )
        let restored = UserDefaultsSettingsRepository.decodeStoredDay(
            stored,
            calendar: calendar
        )

        XCTAssertEqual(stored, "2026-10-03")
        XCTAssertNotNil(restored)
        XCTAssertTrue(
            calendar.isDate(restored!, inSameDayAs: original),
            "Rollover persistence must preserve the user's local calendar day."
        )
    }

    func testExistingDateOnlyValueIsDecodedAsLocalDayInsteadOfUTCInstant() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/New_York")!

        let restored = UserDefaultsSettingsRepository.decodeStoredDay(
            "2026-10-03",
            calendar: calendar
        )

        let components = calendar.dateComponents([.year, .month, .day], from: restored!)
        XCTAssertEqual(components.year, 2026)
        XCTAssertEqual(components.month, 10)
        XCTAssertEqual(components.day, 3)
    }
}
