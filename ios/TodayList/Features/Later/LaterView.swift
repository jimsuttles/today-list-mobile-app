import SwiftUI

struct LaterView: View {
    @Environment(\.appEnvironment) private var env
    @State private var tasks: [TaskItem] = []
    @State private var showQuickAdd = false
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if tasks.isEmpty {
                    ContentUnavailableView(
                        "Nothing later",
                        systemImage: "tray",
                        description: Text("Park tasks here for another day")
                    )
                } else {
                    List {
                        ForEach(tasks) { task in
                            TaskRowView(
                                task: task,
                                onComplete: {
                                    Task { await env.completeTask(task) }
                                },
                                onOpen: { path.append(task.id) }
                            )
                            .swipeActions(edge: .trailing) {
                                Button("Today") {
                                    Task { try? await env.taskRepository.moveToToday(taskId: task.id) }
                                }
                                .tint(Color.tlPrimary)
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
            .navigationTitle("Later")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showQuickAdd = true } label: {
                        Image(systemName: "plus")
                    }
                }
                ToolbarItem(placement: .topBarLeading) {
                    if !tasks.isEmpty { EditButton() }
                }
            }
            .navigationDestination(for: String.self) { id in
                TaskDetailView(taskId: id)
            }
            .sheet(isPresented: $showQuickAdd) {
                QuickAddView(defaultLocation: .later)
            }
            .task {
                for await list in env.taskRepository.observeTasks(location: .later) {
                    tasks = list
                }
            }
        }
    }

    private func move(from source: IndexSet, to destination: Int) {
        var ordered = tasks
        ordered.move(fromOffsets: source, toOffset: destination)
        tasks = ordered
        Task {
            try? await env.taskRepository.reorderTasks(
                location: .later,
                orderedTaskIds: ordered.map(\.id)
            )
        }
    }
}
