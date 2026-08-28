import SwiftUI

struct HistoryView: View {
    @Environment(\.appEnvironment) private var env
    @State private var records: [CompletionRecord] = []
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if records.isEmpty {
                    ContentUnavailableView(
                        "No history yet",
                        systemImage: "clock",
                        description: Text("Completed tasks will show up here")
                    )
                } else {
                    List(records) { record in
                        Button {
                            path.append(record.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(record.titleSnapshot)
                                    .foregroundStyle(Color.tlOnBackground)
                                Text(record.completedAt.formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption)
                                    .foregroundStyle(Color.tlOutline)
                            }
                        }
                        .listRowBackground(Color.tlSurface)
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color.tlBackground)
            .navigationTitle("History")
            .navigationDestination(for: String.self) { id in
                HistoryDetailView(completionId: id)
            }
            .task {
                for await list in env.historyRepository.observeCompletions() {
                    records = list
                }
            }
        }
    }
}

struct HistoryDetailView: View {
    @Environment(\.appEnvironment) private var env
    @Environment(\.dismiss) private var dismiss
    let completionId: String
    @State private var record: CompletionRecord?

    var body: some View {
        Form {
            if let record {
                Section("Completed") {
                    Text(record.titleSnapshot)
                    Text(record.completedAt.formatted())
                }
                Section {
                    Button("Recreate on Today") {
                        Task {
                            _ = try? await env.recreateFromHistory.execute(
                                completionId: completionId,
                                location: .today
                            )
                            dismiss()
                        }
                    }
                    Button("Recreate on Later") {
                        Task {
                            _ = try? await env.recreateFromHistory.execute(
                                completionId: completionId,
                                location: .later
                            )
                            dismiss()
                        }
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle("Details")
        .task {
            record = await env.historyRepository.getCompletion(id: completionId)
        }
    }
}
