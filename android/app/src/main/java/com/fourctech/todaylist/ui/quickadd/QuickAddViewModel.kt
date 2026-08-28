package com.fourctech.todaylist.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.analytics.AnalyticsParams
import com.fourctech.todaylist.core.analytics.toAnalyticsValue
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickAddUiState(
    val title: String = "",
    val location: TaskLocation = TaskLocation.TODAY,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val analytics: Analytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null) }
    }

    fun onLocationChange(location: TaskLocation) {
        _uiState.update { it.copy(location = location) }
    }

    fun reset(defaultLocation: TaskLocation) {
        _uiState.value = QuickAddUiState(location = defaultLocation)
    }

    fun save(onSaved: () -> Unit) {
        val title = _uiState.value.title.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter a task title") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val location = _uiState.value.location
            taskRepository.createTask(title = title, location = location)
            analytics.log(
                AnalyticsEvents.TASK_CREATED,
                mapOf(AnalyticsParams.LOCATION to location.toAnalyticsValue()),
            )
            _uiState.update { it.copy(isSaving = false, title = "") }
            onSaved()
        }
    }
}
