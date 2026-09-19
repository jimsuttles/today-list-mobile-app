import SwiftUI

struct WatchTodayView: View {
    @EnvironmentObject private var store: WatchTodayStore
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Text("\(store.remainingCount) left")
                            .font(.headline)
                        Spacer()
                        if store.pendingCount > 0 {
                            Image(systemName: "arrow.triangle.2.circlepath")
                                .foregroundStyle(.secondary)
                                .accessibilityLabel("\(store.pendingCount) pending changes")
                        }
                    }
                }

                Section("Today") {
                    if store.items.isEmpty {
                        Text("You're clear")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(store.items) { item in
                            Button {
                                store.toggle(item)
                            } label: {
                                HStack(alignment: .firstTextBaseline) {
                                    Image(systemName: item.isCompleted ? "checkmark.circle.fill" : "circle")
                                    Text(item.title)
                                        .strikethrough(item.isCompleted)
                                        .lineLimit(2)
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .navigationTitle("Today")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showAdd = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Add Today task")
                }
            }
            .sheet(isPresented: $showAdd) {
                WatchQuickAddView()
            }
        }
    }
}

private struct WatchQuickAddView: View {
    @EnvironmentObject private var store: WatchTodayStore
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Task", text: $title)
            }
            .navigationTitle("Quick Add")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        store.addToday(title: title)
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
