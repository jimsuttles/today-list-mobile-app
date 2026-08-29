import SwiftUI
import SwiftData

@main
struct TodayListApp: App {
    @State private var env = AppEnvironment()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(env)
                .task { await env.bootstrap() }
        }
        .modelContainer(env.modelContainer)
    }
}
