import SwiftUI

@main
struct TodayListWatchApp: App {
    @StateObject private var store = WatchTodayStore.shared

    var body: some Scene {
        WindowGroup {
            WatchTodayView()
                .environmentObject(store)
        }
    }
}
