import SwiftUI

struct RootView: View {
    @Environment(\.appEnvironment) private var env
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView {
            TodayView()
                .tabItem { Label("Today", systemImage: "sun.max") }
            LaterView()
                .tabItem { Label("Later", systemImage: "tray") }
            HistoryView()
                .tabItem { Label("History", systemImage: "clock") }
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
        .tint(Color.tlPrimary)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 0) {
                if let pending = env.pendingUndo {
                    UndoBanner(
                        title: pending.title,
                        onUndo: { Task { await env.undoPending() } },
                        onDismiss: { env.pendingUndo = nil }
                    )
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .task(id: pending.completionEventId) {
                        try? await Task.sleep(nanoseconds: 5_000_000_000)
                        if env.pendingUndo?.completionEventId == pending.completionEventId {
                            env.pendingUndo = nil
                        }
                    }
                }
                AdBannerSlot(adsRemoved: env.settings.adsRemovedCached)
            }
        }
        .sheet(item: rolloverBinding) { review in
            RolloverReviewView(unfinished: review.unfinished, missedDays: review.missedDays)
        }
        .preferredColorScheme(colorScheme)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                Task {
                    let outcome = await env.rolloverManager.evaluate()
                    if case .needsReview = outcome {
                        env.rolloverReview = outcome
                    }
                }
            }
        }
        .onOpenURL { url in
            // todaylist://task/{id}
            guard url.scheme == "todaylist", url.host == "task" else { return }
            let id = url.pathComponents.filter { $0 != "/" }.first ?? url.lastPathComponent
            if !id.isEmpty { env.deepLinkTaskId = id }
        }
    }

    private var colorScheme: ColorScheme? {
        switch env.settings.themeMode {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    private var rolloverBinding: Binding<RolloverReviewItem?> {
        Binding(
            get: {
                if case .needsReview(let unfinished, let missed, _) = env.rolloverReview {
                    return RolloverReviewItem(unfinished: unfinished, missedDays: missed)
                }
                return nil
            },
            set: { newValue in
                if newValue == nil { env.rolloverReview = nil }
            }
        )
    }
}

struct RolloverReviewItem: Identifiable {
    let id = UUID()
    let unfinished: [TaskItem]
    let missedDays: Int
}
