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
        TabView(selection: $tab) {
            TodayView()
                .modifier(UndoBannerInset())
                .tabItem { Label("Today", systemImage: "sun.max") }
                .tag(AppTab.today)
            LaterView()
                .modifier(UndoBannerInset())
                .tabItem { Label("Later", systemImage: "tray") }
                .tag(AppTab.later)
            HistoryView()
                .modifier(UndoBannerInset())
                .tabItem { Label("History", systemImage: "clock") }
                .tag(AppTab.history)
            SettingsView()
                .modifier(UndoBannerInset())
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag(AppTab.settings)
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

/// Shows the completion undo banner above the bottom edge of a tab's content,
/// clear of the tab bar on iPhone and of the tab rail on iPhone Duo.
private struct UndoBannerInset: ViewModifier {
    @Environment(AppEnvironment.self) private var env

    func body(content: Content) -> some View {
        content.safeAreaInset(edge: .bottom, spacing: 0) {
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
}
