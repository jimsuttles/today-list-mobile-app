import SwiftUI

struct EndMyDayView: View {
    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss

    @State private var unfinished: [TaskItem] = []
    @State private var completedToday: [CompletionRecord] = []
    @State private var tomorrow: [TaskItem] = []
    @State private var quickAddTitle = ""
    @State private var summary: EndMyDaySummary?
    @State private var movedToTomorrowCount = 0
    @State private var movedToLaterCount = 0
    @State private var completedDuringReviewCount = 0
    @State private var deletedCount = 0

    var body: some View {
        NavigationStack {
            List {
                reviewSection

                if unfinished.isEmpty {
                    tomorrowSection
                }
            }
            .navigationTitle("End My Day")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Finish") {
                        summary = EndMyDaySummary(
                            done: completedToday.count,
                            movedToTomorrow: movedToTomorrowCount,
                            movedToLater: movedToLaterCount,
                            deleted: deletedCount
                        )
                    }
                    .disabled(!unfinished.isEmpty)
                }
            }
            .alert(item: $summary) { summary in
                Alert(
                    title: Text("Today complete"),
                    message: Text(summary.message),
                    dismissButton: .default(Text("Done")) {
                        dismiss()
                    }
                )
            }
            .task {
                await observeToday()
            }
            .task {
                await observeCompletions()
            }
        }
        .interactiveDismissDisabled(!unfinished.isEmpty)
    }

    @ViewBuilder
    private var reviewSection: some View {
        Section {
            if !completedToday.isEmpty {
                ForEach(completedToday) { record in
                    Label(record.titleSnapshot, systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }

            if unfinished.isEmpty {
                Label("Everything from Today is resolved.", systemImage: "checkmark.seal.fill")
                    .foregroundStyle(Color.tlPrimary)
            } else {
                ForEach(unfinished) { task in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(task.title)
                            .font(.body.weight(.medium))

                        HStack {
                            Button("Tomorrow") {
                                Task { await moveToTomorrow(task) }
                            }
                            .buttonStyle(.bordered)

                            Button("Later") {
                                Task { await moveToLater(task) }
                            }
                            .buttonStyle(.bordered)

                            Menu {
                                Button("Mark Done") {
                                    Task { await complete(task) }
                                }
                                Button("Delete", role: .destructive) {
                                    Task { await delete(task) }
                                }
                            } label: {
                                Image(systemName: "ellipsis.circle")
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        } header: {
            Text("Review Today")
        } footer: {
            if !unfinished.isEmpty {
                Text("Resolve every unfinished item before finishing the day.")
            }
        }
    }

    @ViewBuilder
    private var tomorrowSection: some View {
        Section("Tomorrow") {
            if tomorrow.isEmpty {
                Text("Nothing planned for tomorrow.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(tomorrow) { task in
                    Text(task.title)
                }
                .onMove(perform: reorderTomorrow)
            }

            HStack {
                TextField("Add for tomorrow", text: $quickAddTitle)
                    .submitLabel(.done)
                    .onSubmit { addTomorrow() }

                Button {
                    addTomorrow()
                } label: {
                    Image(systemName: "plus.circle.fill")
                }
                .disabled(quickAddTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .accessibilityLabel("Add task for tomorrow")
            }
        }
    }

    private func observeToday() async {
        for await tasks in env.taskRepository.observeTasks(location: .today) {
            unfinished = tasks
            tomorrow = await env.taskRepository.tasksScheduled(on: tomorrowDate)
        }
    }

    private func observeCompletions() async {
        for await records in env.historyRepository.observeCompletions() {
            completedToday = records.filter {
                Calendar.current.isDate($0.completionDate, inSameDayAs: Date())
            }
        }
    }

    private var tomorrowDate: Date {
        Calendar.current.date(byAdding: .day, value: 1, to: CalendarHelpers.today())!
    }

    private func moveToTomorrow(_ task: TaskItem) async {
        guard var latest = await env.taskRepository.getTask(id: task.id) else { return }
        latest.location = .today
        latest.scheduledDate = tomorrowDate
        do {
            try await env.taskRepository.updateTask(latest)
            movedToTomorrowCount += 1
            tomorrow = await env.taskRepository.tasksScheduled(on: tomorrowDate)
            await env.refreshWidget()
        } catch {
            Analytics.log("end_my_day_tomorrow_failed")
        }
    }

    private func moveToLater(_ task: TaskItem) async {
        do {
            try await env.taskRepository.moveToLater(taskId: task.id)
            movedToLaterCount += 1
            await env.refreshWidget()
        } catch {
            Analytics.log("end_my_day_later_failed")
        }
    }

    private func complete(_ task: TaskItem) async {
        let before = completedToday.count
        await env.completeTask(task)
        if completedToday.count == before {
            completedDuringReviewCount += 1
        }
    }

    private func delete(_ task: TaskItem) async {
        do {
            try await env.taskRepository.deleteTask(taskId: task.id, scope: .thisTask)
            deletedCount += 1
            await env.notificationScheduler.cancelReminder(taskId: task.id)
            await env.refreshWidget()
        } catch {
            Analytics.log("end_my_day_delete_failed")
        }
    }

    private func addTomorrow() {
        let title = quickAddTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else { return }
        quickAddTitle = ""

        Task {
            _ = try? await env.taskRepository.createTask(
                title: title,
                notes: nil,
                location: .today,
                reminderAt: nil,
                scheduledDate: tomorrowDate,
                recurrence: nil
            )
            tomorrow = await env.taskRepository.tasksScheduled(on: tomorrowDate)
        }
    }

    private func reorderTomorrow(from source: IndexSet, to destination: Int) {
        var ordered = tomorrow
        ordered.move(fromOffsets: source, toOffset: destination)
        tomorrow = ordered

        Task {
            try? await env.taskRepository.reorderTasks(
                location: .today,
                orderedTaskIds: ordered.map(\.id)
            )
        }
    }
}

private struct EndMyDaySummary: Identifiable {
    let id = UUID()
    let done: Int
    let movedToTomorrow: Int
    let movedToLater: Int
    let deleted: Int

    var message: String {
        var lines = ["\(done) done"]
        if movedToTomorrow > 0 { lines.append("\(movedToTomorrow) moved to tomorrow") }
        if movedToLater > 0 { lines.append("\(movedToLater) moved to later") }
        if deleted > 0 { lines.append("\(deleted) deleted") }
        return lines.joined(separator: "\n")
    }
}
