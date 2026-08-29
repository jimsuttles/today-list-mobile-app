import SwiftUI

struct UndoBanner: View {
    let title: String
    var onUndo: () -> Void
    var onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text("Completed \"\(title)\"")
                .font(.subheadline)
                .foregroundStyle(.white)
                .lineLimit(1)
            Spacer(minLength: 8)
            Button(action: onUndo) {
                Text("Undo")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.tlSecondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .buttonStyle(.plain)
            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.9))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Dismiss")
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(Color.tlPrimary)
    }
}
