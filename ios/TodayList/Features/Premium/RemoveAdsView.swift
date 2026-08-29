import SwiftUI

struct RemoveAdsView: View {
    @Environment(AppEnvironment.self) private var env

    var body: some View {
        Form {
            Section {
                Text("Remove banner ads with a one-time purchase. Your tasks stay on this device.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            if env.settings.adsRemovedCached {
                Section {
                    Label("Ads removed", systemImage: "checkmark.seal.fill")
                        .foregroundStyle(Color.tlPrimary)
                }
            } else {
                Section {
                    Button {
                        Task { await env.billing.purchaseRemoveAds() }
                    } label: {
                        if env.billing.isPurchasing {
                            ProgressView()
                        } else {
                            Text(purchaseLabel)
                        }
                    }
                    .disabled(env.billing.isPurchasing)
                }
            }
            Section {
                Button("Restore purchases") {
                    Task { await env.billing.restorePurchases() }
                }
            }
            if let err = env.billing.lastError {
                Section {
                    Text(err).foregroundStyle(.red).font(.caption)
                }
            }
        }
        .navigationTitle("Remove Ads")
        .task { await env.billing.loadProducts() }
    }

    private var purchaseLabel: String {
        if let product = env.billing.products.first {
            return "Remove Ads — \(product.displayPrice)"
        }
        return "Remove Ads"
    }
}
