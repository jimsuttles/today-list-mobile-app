import Foundation
import StoreKit

@MainActor
@Observable
final class StoreKitBilling {
    static let removeAdsProductId = "com.fourctech.todaylist.removeads"

    private let settingsRepository: SettingsRepository
    var products: [Product] = []
    var isPurchasing = false
    var lastError: String?

    init(settingsRepository: SettingsRepository) {
        self.settingsRepository = settingsRepository
    }

    var adsRemoved: Bool {
        settingsRepository.currentSettings().adsRemovedCached
    }

    func loadProducts() async {
        do {
            products = try await Product.products(for: [Self.removeAdsProductId])
        } catch {
            lastError = error.localizedDescription
        }
    }

    func purchaseRemoveAds() async {
        var product = products.first(where: { $0.id == Self.removeAdsProductId })
        if product == nil {
            product = try? await Product.products(for: [Self.removeAdsProductId]).first
        }
        guard let product else {
            lastError = "Product unavailable"
            return
        }
        isPurchasing = true
        defer { isPurchasing = false }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                if case .verified = verification {
                    await settingsRepository.updateSettings { $0.adsRemovedCached = true }
                }
            case .userCancelled, .pending:
                break
            @unknown default:
                break
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func restorePurchases() async {
        do {
            try await AppStore.sync()
            for await result in Transaction.currentEntitlements {
                if case .verified(let transaction) = result,
                   transaction.productID == Self.removeAdsProductId {
                    await settingsRepository.updateSettings { $0.adsRemovedCached = true }
                    return
                }
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func listenForTransactions() -> Task<Void, Never> {
        Task { [weak self] in
            for await result in Transaction.updates {
                guard let self else { return }
                if case .verified(let transaction) = result,
                   transaction.productID == Self.removeAdsProductId {
                    await self.settingsRepository.updateSettings { $0.adsRemovedCached = true }
                    await transaction.finish()
                }
            }
        }
    }
}
