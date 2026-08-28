import Foundation

enum CalendarHelpers {
    static var calendar: Calendar {
        var cal = Calendar.current
        cal.timeZone = .current
        return cal
    }

    static func startOfDay(_ date: Date = Date()) -> Date {
        calendar.startOfDay(for: date)
    }

    static func today() -> Date { startOfDay() }

    static func daysBetween(_ from: Date, _ to: Date) -> Int {
        let a = startOfDay(from)
        let b = startOfDay(to)
        return calendar.dateComponents([.day], from: a, to: b).day ?? 0
    }

    static func addingDays(_ days: Int, to date: Date) -> Date {
        calendar.date(byAdding: .day, value: days, to: startOfDay(date)) ?? date
    }

    static func weekday(of date: Date) -> Int {
        calendar.component(.weekday, from: date)
    }

    /// Monday-based week start date for alignment (matches Android TemporalAdjusters.previousOrSame(MONDAY))
    static func mondayWeekStart(of date: Date) -> Date {
        let day = startOfDay(date)
        let weekday = calendar.component(.weekday, from: day) // 1=Sun ... 7=Sat
        let daysFromMonday: Int
        switch weekday {
        case 1: daysFromMonday = 6 // Sunday
        case 2: daysFromMonday = 0
        default: daysFromMonday = weekday - 2
        }
        return addingDays(-daysFromMonday, to: day)
    }

    static func lengthOfMonth(year: Int, month: Int) -> Int {
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = 1
        guard let date = calendar.date(from: comps),
              let range = calendar.range(of: .day, in: .month, for: date) else {
            return 30
        }
        return range.count
    }

    static func clampedDayOfMonth(year: Int, month: Int, day: Int) -> Int {
        min(max(day, 1), lengthOfMonth(year: year, month: month))
    }

    static func date(year: Int, month: Int, day: Int) -> Date? {
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = day
        return calendar.date(from: comps).map { startOfDay($0) }
    }

    static func yearMonthDay(_ date: Date) -> (Int, Int, Int) {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        return (c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }
}
