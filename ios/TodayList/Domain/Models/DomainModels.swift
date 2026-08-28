import Foundation

struct RecurrenceRule: Equatable, Sendable, Identifiable {
    var id: String
    var type: RecurrenceType
    var interval: Int
    var startDate: Date // start of day
    var endDate: Date?
    var weekdays: Set<Int> // 1=Sun ... 7=Sat (Calendar weekday)
    var dayOfMonth: Int?

    init(
        id: String = UUID().uuidString,
        type: RecurrenceType,
        interval: Int = 1,
        startDate: Date,
        endDate: Date? = nil,
        weekdays: Set<Int> = [],
        dayOfMonth: Int? = nil
    ) {
        self.id = id
        self.type = type
        self.interval = interval
        self.startDate = startDate
        self.endDate = endDate
        self.weekdays = weekdays
        self.dayOfMonth = dayOfMonth
    }
}

struct TaskItem: Equatable, Sendable, Identifiable {
    var id: String
    var title: String
    var notes: String?
    var location: TaskLocation
    var sortOrder: Int
    var createdAt: Date
    var updatedAt: Date
    var scheduledDate: Date?
    var reminderAt: Date?
    var recurrence: RecurrenceRule?
}

struct CompletionRecord: Equatable, Sendable, Identifiable {
    var id: String
    var taskId: String?
    var occurrenceId: String?
    var titleSnapshot: String
    var completedAt: Date
    var completionDate: Date
}

struct AppSettings: Equatable, Sendable {
    var themeMode: ThemeMode = .system
    var rolloverMode: RolloverMode = .ask
    var weekStart: WeekStart = .sunday
    var hapticsEnabled: Bool = true
    var adsRemovedCached: Bool = false
    var lastRolloverDate: Date? = nil
    var notificationPermissionPrompted: Bool = false
}
