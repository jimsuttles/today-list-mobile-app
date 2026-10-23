import SwiftUI

private enum AppTab: Hashable {
    case today, later, history, settings
}

struct RootView: View {
    @Environment(AppEnvironment.self) private var env
    @Environment(\.scenePhase) private var scenePhase
    @State private var tab: AppTab = .today
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        @Bindable var env = env
        VStack(spacing: 0) {
            TabView(selection: $tab) {
                tabContent({ TodayView() })
                    .tabItem { Label("Today", systemImage: "sun.max") }
                    .tag(AppTab.today)
                tabContent({ LaterView() })
                    .tabItem { Label("Later", systemImage: "tray") }
                    .tag(AppTab.later)
                tabContent({ HistoryView() })
                    .tabItem { Label("History", systemImage: "clock") }
                    .tag(AppTab.history)
                tabContent({ SettingsView() })
                    .tabItem { Label("Settings", systemImage: "gearshape") }
                    .tag(AppTab.settings)
            }
            PendingUndoBanner()
            if usesCustomBar { customTabBar }
        }
        .background(Color.tlBackground.ignoresSafeArea())
        .animation(.easeInOut(duration: 0.2), value: env.pendingUndo?.completionEventId)
        .tint(Color.tlPrimary)
        .sheet(item: rolloverBinding) { review in
            RolloverReviewView(unfinished: review.unfinished, missedDays: review.missedDays)
        }
        .confirmationDialog(
            "Follow-up created. What should happen to this Today List task?",
            isPresented: Binding(
                get: { env.waitingForDisposition != nil },
                set: { if !$0 { env.waitingForDisposition = nil } }
            ),
            titleVisibility: .visible
        ) {
            if let disposition = env.waitingForDisposition {
                Button("Keep") {
                    Task { await env.resolveWaitingForDisposition(disposition, action: .keep) }
                }
                Button("Mark Done") {
                    Task { await env.resolveWaitingForDisposition(disposition, action: .markDone) }
                }
                Button("Remove", role: .destructive) {
                    Task { await env.resolveWaitingForDisposition(disposition, action: .remove) }
                }
            }
        }
        .alert(
            "Could Not Add Suite Item",
            isPresented: Binding(
                get: { env.suiteHandoffError != nil },
                set: { if !$0 { env.suiteHandoffError = nil } }
            )
        ) {
            Button("OK", role: .cancel) { env.suiteHandoffError = nil }
        } message: {
            Text(env.suiteHandoffError ?? "")
        }
        .alert(
            "Could Not Update Task",
            isPresented: Binding(
                get: { env.sourceDispositionError != nil },
                set: { if !$0 { env.sourceDispositionError = nil } }
            )
        ) {
            Button("OK", role: .cancel) { env.sourceDispositionError = nil }
        } message: {
            Text(env.sourceDispositionError ?? "")
        }
        .preferredColorScheme(colorScheme)
        .onAppear {
            consumePendingIntentRoute()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                consumePendingIntentRoute()
                Task {
                    await env.refreshWaitingForDisposition()
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
        case .handoff(let id):
            Task { @MainActor in
                if let location = await env.importSuiteHandoff(id: id) {
                    tab = location == .today ? .today : .later
                }
            }
        case .endMyDay:
            tab = .today
            env.endMyDayRequested = true
        case .task(let id):
            env.deepLinkTaskId = id
            tab = .today
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

extension RootView {
    /// Regular width (the unfolded iPhone Duo) uses the system tab rail. Compact
    /// width (iPhone, the folded Duo, narrow Split View) keeps the custom bar.
    private var usesCustomBar: Bool { horizontalSizeClass != .regular }

    /// In compact width the system tab bar is hidden; the custom bar and the undo
    /// banner sit below the TabView, outside the tab pages, as on main.
    private func tabContent<Content: View>(_ content: () -> Content) -> some View {
        content()
            .toolbar(usesCustomBar ? .hidden : .automatic, for: .tabBar)
    }

    private var customTabBar: some View {
        VStack(spacing: 0) {
            Divider()
            HStack {
                customTab(.today, title: "Today", systemImage: "sun.max")
                customTab(.later, title: "Later", systemImage: "tray")
                customTab(.history, title: "History", systemImage: "clock")
                customTab(.settings, title: "Settings", systemImage: "gearshape")
            }
            .padding(.vertical, 8)
            // Like the system tab bar: cap text growth so four tabs fit at accessibility
            // sizes; each tab offers the Large Content Viewer instead.
            .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
            .background(Color.tlSurface.ignoresSafeArea(edges: .bottom))
        }
    }

    private func customTab(_ value: AppTab, title: String, systemImage: String) -> some View {
        Button {
            tab = value
        } label: {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                Text(title)
                    .font(.caption2)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
            .foregroundStyle(tab == value ? Color.tlPrimary : Color.tlOutline)
        }
        .buttonStyle(.plain)
        .accessibilityShowsLargeContentViewer {
            Label(title, systemImage: systemImage)
        }
        .accessibilityIdentifier("tab-\(title.lowercased())")
        .accessibilityAddTraits(tab == value ? .isSelected : [])
    }
}

/// The completion undo banner, shown while an undo is pending.
private struct PendingUndoBanner: View {
    @Environment(AppEnvironment.self) private var env

    var body: some View {
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
    }
}
