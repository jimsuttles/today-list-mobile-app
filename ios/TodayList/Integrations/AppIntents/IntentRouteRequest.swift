import Foundation

enum IntentRouteRequest {
    private static let suiteName = "group.com.fourctech.todaylist"
    private static let key = "pending_intent_route"

    static func store(_ route: String) {
        UserDefaults(suiteName: suiteName)?.set(route, forKey: key)
    }

    static func consume() -> TodayListRoute? {
        let defaults = UserDefaults(suiteName: suiteName)
        guard let value = defaults?.string(forKey: key) else { return nil }
        defaults?.removeObject(forKey: key)
        guard let url = URL(string: value) else { return nil }
        return TodayListRoute(url: url)
    }
}
