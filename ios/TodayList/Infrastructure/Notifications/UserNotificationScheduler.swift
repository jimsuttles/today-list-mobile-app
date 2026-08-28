import Foundation
import UserNotifications

final class UserNotificationScheduler: NotificationScheduler, @unchecked Sendable {
    private let center = UNUserNotificationCenter.current()

    func scheduleReminder(taskId: String, title: String, at date: Date) async {
        await cancelReminder(taskId: taskId)
        guard date > Date() else { return }
        let content = UNMutableNotificationContent()
        content.title = "Today List"
        content.body = title.isEmpty ? "Task reminder" : title
        content.sound = .default
        content.userInfo = ["taskId": taskId]
        let comps = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute],
            from: date
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        let request = UNNotificationRequest(
            identifier: Self.identifier(for: taskId),
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    func cancelReminder(taskId: String) async {
        center.removePendingNotificationRequests(withIdentifiers: [Self.identifier(for: taskId)])
        center.removeDeliveredNotifications(withIdentifiers: [Self.identifier(for: taskId)])
    }

    func requestPermissionIfNeeded() async -> Bool {
        do {
            return try await center.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
    }

    static func identifier(for taskId: String) -> String {
        "task-reminder-\(taskId)"
    }
}
