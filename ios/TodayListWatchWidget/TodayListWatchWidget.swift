import WidgetKit
import SwiftUI

struct TodayWatchEntry: TimelineEntry {
    let date: Date
    let remaining: Int
}

struct TodayWatchProvider: TimelineProvider {
    func placeholder(in context: Context) -> TodayWatchEntry {
        TodayWatchEntry(date: Date(), remaining: 3)
    }

    func getSnapshot(in context: Context, completion: @escaping (TodayWatchEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TodayWatchEntry>) -> Void) {
        completion(Timeline(entries: [loadEntry()], policy: .after(Date().addingTimeInterval(900))))
    }

    private func loadEntry() -> TodayWatchEntry {
        let defaults = UserDefaults(suiteName: "group.com.fourctech.todaylist")
        return TodayWatchEntry(
            date: Date(),
            remaining: defaults?.integer(forKey: "watch_remaining_count") ?? 0
        )
    }
}

struct TodayWatchWidgetView: View {
    let entry: TodayWatchEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                VStack(spacing: 0) {
                    Text("\(entry.remaining)")
                        .font(.headline)
                    Text("left")
                        .font(.caption2)
                }
            }
        default:
            VStack(alignment: .leading, spacing: 2) {
                Label("Today", systemImage: "sun.max")
                Text(entry.remaining == 0 ? "Clear" : "\(entry.remaining) left")
                    .font(.caption)
            }
        }
    }
}

@main
struct TodayListWatchWidget: Widget {
    let kind = "TodayListWatchWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: TodayWatchProvider()) { entry in
            TodayWatchWidgetView(entry: entry)
        }
        .configurationDisplayName("Today")
        .description("See how many Today tasks remain.")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular])
    }
}
