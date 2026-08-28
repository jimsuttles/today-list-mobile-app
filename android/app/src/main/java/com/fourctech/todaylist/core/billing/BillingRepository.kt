package com.fourctech.todaylist.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.fourctech.todaylist.BuildConfig
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

sealed class PurchaseOutcome {
    data object Success : PurchaseOutcome()
    data object Cancelled : PurchaseOutcome()
    data object Pending : PurchaseOutcome()
    data class Error(val message: String) : PurchaseOutcome()
}

sealed class RestoreOutcome {
    data object Restored : RestoreOutcome()
    data object NothingToRestore : RestoreOutcome()
    data class Error(val message: String) : RestoreOutcome()
}

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val analytics: Analytics,
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _priceLabel = MutableStateFlow<String?>(null)
    val priceLabel: StateFlow<String?> = _priceLabel.asStateFlow()

    private val _productAvailable = MutableStateFlow(false)
    val productAvailable: StateFlow<Boolean> = _productAvailable.asStateFlow()

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    private var productDetails: ProductDetails? = null
    private var purchaseContinuation: (kotlin.coroutines.Continuation<PurchaseOutcome>)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        connectAndRefresh()
    }

    fun connectAndRefresh() {
        if (billingClient.isReady) {
            scope.launch { queryProductAndEntitlement() }
            return
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    _billingReady.value = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        scope.launch { queryProductAndEntitlement() }
                    } else {
                        Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                        if (BuildConfig.DEBUG) {
                            _priceLabel.value = DEBUG_PRICE_LABEL
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _billingReady.value = false
                }
            },
        )
    }

    private suspend fun queryProductAndEntitlement() {
        withContext(Dispatchers.IO) {
            refreshEntitlementFromStore()
            queryProductDetails()
        }
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BuildConfig.REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val detailsResult = suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
                cont.resume(billingResult to detailsList)
            }
        }

        val (result, details) = detailsResult
        if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
            val product = details.first()
            productDetails = product
            _productAvailable.value = true
            _priceLabel.value = product.oneTimePurchaseOfferDetails?.formattedPrice
                ?: DEBUG_PRICE_LABEL.takeIf { BuildConfig.DEBUG }
        } else {
            productDetails = null
            _productAvailable.value = false
            if (BuildConfig.DEBUG) {
                _priceLabel.value = DEBUG_PRICE_LABEL
            } else {
                _priceLabel.value = null
            }
            Log.w(TAG, "Product query failed: ${result.debugMessage}")
        }
    }

    suspend fun refreshEntitlementFromStore(): Boolean {
        if (!billingClient.isReady) return false

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val (result, purchases) = suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
                cont.resume(billingResult to purchaseList)
            }
        }

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            return false
        }

        val owned = purchases.any { purchase ->
            purchase.products.contains(BuildConfig.REMOVE_ADS_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (owned) {
            purchases
                .filter {
                    it.products.contains(BuildConfig.REMOVE_ADS_PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !it.isAcknowledged
                }
                .forEach { acknowledge(it) }
            settingsRepository.setAdsRemovedCached(true)
        }
        return owned
    }

    suspend fun purchase(activity: Activity): PurchaseOutcome {
        analytics.log(AnalyticsEvents.REMOVE_ADS_STARTED)

        // Debug convenience when Play product isn't configured yet.
        if (BuildConfig.DEBUG && productDetails == null) {
            settingsRepository.setAdsRemovedCached(true)
            analytics.log(AnalyticsEvents.REMOVE_ADS_COMPLETED)
            return PurchaseOutcome.Success
        }

        val details = productDetails
            ?: return PurchaseOutcome.Error("Product details unavailable. Please try again later.")

        if (!billingClient.isReady) {
            return PurchaseOutcome.Error("Store unavailable. Please try again later.")
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        return suspendCancellableCoroutine { cont ->
            purchaseContinuation = cont
            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseContinuation = null
                cont.resume(
                    PurchaseOutcome.Error(
                        launchResult.debugMessage.ifBlank {
                            "Purchase couldn't be completed. Please try again later."
                        },
                    ),
                )
            }
        }
    }

    suspend fun restore(): RestoreOutcome {
        if (billingClient.isReady) {
            val owned = refreshEntitlementFromStore()
            return if (owned) {
                analytics.log(AnalyticsEvents.REMOVE_ADS_RESTORED)
                RestoreOutcome.Restored
            } else {
                RestoreOutcome.NothingToRestore
            }
        }

        connectAndRefresh()
        return if (BuildConfig.DEBUG) {
            RestoreOutcome.NothingToRestore
        } else {
            RestoreOutcome.Error("Store unavailable. Please try again later.")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val cont = purchaseContinuation
        purchaseContinuation = null

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases.orEmpty().firstOrNull {
                    it.products.contains(BuildConfig.REMOVE_ADS_PRODUCT_ID)
                }
                when {
                    purchase == null -> cont?.resume(
                        PurchaseOutcome.Error("Purchase couldn't be completed. Please try again later."),
                    )
                    purchase.purchaseState == Purchase.PurchaseState.PENDING -> {
                        cont?.resume(PurchaseOutcome.Pending)
                    }
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED -> {
                        scope.launch {
                            if (!purchase.isAcknowledged) {
                                acknowledge(purchase)
                            }
                            settingsRepository.setAdsRemovedCached(true)
                            analytics.log(AnalyticsEvents.REMOVE_ADS_COMPLETED)
                            cont?.resume(PurchaseOutcome.Success)
                        }
                    }
                    else -> cont?.resume(
                        PurchaseOutcome.Error("Purchase couldn't be completed. Please try again later."),
                    )
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                cont?.resume(PurchaseOutcome.Cancelled)
            }
            else -> {
                cont?.resume(
                    PurchaseOutcome.Error(
                        result.debugMessage.ifBlank {
                            "Purchase couldn't be completed. Please try again later."
                        },
                    ),
                )
            }
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
                cont.resume(Unit)
            }
        }
    }

    companion object {
        private const val TAG = "BillingRepository"
        private const val DEBUG_PRICE_LABEL = "$4.99"
    }
}
