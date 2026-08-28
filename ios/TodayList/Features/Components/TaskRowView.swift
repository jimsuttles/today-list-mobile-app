import SwiftUI

struct TaskRowView: View {
    let task: TaskItem
    var onComplete: () -> Void
    var onOpen: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onComplete) {
                Image(systemName: "circle")
                    .font(.title3)
                    .foregroundStyle(Color.tlPrimary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Complete \(task.title)")

            Button(action: onOpen) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(task.title)
                        .foregroundStyle(Color.tlOnBackground)
                        .lineLimit(2)
                    if let notes = task.notes, !notes.isEmpty {
                        Text(notes)
                            .font(.caption)
                            .foregroundStyle(Color.tlOutline)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 6)
    }
}
