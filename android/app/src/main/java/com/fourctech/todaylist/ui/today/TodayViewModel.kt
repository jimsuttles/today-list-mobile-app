package com.fourctech.todaylist.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.analytics.AnalyticsParams
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import com.fourctech.todaylist.domain.usecase.CompleteTaskResult
import com.fourctech.todaylist.domain.usecase.CompleteTaskUseCase
import com.fourctech.todaylist.domain.usecase.UndoCompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val sessionCompletedCount: Int = 0,
    val adsRemoved: Boolean = false,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val analytics: Analytics,
    settingsRepository: com.fourctech.todaylist.domain.repository.SettingsRepository,
) : ViewModel() {

    private val sessionCompleted = MutableStateFlow(0)
    private val _undoEvents = MutableSharedFlow<CompleteTaskResult>(extraBufferCapacity = 1)
    val undoEvents: SharedFlow<CompleteTaskResult> = _undoEvents.asSharedFlow()

    val uiState: StateFlow<TodayUiState> = combine(
        taskRepository.observeTodayTasks(),
        sessionCompleted,
        settingsRepository.observeSettings(),
    ) { tasks, completed, settings ->
        TodayUiState(
            tasks = tasks,
            sessionCompletedCount = completed,
            adsRemoved = settings.adsRemovedCached,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val result = completeTaskUseCase(taskId) ?: return@launch
            sessionCompleted.value = sessionCompleted.value + 1
            _undoEvents.emit(result)
        }
    }

    fun undoComplete(result: CompleteTaskResult) {
        viewModelScope.launch {
            undoCompleteTaskUseCase(result.completionEventId, result.previousLocation)
            sessionCompleted.value = (sessionCompleted.value - 1).coerceAtLeast(0)
        }
    }

    fun moveToLater(taskId: String) {
        viewModelScope.launch {
            taskRepository.moveToLater(taskId)
            analytics.log(
                AnalyticsEvents.TASK_MOVED,
                mapOf(AnalyticsParams.TO_LOCATION to AnalyticsParams.LOCATION_LATER),
            )
        }
    }

    fun moveUp(taskId: String) {
        viewModelScope.launch {
            val tasks = uiState.value.tasks
            val index = tasks.indexOfFirst { it.id == taskId }
            if (index <= 0) return@launch
            val reordered = tasks.toMutableList().also {
                val item = it.removeAt(index)
                it.add(index - 1, item)
            }
            taskRepository.reorderTasks(TaskLocation.TODAY, reordered.map { it.id })
        }
    }

    fun moveDown(taskId: String) {
        viewModelScope.launch {
            val tasks = uiState.value.tasks
            val index = tasks.indexOfFirst { it.id == taskId }
            if (index < 0 || index >= tasks.lastIndex) return@launch
            val reordered = tasks.toMutableList().also {
                val item = it.removeAt(index)
                it.add(index + 1, item)
            }
            taskRepository.reorderTasks(TaskLocation.TODAY, reordered.map { it.id })
        }
    }
}
