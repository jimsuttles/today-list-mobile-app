import XCTest
@testable import TodayList

final class TodayListRouteTests: XCTestCase {
    func testParsesSuiteHandoffRoute() {
        let id = UUID()
        let url = URL(string: "todaylist://v1/handoff/\(id.uuidString)")!

        XCTAssertEqual(TodayListRoute(url: url), .handoff(id))
    }

    func testRejectsMalformedSuiteHandoffRoute() {
        let url = URL(string: "todaylist://v1/handoff/not-a-uuid")!

        XCTAssertNil(TodayListRoute(url: url))
    }

    func testPreservesAddRouteDestination() {
        let url = URL(string: "todaylist://v1/add?destination=later")!

        XCTAssertEqual(TodayListRoute(url: url), .add(.later))
    }
}
