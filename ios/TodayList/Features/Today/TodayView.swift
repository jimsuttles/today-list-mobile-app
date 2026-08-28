import SwiftUI

struct TodayView: View {
    @Environment(\.appEnvironment) private var env
    @State private var tasks: [TaskItem] = []
    @State private var sessionCompleted = 0
    @State private var showQuickAdd = false
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if tasks.isEmpty {
                    ContentUnavailableView(
                        "You're clear",
                        systemImage: "checkmark.circle",
                        description: Text("Add something for today")
                    )
                } else {
                    List {
                        Section {
                            Text(progressLabel)
                                .font(.subheadline)
                                .foregroundStyle(Color.tlOutline)
                                .listRowBackground(Color.clear)
                        }
                        ForEach(tasks) { task in
                            TaskRowView(
                                task: task,
                                onComplete: {
                                    Task {
                                        await env.completeTask(task)
                                        sessionCompleted += 1
                                    }
                                },
                                onOpen: { path.append(task.id) }
                            )
                            .swipeActions(edge: .trailing) {
                                Button("Later") {
                                    Task { try? await env.taskRepository.moveToLater(taskId: task.id) }
                                }
                                .tint(Color.tlSecondary)
                            }
                            .listRowBackground(Color.tlSurface)
                        }
                        .onMove(perform: move)
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color.tlBackground)
            .navigationTitle("Today")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showQuickAdd = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Add task")
                }
                ToolbarItem(placement: .topBarLeading) {
                    if !tasks.isEmpty {
                        EditButton()
                    }
                }
            }
            .navigationDestination(for: String.self) { id in
                TaskDetailView(taskId: id)
            }
            .sheet(isPresented: $showQuickAdd) {
                QuickAddView(defaultLocation: .today)
            }
            .task {
                for await list in env.taskRepository.observeTasks(location: .today) {
                    tasks = list
                    WidgetSnapshot.publish(todayTitles: list.map(\.title))
                }
            }
        }
    }

    private var progressLabel: String {
        let left = tasks.count
        if sessionCompleted == 0 {
            return left == 1 ? "1 left" : "\(left) left"
        }
        return "\(sessionCompleted) done · \(left) left"
    }

    private func move(from source: IndexSet, to destination: Int) {
        var ordered = tasks
        ordered.move(fromOffsets: source, toOffset: destination)
        tasks = ordered
        Task {
            try? await env.taskRepository.reorderTasks(
                location: .today,
                orderedTaskIds: ordered.map(\.id)
            )
        }
    }
}
