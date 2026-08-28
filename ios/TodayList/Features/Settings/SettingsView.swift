import SwiftUI

struct SettingsView: View {
    @Environment(\.appEnvironment) private var env
    @State private var confirmClearHistory = false
    @State private var confirmDeleteAll = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Appearance") {
                    Picker("Theme", selection: themeBinding) {
                        ForEach(ThemeMode.allCases, id: \.self) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                }
                Section("Unfinished tasks") {
                    Picker("When a new day starts", selection: rolloverBinding) {
                        ForEach(RolloverMode.allCases, id: \.self) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                }
                Section("Notifications") {
                    Button("Open system settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                }
                Section("Week starts") {
                    Picker("Week starts", selection: weekStartBinding) {
                        ForEach(WeekStart.allCases, id: \.self) { w in
                            Text(w.title).tag(w)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Experience") {
                    Toggle("Haptics", isOn: hapticsBinding)
                }
                Section("Premium") {
                    if env.settings.adsRemovedCached {
                        Label("Ads removed", systemImage: "checkmark.seal.fill")
                    } else {
                        NavigationLink("Remove Ads") {
                            RemoveAdsView()
                        }
                    }
                    Button("Restore purchases") {
                        Task { await env.billing.restorePurchases() }
                    }
                }
                Section("Data") {
                    Button("Clear History…", role: .destructive) { confirmClearHistory = true }
                    Button("Delete All App Data…", role: .destructive) { confirmDeleteAll = true }
                }
                Section("Support") {
                    Link("Send Feedback", destination: mailto("Today List Feedback"))
                    Link("Report a Problem", destination: mailto("Today List Problem Report"))
                }
                Section("Legal") {
                    Link("Privacy Policy", destination: URL(string: "https://www.4ctech.io/today-list/privacy/")!)
                    Link("Terms of Use", destination: URL(string: "https://www.4ctech.io/today-list/terms/")!)
                }
                Section("About") {
                    LabeledContent("Version", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                    Text("© \(Calendar.current.component(.year, from: Date())) 4CTech, LLC")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .confirmationDialog("Clear History?", isPresented: $confirmClearHistory, titleVisibility: .visible) {
                Button("Clear History", role: .destructive) {
                    Task { try? await env.historyRepository.clearHistory() }
                }
                Button("Cancel", role: .cancel) {}
            }
            .confirmationDialog("Delete all app data?", isPresented: $confirmDeleteAll, titleVisibility: .visible) {
                Button("Delete All", role: .destructive) {
                    Task {
                        try? await env.taskRepository.deleteAllTasks()
                        await env.settingsRepository.resetPreferencesKeepingEntitlement()
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }

    private var themeBinding: Binding<ThemeMode> {
        Binding(
            get: { env.settings.themeMode },
            set: { v in Task { await env.settingsRepository.updateSettings { $0.themeMode = v } } }
        )
    }

    private var rolloverBinding: Binding<RolloverMode> {
        Binding(
            get: { env.settings.rolloverMode },
            set: { v in Task { await env.settingsRepository.updateSettings { $0.rolloverMode = v } } }
        )
    }

    private var weekStartBinding: Binding<WeekStart> {
        Binding(
            get: { env.settings.weekStart },
            set: { v in Task { await env.settingsRepository.updateSettings { $0.weekStart = v } } }
        )
    }

    private var hapticsBinding: Binding<Bool> {
        Binding(
            get: { env.settings.hapticsEnabled },
            set: { v in Task { await env.settingsRepository.updateSettings { $0.hapticsEnabled = v } } }
        )
    }

    private func mailto(_ subject: String) -> URL {
        let encoded = subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? subject
        return URL(string: "mailto:jim@4ctech.io?subject=\(encoded)")!
    }
}

import UIKit
