import Foundation
import SwiftUI

/// Banner ads without linking Google Mobile Ads SDK in CI.
/// Shows a placeholder strip when ads are enabled; swap in GADBannerView when AdMob units are configured.
struct AdBannerSlot: View {
    let adsRemoved: Bool

    var body: some View {
        if !adsRemoved {
            HStack {
                Image(systemName: "megaphone.fill")
                Text("Ad")
                    .font(.caption)
                Spacer()
                Text("Remove ads in Settings")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .background(Color.tlSurfaceVariant)
            .accessibilityLabel("Advertisement placeholder")
        }
    }
}

enum AdsConfig {
    /// Replace with production AdMob app/banner IDs when ready.
    static let appIdPlaceholder = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"
    static let bannerUnitIdPlaceholder = "ca-app-pub-3940256099942544/2934735716" // Google sample
}
