import Foundation
import SwiftData

@Model
final class PersistedRecurrence {
    @Attribute(.unique) var id: String
    var typeRaw: String
    var interval: Int
    var startDate: Date
    var endDate: Date?
    var weekdaysCSV: String?
    var dayOfMonth: Int?

    init(
        id: String,
        typeRaw: String,
        interval: Int,
        startDate: Date,
        endDate: Date? = nil,
        weekdaysCSV: String? = nil,
        dayOfMonth: Int? = nil
    ) {
        self.id = id
        self.typeRaw = typeRaw
        self.interval = interval
        self.startDate = startDate
        self.endDate = endDate
        self.weekdaysCSV = weekdaysCSV
        self.dayOfMonth = dayOfMonth
    }

    func toDomain() -> RecurrenceRule {
        let days: Set<Int>
        if let weekdaysCSV, !weekdaysCSV.isEmpty {
            days = Set(weekdaysCSV.split(separator: ",").compactMap { Int($0.trimmingCharacters(in: .whitespaces)) })
        } else {
            days = []
        }
        return RecurrenceRule(
            id: id,
            type: RecurrenceType(rawValue: typeRaw) ?? .daily,
            interval: interval,
            startDate: startDate,
            endDate: endDate,
            weekdays: days,
            dayOfMonth: dayOfMonth
        )
    }

    static func from(_ rule: RecurrenceRule) -> PersistedRecurrence {
        let csv = rule.weekdays.sorted().map(String.init).joined(separator: ",")
        return PersistedRecurrence(
            id: rule.id,
            typeRaw: rule.type.rawValue,
            interval: rule.interval,
            startDate: CalendarHelpers.startOfDay(rule.startDate),
            endDate: rule.endDate.map { CalendarHelpers.startOfDay($0) },
            weekdaysCSV: csv.isEmpty ? nil : csv,
            dayOfMonth: rule.dayOfMonth
        )
    }
}

@Model
final class PersistedTask {
    @Attribute(.unique) var id: String
    var title: String
    var notes: String?
    var statusRaw: String
    var sortOrder: Int
    var createdAt: Date
    var updatedAt: Date
    var scheduledDate: Date?
    var reminderAt: Date?
    var recurrenceId: String?

    init(
        id: String,
        title: String,
        notes: String? = nil,
        statusRaw: String,
        sortOrder: Int,
        createdAt: Date,
        updatedAt: Date,
        scheduledDate: Date? = nil,
        reminderAt: Date? = nil,
        recurrenceId: String? = nil
    ) {
        self.id = id
        self.title = title
        self.notes = notes
        self.statusRaw = statusRaw
        self.sortOrder = sortOrder
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.scheduledDate = scheduledDate
        self.reminderAt = reminderAt
        self.recurrenceId = recurrenceId
    }
}

@Model
final class PersistedOccurrence {
    @Attribute(.unique) var id: String
    var taskId: String
    var occurrenceDate: Date
    var completedAt: Date?
    var movedToLater: Bool
    var createdAt: Date

    init(
        id: String = UUID().uuidString,
        taskId: String,
        occurrenceDate: Date,
        completedAt: Date? = nil,
        movedToLater: Bool = false,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.taskId = taskId
        self.occurrenceDate = occurrenceDate
        self.completedAt = completedAt
        self.movedToLater = movedToLater
        self.createdAt = createdAt
    }
}

@Model
final class PersistedCompletion {
    @Attribute(.unique) var id: String
    var taskId: String?
    var occurrenceId: String?
    var titleSnapshot: String
    var completedAt: Date
    var completionDate: Date

    init(
        id: String = UUID().uuidString,
        taskId: String?,
        occurrenceId: String?,
        titleSnapshot: String,
        completedAt: Date = Date(),
        completionDate: Date = CalendarHelpers.today()
    ) {
        self.id = id
        self.taskId = taskId
        self.occurrenceId = occurrenceId
        self.titleSnapshot = titleSnapshot
        self.completedAt = completedAt
        self.completionDate = completionDate
    }

    func toDomain() -> CompletionRecord {
        CompletionRecord(
            id: id,
            taskId: taskId,
            occurrenceId: occurrenceId,
            titleSnapshot: titleSnapshot,
            completedAt: completedAt,
            completionDate: completionDate
        )
    }
}
