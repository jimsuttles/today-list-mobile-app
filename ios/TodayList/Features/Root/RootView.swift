import SwiftUI

private enum AppTab: Hashable {
    case today, later, history, settings
}

struct RootView: View {
    @Environment(AppEnvironment.self) private var env
    @Environment(\.scenePhase) private var scenePhase
    @State private var tab: AppTab = .today

    var body: some View {
        @Bindable var env = env
        VStack(spacing: 0) {
            Group {
                switch tab {
                case .today:
                    TodayView()
                case .later:
                    LaterView()
                case .history:
                    HistoryView()
                case .settings:
                    SettingsView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if let pending = env.pendingUndo {
                UndoBanner(
                    title: pending.title,
                    onUndo: {
                        Task { @MainActor in
                            await env.undoPending()
                        }
                    },
                    onDismiss: {
                        env.pendingUndo = nil
                    }
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .task(id: pending.completionEventId) {
                    try? await Task.sleep(nanoseconds: 6_000_000_000)
                    if env.pendingUndo?.completionEventId == pending.completionEventId {
                        env.pendingUndo = nil
                    }
                }
            }

            AdBannerSlot(adsRemoved: env.settings.adsRemovedCached)

            Divider()
            HStack {
                tabButton(.today, title: "Today", systemImage: "sun.max")
                tabButton(.later, title: "Later", systemImage: "tray")
                tabButton(.history, title: "History", systemImage: "clock")
                tabButton(.settings, title: "Settings", systemImage: "gearshape")
            }
            .padding(.top, 8)
            .padding(.bottom, 8)
            .background(Color.tlSurface.ignoresSafeArea(edges: .bottom))
        }
        .background(Color.tlBackground.ignoresSafeArea())
        .animation(.easeInOut(duration: 0.2), value: env.pendingUndo?.completionEventId)
        .tint(Color.tlPrimary)
        .sheet(item: rolloverBinding) { review in
            RolloverReviewView(unfinished: review.unfinished, missedDays: review.missedDays)
        }
        .preferredColorScheme(colorScheme)
        .onAppear {
            consumePendingIntentRoute()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                consumePendingIntentRoute()
                Task {
                    let outcome = await env.rolloverManager.evaluate()
                    if case .needsReview = outcome {
                        env.rolloverReview = outcome
                    }
                }
            }
        }
        .onOpenURL { url in
            guard let route = TodayListRoute(url: url) else { return }
            handle(route)
        }
    }

    private func consumePendingIntentRoute() {
        if let route = IntentRouteRequest.consume() {
            handle(route)
        }
    }

    private func handle(_ route: TodayListRoute) {
        switch route {
        case .today:
            tab = .today
        case .later:
            tab = .later
        case .add(let location):
            tab = location == .today ? .today : .later
            env.requestedQuickAddLocation = location
        case .endMyDay:
            tab = .today
            env.endMyDayRequested = true
        case .task(let id):
            env.deepLinkTaskId = id
            tab = .today
        }
    }

    private func tabButton(_ value: AppTab, title: String, systemImage: String) -> some View {
        Button {
            tab = value
        } label: {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                Text(title)
                    .font(.caption2)
            }
            .frame(maxWidth: .infinity)
            .foregroundStyle(tab == value ? Color.tlPrimary : Color.tlOutline)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(tab == value ? .isSelected : [])
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
