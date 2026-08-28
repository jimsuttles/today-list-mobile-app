import SwiftUI

struct UndoBanner: View {
    let title: String
    var onUndo: () -> Void
    var onDismiss: () -> Void

    var body: some View {
        HStack {
            Text("Completed \"\(title)\"")
                .font(.subheadline)
                .lineLimit(1)
            Spacer()
            Button("Undo", action: onUndo)
                .fontWeight(.semibold)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
            }
            .accessibilityLabel("Dismiss")
        }
        .padding()
        .background(Color.tlOnBackground.opacity(0.9))
        .foregroundStyle(Color.tlSurface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding()
    }
}
