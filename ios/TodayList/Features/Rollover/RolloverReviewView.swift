import SwiftUI

struct RolloverReviewView: View {
    @Environment(AppEnvironment.self) private var env
    let unfinished: [TaskItem]
    let missedDays: Int
    @State private var decisions: [String: TaskLocation]

    init(unfinished: [TaskItem], missedDays: Int) {
        self.unfinished = unfinished
        self.missedDays = missedDays
        _decisions = State(initialValue: Dictionary(
            uniqueKeysWithValues: unfinished.map { ($0.id, TaskLocation.today) }
        ))
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(missedDays == 1
                         ? "A new day started. What should happen to unfinished Today tasks?"
                         : "It's been \(missedDays) days. Review unfinished Today tasks.")
                        .font(.subheadline)
                        .foregroundStyle(Color.tlOutline)
                }
                ForEach(unfinished) { task in
                    Picker(task.title, selection: binding(for: task.id)) {
                        Text("Keep on Today").tag(TaskLocation.today)
                        Text("Move to Later").tag(TaskLocation.later)
                    }
                }
            }
            .navigationTitle("Unfinished tasks")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        Task {
                            await env.rolloverManager.applyDecisions(decisions)
                            env.rolloverReview = nil
                        }
                    }
                }
            }
            .interactiveDismissDisabled()
        }
    }

    private func binding(for id: String) -> Binding<TaskLocation> {
        Binding(
            get: { decisions[id] ?? .today },
            set: { decisions[id] = $0 }
        )
    }
}
