import Foundation
import WidgetKit

enum WidgetSnapshot {
    static let suiteName = "group.com.fourctech.todaylist"

    static func publish(todayTitles: [String]) {
        let defaults = UserDefaults(suiteName: suiteName)
        defaults?.set(todayTitles, forKey: "today_titles")
        WidgetCenter.shared.reloadAllTimelines()
    }
}
