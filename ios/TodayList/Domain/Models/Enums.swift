import Foundation

enum TaskLocation: String, Codable, CaseIterable, Sendable {
    case today = "TODAY"
    case later = "LATER"
}

enum TaskStatus: String, Codable, CaseIterable, Sendable {
    case today = "TODAY"
    case later = "LATER"
    case deleted = "DELETED"

    var location: TaskLocation? {
        switch self {
        case .today: return .today
        case .later: return .later
        case .deleted: return nil
        }
    }

    static func from(_ location: TaskLocation) -> TaskStatus {
        switch location {
        case .today: return .today
        case .later: return .later
        }
    }
}

enum RecurrenceType: String, Codable, CaseIterable, Sendable {
    case daily = "DAILY"
    case weekdays = "WEEKDAYS"
    case weekly = "WEEKLY"
    case monthly = "MONTHLY"
    case customDays = "CUSTOM_DAYS"
    case customWeeks = "CUSTOM_WEEKS"
    case customMonths = "CUSTOM_MONTHS"
}

enum RolloverMode: String, Codable, CaseIterable, Sendable {
    case ask = "ASK"
    case autoToday = "AUTO_TODAY"
    case autoLater = "AUTO_LATER"

    var title: String {
        switch self {
        case .ask: return "Ask each day"
        case .autoToday: return "Keep on Today"
        case .autoLater: return "Move to Later"
        }
    }
}

enum ThemeMode: String, Codable, CaseIterable, Sendable {
    case system = "SYSTEM"
    case light = "LIGHT"
    case dark = "DARK"

    var title: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }
}

enum WeekStart: String, Codable, CaseIterable, Sendable {
    case sunday = "SUNDAY"
    case monday = "MONDAY"

    var title: String {
        switch self {
        case .sunday: return "Sunday"
        case .monday: return "Monday"
        }
    }
}

enum DeleteScope: String, Sendable {
    case thisTask = "THIS_TASK"
    case entireSeries = "ENTIRE_SERIES"
}

enum RepeatOption: String, CaseIterable, Identifiable, Sendable {
    case none
    case daily
    case weekdays
    case weekly
    case monthly

    var id: String { rawValue }

    var title: String {
        switch self {
        case .none: return "None"
        case .daily: return "Daily"
        case .weekdays: return "Weekdays"
        case .weekly: return "Weekly"
        case .monthly: return "Monthly"
        }
    }
}
