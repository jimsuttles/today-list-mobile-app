import Foundation

@MainActor
final class UserDefaultsSettingsRepository: SettingsRepository {
    private let defaults: UserDefaults
    private var continuations: [UUID: AsyncStream<AppSettings>.Continuation] = [:]
    private var cached: AppSettings

    private enum Keys {
        static let themeMode = "theme_mode"
        static let rolloverMode = "rollover_mode"
        static let weekStart = "week_start"
        static let hapticsEnabled = "haptics_enabled"
        static let lastRolloverDate = "last_rollover_date"
        static let notificationPermissionPrompted = "notification_permission_prompted"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.cached = Self.load(from: defaults)
    }

    func observeSettings() -> AsyncStream<AppSettings> {
        AsyncStream { [weak self] continuation in
            guard let self else {
                continuation.finish()
                return
            }
            let id = UUID()
            self.continuations[id] = continuation
            continuation.yield(self.cached)
            continuation.onTermination = { _ in
                Task { @MainActor in
                    self.continuations[id] = nil
                }
            }
        }
    }

    func currentSettings() -> AppSettings {
        cached
    }

    func updateSettings(_ transform: (inout AppSettings) -> Void) async {
        transform(&cached)
        Self.save(cached, to: defaults)
        for c in continuations.values { c.yield(cached) }
    }

    func resetPreferences() async {
        cached = AppSettings()
        Self.save(cached, to: defaults)
        for c in continuations.values { c.yield(cached) }
    }

    private static func load(from defaults: UserDefaults) -> AppSettings {
        var s = AppSettings()
        if let v = defaults.string(forKey: Keys.themeMode), let m = ThemeMode(rawValue: v) {
            s.themeMode = m
        }
        if let v = defaults.string(forKey: Keys.rolloverMode), let m = RolloverMode(rawValue: v) {
            s.rolloverMode = m
        }
        if let v = defaults.string(forKey: Keys.weekStart), let m = WeekStart(rawValue: v) {
            s.weekStart = m
        }
        if defaults.object(forKey: Keys.hapticsEnabled) != nil {
            s.hapticsEnabled = defaults.bool(forKey: Keys.hapticsEnabled)
        }
        if let storedDay = defaults.string(forKey: Keys.lastRolloverDate) {
            s.lastRolloverDate = decodeStoredDay(storedDay)
        }
        s.notificationPermissionPrompted = defaults.bool(forKey: Keys.notificationPermissionPrompted)
        return s
    }

    nonisolated static func encodeStoredDay(_ date: Date, calendar: Calendar = .current) -> String {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        guard let year = components.year,
              let month = components.month,
              let day = components.day else {
            return ""
        }
        return String(format: "%04d-%02d-%02d", year, month, day)
    }

    nonisolated static func decodeStoredDay(_ value: String, calendar: Calendar = .current) -> Date? {
        let parts = value.split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2]) else {
            return nil
        }

        var components = DateComponents()
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        components.year = year
        components.month = month
        components.day = day

        guard let date = calendar.date(from: components) else {
            return nil
        }
        return calendar.startOfDay(for: date)
    }

    private static func save(_ settings: AppSettings, to defaults: UserDefaults) {
        defaults.set(settings.themeMode.rawValue, forKey: Keys.themeMode)
        defaults.set(settings.rolloverMode.rawValue, forKey: Keys.rolloverMode)
        defaults.set(settings.weekStart.rawValue, forKey: Keys.weekStart)
        defaults.set(settings.hapticsEnabled, forKey: Keys.hapticsEnabled)
        if let d = settings.lastRolloverDate {
            defaults.set(encodeStoredDay(d), forKey: Keys.lastRolloverDate)
        } else {
            defaults.removeObject(forKey: Keys.lastRolloverDate)
        }
        defaults.set(settings.notificationPermissionPrompted, forKey: Keys.notificationPermissionPrompted)
    }
}

extension ISO8601DateFormatter {
    static let dateOnly: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withFullDate]
        f.timeZone = TimeZone(secondsFromGMT: 0)
        return f
    }()
}
