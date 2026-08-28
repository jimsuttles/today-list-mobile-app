import WidgetKit
import SwiftUI

struct TodayEntry: TimelineEntry {
    let date: Date
    let titles: [String]
    let count: Int
}

struct TodayProvider: TimelineProvider {
    func placeholder(in context: Context) -> TodayEntry {
        TodayEntry(date: Date(), titles: ["Sample task"], count: 1)
    }

    func getSnapshot(in context: Context, completion: @escaping (TodayEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TodayEntry>) -> Void) {
        let entry = loadEntry()
        let next = Calendar.current.date(byAdding: .minute, value: 15, to: Date()) ?? Date()
        completion(Timeline(entries: [entry], policy: .after(next)))
    }

    private func loadEntry() -> TodayEntry {
        // Widget reads App Group UserDefaults snapshot written by the app.
        let defaults = UserDefaults(suiteName: "group.com.fourctech.todaylist")
        let titles = defaults?.stringArray(forKey: "today_titles") ?? []
        return TodayEntry(date: Date(), titles: Array(titles.prefix(5)), count: titles.count)
    }
}

struct TodayListWidgetEntryView: View {
    var entry: TodayEntry
    @Environment(\.widgetFamily) var family

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Today")
                    .font(.headline)
                    .foregroundStyle(Color(red: 0x1B/255, green: 0x4F/255, blue: 0x72/255))
                Spacer()
                Text(entry.count == 0 ? "You're clear" : "\(entry.count) left")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if family != .systemSmall {
                ForEach(entry.titles, id: \.self) { title in
                    Text("○  \(title)")
                        .font(.subheadline)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding()
        .containerBackground(for: .widget) {
            Color(red: 0xEE/255, green: 0xF4/255, blue: 0xF8/255)
        }
    }
}

@main
struct TodayListWidget: Widget {
    let kind = "TodayListWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: TodayProvider()) { entry in
            TodayListWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Today")
        .description("See what's on your Today list.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
