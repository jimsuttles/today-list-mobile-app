import Foundation

protocol RecurrenceEngine: Sendable {
    func nextOccurrence(rule: RecurrenceRule, after: Date) -> Date?
}

struct DefaultRecurrenceEngine: RecurrenceEngine {
    func nextOccurrence(rule: RecurrenceRule, after: Date) -> Date? {
        let interval = max(1, rule.interval)
        let start = CalendarHelpers.startOfDay(rule.startDate)
        let end = rule.endDate.map { CalendarHelpers.startOfDay($0) }

        var candidate = max(CalendarHelpers.addingDays(1, to: after), start)
        let limit = CalendarHelpers.addingDays(365 * 5, to: candidate)

        while candidate <= limit {
            if let end, candidate > end { return nil }
            if matches(rule: rule, date: candidate, interval: interval, start: start) {
                return candidate
            }
            candidate = CalendarHelpers.addingDays(1, to: candidate)
        }
        return nil
    }

    private func matches(rule: RecurrenceRule, date: Date, interval: Int, start: Date) -> Bool {
        if date < start { return false }
        switch rule.type {
        case .daily, .customDays:
            let days = CalendarHelpers.daysBetween(start, date)
            return days >= 0 && days % interval == 0
        case .weekdays:
            let wd = CalendarHelpers.weekday(of: date)
            let isWeekday = (2...6).contains(wd)
            return isWeekday && weeksAligned(start: start, date: date, interval: 1)
        case .weekly, .customWeeks:
            let days = rule.weekdays.isEmpty
                ? Set([CalendarHelpers.weekday(of: start)])
                : rule.weekdays
            return days.contains(CalendarHelpers.weekday(of: date))
                && weeksAligned(start: start, date: date, interval: interval)
        case .monthly, .customMonths:
            let (y, m, d) = CalendarHelpers.yearMonthDay(date)
            let targetDay = rule.dayOfMonth ?? CalendarHelpers.yearMonthDay(start).2
            let clamped = CalendarHelpers.clampedDayOfMonth(year: y, month: m, day: targetDay)
            return d == clamped && monthsAligned(start: start, date: date, interval: interval, targetDay: targetDay)
        }
    }

    private func weeksAligned(start: Date, date: Date, interval: Int) -> Bool {
        let startWeek = CalendarHelpers.mondayWeekStart(of: start)
        let dateWeek = CalendarHelpers.mondayWeekStart(of: date)
        let weeks = CalendarHelpers.daysBetween(startWeek, dateWeek) / 7
        return weeks >= 0 && weeks % interval == 0
    }

    private func monthsAligned(start: Date, date: Date, interval: Int, targetDay: Int) -> Bool {
        let (sy, sm, _) = CalendarHelpers.yearMonthDay(start)
        let (dy, dm, _) = CalendarHelpers.yearMonthDay(date)
        let startIndex = sy * 12 + sm
        let dateIndex = dy * 12 + dm
        let months = dateIndex - startIndex
        return months >= 0 && months % interval == 0
    }
}

extension RecurrenceRule {
    static func from(option: RepeatOption, startDate: Date = CalendarHelpers.today()) -> RecurrenceRule? {
        switch option {
        case .none: return nil
        case .daily:
            return RecurrenceRule(type: .daily, startDate: startDate)
        case .weekdays:
            return RecurrenceRule(type: .weekdays, startDate: startDate)
        case .weekly:
            return RecurrenceRule(
                type: .weekly,
                startDate: startDate,
                weekdays: [CalendarHelpers.weekday(of: startDate)]
            )
        case .monthly:
            return RecurrenceRule(
                type: .monthly,
                startDate: startDate,
                dayOfMonth: CalendarHelpers.yearMonthDay(startDate).2
            )
        }
    }

    var asRepeatOption: RepeatOption {
        switch type {
        case .daily, .customDays: return .daily
        case .weekdays: return .weekdays
        case .weekly, .customWeeks: return .weekly
        case .monthly, .customMonths: return .monthly
        }
    }
}
