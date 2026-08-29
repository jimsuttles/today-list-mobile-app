import SwiftUI

struct QuickAddView: View {
    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss

    var defaultLocation: TaskLocation = .today
    @State private var title = ""
    @State private var location: TaskLocation

    init(defaultLocation: TaskLocation = .today) {
        self.defaultLocation = defaultLocation
        _location = State(initialValue: defaultLocation)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Task title", text: $title)
                    .submitLabel(.done)
                    .onSubmit { save() }
                Picker("List", selection: $location) {
                    Text("Today").tag(TaskLocation.today)
                    Text("Later").tag(TaskLocation.later)
                }
                .pickerStyle(.segmented)
            }
            .navigationTitle("Quick add")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { save() }
                        .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func save() {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task {
            _ = try? await env.taskRepository.createTask(
                title: trimmed,
                notes: nil,
                location: location,
                reminderAt: nil,
                scheduledDate: nil,
                recurrence: nil
            )
            dismiss()
        }
    }
}
