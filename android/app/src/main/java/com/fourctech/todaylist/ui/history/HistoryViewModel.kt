package com.fourctech.todaylist.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.domain.model.CompletionRecord
import com.fourctech.todaylist.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryDayGroup(
    val date: LocalDate,
    val items: List<CompletionRecord>,
)

data class HistoryUiState(
    val groups: List<HistoryDayGroup> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: HistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = historyRepository.observeHistory()
        .map { records ->
            val groups = records
                .groupBy { it.completionDate }
                .entries
                .sortedByDescending { it.key }
                .map { (date, items) -> HistoryDayGroup(date = date, items = items) }
            HistoryUiState(groups = groups)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
