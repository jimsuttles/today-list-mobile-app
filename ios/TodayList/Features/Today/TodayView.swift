import SwiftUI

struct TodayView: View {
    @Environment(AppEnvironment.self) private var env
    @State private var tasks: [TaskItem] = []
    @State private var showQuickAdd = false
    @State private var quickAddLocation: TaskLocation = .today
    @State private var showEndMyDay = false
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
                        Section {
                            ForEach(tasks) { task in
                                TaskRowView(
                                    task: task,
                                    onComplete: {
                                        Task { await env.completeTask(task) }
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
                                .deleteDisabled(true)
                            }
                            .onMove(perform: move)
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                    .environment(\.editMode, .constant(.active))
                }
            }
            .background(Color.tlBackground)
            .navigationTitle("Today")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        showEndMyDay = true
                    } label: {
                        Image(systemName: "moon.stars")
                    }
                    .accessibilityLabel("End My Day")

                    Button {
                        quickAddLocation = .today
                        showQuickAdd = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Add task")
                }
            }
            .navigationDestination(for: String.self) { id in
                TaskDetailView(taskId: id)
            }
            .sheet(isPresented: $showQuickAdd) {
                QuickAddView(defaultLocation: quickAddLocation)
            }
            .sheet(isPresented: $showEndMyDay) {
                EndMyDayView()
            }
            .task {
                for await list in env.taskRepository.observeTasks(location: .today) {
                    tasks = list
                    WidgetSnapshot.publish(todayTitles: list.map(\.title))
                }
            }
            .onChange(of: env.requestedQuickAddLocation) { _, location in
                guard let location else { return }
                quickAddLocation = location
                showQuickAdd = true
                env.requestedQuickAddLocation = nil
            }
            .onChange(of: env.endMyDayRequested) { _, requested in
                guard requested else { return }
                showEndMyDay = true
                env.endMyDayRequested = false
            }
        }
    }

    private var progressLabel: String {
        let left = tasks.count
        let done = env.sessionCompletedCount
        if done == 0 {
            return left == 1 ? "1 left" : "\(left) left"
        }
        return "\(done) done · \(left) left"
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
