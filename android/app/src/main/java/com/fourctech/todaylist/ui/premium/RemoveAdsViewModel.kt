package com.fourctech.todaylist.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.billing.BillingRepository
import com.fourctech.todaylist.core.billing.PurchaseOutcome
import com.fourctech.todaylist.core.billing.RestoreOutcome
import com.fourctech.todaylist.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemoveAdsUiState(
    val adsRemoved: Boolean = false,
    val priceLabel: String? = null,
    val productAvailable: Boolean = false,
    val loadingProduct: Boolean = true,
    val purchasing: Boolean = false,
    val restoring: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class RemoveAdsViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    settingsRepository: SettingsRepository,
    private val analytics: Analytics,
) : ViewModel() {
    private val transient = MutableStateFlow(TransientUi(loadingProduct = true))

    val uiState: StateFlow<RemoveAdsUiState> = combine(
        settingsRepository.observeSettings(),
        billingRepository.priceLabel,
        billingRepository.productAvailable,
        transient,
    ) { settings, price, available, t ->
        RemoveAdsUiState(
            adsRemoved = settings.adsRemovedCached,
            priceLabel = price,
            productAvailable = available,
            loadingProduct = t.loadingProduct && price == null && !settings.adsRemovedCached,
            purchasing = t.purchasing,
            restoring = t.restoring,
            message = t.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemoveAdsUiState(),
    )

    fun onAppear() {
        analytics.log(AnalyticsEvents.REMOVE_ADS_VIEWED)
        billingRepository.connectAndRefresh()
        viewModelScope.launch {
            transient.update { it.copy(loadingProduct = true) }
            billingRepository.refreshEntitlementFromStore()
            transient.update { it.copy(loadingProduct = false) }
        }
    }

    fun purchase(activity: Activity) {
        viewModelScope.launch {
            transient.update { it.copy(purchasing = true, message = null) }
            when (val outcome = billingRepository.purchase(activity)) {
                PurchaseOutcome.Success -> {
                    transient.update {
                        it.copy(purchasing = false, message = "Ads removed. Thank you!")
                    }
                }
                PurchaseOutcome.Cancelled -> {
                    transient.update { it.copy(purchasing = false) }
                }
                PurchaseOutcome.Pending -> {
                    transient.update {
                        it.copy(
                            purchasing = false,
                            message = "Your purchase is pending approval. Ads will be removed once the store confirms it.",
                        )
                    }
                }
                is PurchaseOutcome.Error -> {
                    transient.update {
                        it.copy(purchasing = false, message = outcome.message)
                    }
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            transient.update { it.copy(restoring = true, message = null) }
            when (val outcome = billingRepository.restore()) {
                RestoreOutcome.Restored -> {
                    transient.update {
                        it.copy(restoring = false, message = "Purchases restored.")
                    }
                }
                RestoreOutcome.NothingToRestore -> {
                    transient.update {
                        it.copy(restoring = false, message = "No purchases found for this account.")
                    }
                }
                is RestoreOutcome.Error -> {
                    transient.update {
                        it.copy(restoring = false, message = outcome.message)
                    }
                }
            }
        }
    }

    fun consumeMessage() {
        transient.update { it.copy(message = null) }
    }

    private data class TransientUi(
        val loadingProduct: Boolean = false,
        val purchasing: Boolean = false,
        val restoring: Boolean = false,
        val message: String? = null,
    )
}
