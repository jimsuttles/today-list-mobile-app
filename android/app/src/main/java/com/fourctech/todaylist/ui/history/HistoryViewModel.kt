package com.fourctech.todaylist.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.domain.model.CompletionRecord
import com.fourctech.todaylist.domain.repository.HistoryRepository
import com.fourctech.todaylist.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryDayGroup(
    val date: LocalDate,
    val items: List<CompletionRecord>,
)

data class HistoryUiState(
    val groups: List<HistoryDayGroup> = emptyList(),
    val adsRemoved: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    analytics: Analytics,
) : ViewModel() {

    init {
        analytics.log(AnalyticsEvents.HISTORY_OPENED)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.observeHistory(),
        settingsRepository.observeSettings(),
    ) { records, settings ->
        val groups = records
            .groupBy { it.completionDate }
            .entries
            .sortedByDescending { it.key }
            .map { (date, items) -> HistoryDayGroup(date = date, items = items) }
        HistoryUiState(groups = groups, adsRemoved = settings.adsRemovedCached)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
