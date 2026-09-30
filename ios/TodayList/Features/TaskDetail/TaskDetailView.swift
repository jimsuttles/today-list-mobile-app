import SwiftUI

struct TaskDetailView: View {
    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    let taskId: String

    @State private var title = ""
    @State private var notes = ""
    @State private var location: TaskLocation = .today
    @State private var reminderEnabled = false
    @State private var reminderAt = Date().addingTimeInterval(3600)
    @State private var repeatOption: RepeatOption = .none
    @State private var showDeleteConfirm = false
    @State private var promotionError: String?
    @State private var isPromoting = false
    @State private var original: TaskItem?

    private var noteLinks: [URL] {
        LinkDetector.urls(in: notes)
    }

    var body: some View {
        Form {
            Section {
                TextField("Title", text: $title)
            }
            Section("Notes") {
                TextField("Notes (URLs open in Safari)", text: $notes, axis: .vertical)
                    .lineLimit(4...10)
                ForEach(noteLinks, id: \.absoluteString) { url in
                    Button {
                        openURL(url)
                    } label: {
                        Label(url.absoluteString, systemImage: "safari")
                            .lineLimit(2)
                    }
                }
            }
            Section("List") {
                Picker("List", selection: $location) {
                    Text("Today").tag(TaskLocation.today)
                    Text("Later").tag(TaskLocation.later)
                }
                .pickerStyle(.segmented)
            }
            Section("Reminder") {
                Toggle("Remind me", isOn: $reminderEnabled)
                if reminderEnabled {
                    DatePicker("When", selection: $reminderAt)
                }
            }
            Section("Repeat") {
                Picker("Repeat", selection: $repeatOption) {
                    ForEach(RepeatOption.allCases) { opt in
                        Text(opt.title).tag(opt)
                    }
                }
            }
            Section("Actions") {
                Button {
                    Task { await promoteToTop3() }
                } label: {
                    Label("Promote to Top 3", systemImage: "arrow.up.forward.app")
                }
                .disabled(isPromoting || original?.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty != false)
            }
            Section {
                Button("Delete…", role: .destructive) {
                    showDeleteConfirm = true
                }
            }
        }
        .navigationTitle("Task")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { Task { await save() } }
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .confirmationDialog("Delete task", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Delete this task", role: .destructive) {
                Task { await delete(.thisTask) }
            }
            if original?.recurrence != nil {
                Button("Delete entire series", role: .destructive) {
                    Task { await delete(.entireSeries) }
                }
            }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Could Not Promote Task", isPresented: promotionErrorBinding) {
            Button("OK", role: .cancel) { promotionError = nil }
        } message: {
            Text(promotionError ?? "")
        }
        .task { await load() }
    }

    private func load() async {
        guard let task = await env.taskRepository.getTask(id: taskId) else {
            dismiss()
            return
        }
        original = task
        title = task.title
        notes = task.notes ?? ""
        location = task.location
        if let r = task.reminderAt {
            reminderEnabled = true
            reminderAt = r
        }
        repeatOption = task.recurrence?.asRepeatOption ?? .none
    }

    private func save() async {
        var task = original
        if task == nil {
            task = await env.taskRepository.getTask(id: taskId)
        }
        guard var task else { return }
        task.title = title.trimmingCharacters(in: .whitespacesAndNewlines)
        task.notes = notes.isEmpty ? nil : notes
        task.location = location
        task.reminderAt = reminderEnabled ? reminderAt : nil
        task.recurrence = RecurrenceRule.from(
            option: repeatOption,
            startDate: task.scheduledDate ?? CalendarHelpers.today()
        )
        try? await env.taskRepository.updateTask(task)
        if let at = task.reminderAt {
            let granted = await env.notificationScheduler.requestPermissionIfNeeded()
            if granted {
                await env.notificationScheduler.scheduleReminder(
                    taskId: task.id,
                    title: task.title,
                    at: at
                )
            }
            await env.settingsRepository.updateSettings { $0.notificationPermissionPrompted = true }
        } else {
            await env.notificationScheduler.cancelReminder(taskId: task.id)
        }
        dismiss()
    }

    private func promoteToTop3() async {
        guard !isPromoting else { return }
        isPromoting = true
        defer { isPromoting = false }

        guard let task = await env.taskRepository.getTask(id: taskId),
              !task.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            promotionError = "This task needs a title before it can be promoted."
            return
        }

        let payload = SuiteHandoffStore.top3Payload(for: task)
        do {
            try SuiteHandoffStore.writePending(payload)
        } catch let error as SuiteHandoffError {
            promotionError = error.localizedDescription
            return
        } catch {
            promotionError = "Top 3 promotion could not be prepared. Your Today List task was not changed."
            return
        }

        let accepted = await withCheckedContinuation { continuation in
            openURL(SuiteHandoffStore.top3URL(for: payload.id)) { accepted in
                continuation.resume(returning: accepted)
            }
        }
        guard accepted else {
            promotionError = "Top 3 could not be opened. Your Today List task was not changed."
            return
        }
    }

    private func delete(_ scope: DeleteScope) async {
        await env.notificationScheduler.cancelReminder(taskId: taskId)
        try? await env.taskRepository.deleteTask(taskId: taskId, scope: scope)
        dismiss()
    }

    private var promotionErrorBinding: Binding<Bool> {
        Binding(
            get: { promotionError != nil },
            set: { if !$0 { promotionError = nil } }
        )
    }
}
