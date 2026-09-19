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
        let defaults = UserDefaults(suiteName: "group.com.fourctech.todaylist")
        let titles = defaults?.stringArray(forKey: "today_titles") ?? []
        return TodayEntry(date: Date(), titles: Array(titles.prefix(5)), count: titles.count)
    }
}

struct TodayListWidgetEntryView: View {
    var entry: TodayEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        Group {
            switch family {
            case .accessoryCircular:
                ZStack {
                    AccessoryWidgetBackground()
                    VStack(spacing: 0) {
                        Text("\(entry.count)")
                            .font(.headline)
                        Text("left")
                            .font(.caption2)
                    }
                }
            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 2) {
                    Label("Today", systemImage: "sun.max")
                        .font(.headline)
                    Text(entry.count == 0 ? "You're clear" : "\(entry.count) remaining")
                        .font(.caption)
                }
            case .systemSmall:
                VStack(alignment: .leading, spacing: 8) {
                    Label("Today", systemImage: "sun.max")
                        .font(.headline)
                    Spacer()
                    Text(entry.count == 0 ? "You're clear" : "\(entry.count)")
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                    Text(entry.count == 1 ? "task remaining" : "tasks remaining")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            default:
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Today")
                            .font(.headline)
                        Spacer()
                        Text(entry.count == 0 ? "You're clear" : "\(entry.count) left")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    if entry.titles.isEmpty {
                        Text("Nothing left for today.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(entry.titles.prefix(5), id: \.self) { title in
                            Text("○  \(title)")
                                .font(.subheadline)
                                .lineLimit(1)
                        }
                    }
                    Spacer(minLength: 0)
                }
            }
        }
        .widgetURL(URL(string: "todaylist://v1/today"))
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
        .description("See what's left on your Today list.")
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .accessoryCircular,
            .accessoryRectangular
        ])
    }
}
