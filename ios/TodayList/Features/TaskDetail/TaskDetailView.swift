import SwiftUI

struct TaskDetailView: View {
    @Environment(\.appEnvironment) private var env
    @Environment(\.dismiss) private var dismiss
    let taskId: String

    @State private var title = ""
    @State private var notes = ""
    @State private var location: TaskLocation = .today
    @State private var reminderEnabled = false
    @State private var reminderAt = Date().addingTimeInterval(3600)
    @State private var repeatOption: RepeatOption = .none
    @State private var showDeleteConfirm = false
    @State private var loaded = false
    @State private var original: TaskItem?

    var body: some View {
        Form {
            Section {
                TextField("Title", text: $title)
                TextField("Notes", text: $notes, axis: .vertical)
                    .lineLimit(3...6)
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
        loaded = true
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
        task.recurrence = RecurrenceRule.from(option: repeatOption, startDate: task.scheduledDate ?? CalendarHelpers.today())
        try? await env.taskRepository.updateTask(task)
        if let at = task.reminderAt {
            let granted = await env.notificationScheduler.requestPermissionIfNeeded()
            if granted {
                await env.notificationScheduler.scheduleReminder(taskId: task.id, title: task.title, at: at)
            }
            await env.settingsRepository.updateSettings { $0.notificationPermissionPrompted = true }
        } else {
            await env.notificationScheduler.cancelReminder(taskId: task.id)
        }
        dismiss()
    }

    private func delete(_ scope: DeleteScope) async {
        await env.notificationScheduler.cancelReminder(taskId: taskId)
        try? await env.taskRepository.deleteTask(taskId: taskId, scope: scope)
        dismiss()
    }
}
