package com.fourctech.todaylist.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.domain.model.CompletionRecord
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.HistoryRepository
import com.fourctech.todaylist.domain.usecase.RecreateTaskFromHistoryUseCase
import com.fourctech.todaylist.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryDetailUiState(
    val record: CompletionRecord? = null,
    val missing: Boolean = false,
    val recreating: Boolean = false,
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    private val recreateTaskFromHistory: RecreateTaskFromHistoryUseCase,
) : ViewModel() {

    private val completionId: String = checkNotNull(savedStateHandle[Route.HistoryDetail.arg])

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    private val _recreated = MutableSharedFlow<TaskLocation>(extraBufferCapacity = 1)
    val recreated: SharedFlow<TaskLocation> = _recreated.asSharedFlow()

    init {
        viewModelScope.launch {
            val record = historyRepository.getCompletion(completionId)
            _uiState.value = if (record == null) {
                HistoryDetailUiState(missing = true)
            } else {
                HistoryDetailUiState(record = record)
            }
        }
    }

    fun recreate(location: TaskLocation) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recreating = true)
            val created = recreateTaskFromHistory(completionId, location)
            _uiState.value = _uiState.value.copy(recreating = false)
            if (created != null) {
                _recreated.emit(location)
            }
        }
    }
}
