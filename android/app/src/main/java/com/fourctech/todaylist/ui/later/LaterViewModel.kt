package com.fourctech.todaylist.ui.later

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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LaterUiState(
    val tasks: List<Task> = emptyList(),
)

@HiltViewModel
class LaterViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val analytics: Analytics,
) : ViewModel() {

    private val _undoEvents = MutableSharedFlow<CompleteTaskResult>(extraBufferCapacity = 1)
    val undoEvents: SharedFlow<CompleteTaskResult> = _undoEvents.asSharedFlow()

    val uiState: StateFlow<LaterUiState> = taskRepository.observeLaterTasks()
        .map { LaterUiState(tasks = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LaterUiState())

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val result = completeTaskUseCase(taskId) ?: return@launch
            _undoEvents.emit(result)
        }
    }

    fun undoComplete(result: CompleteTaskResult) {
        viewModelScope.launch {
            undoCompleteTaskUseCase(result.completionEventId, result.previousLocation)
        }
    }

    fun moveToToday(taskId: String) {
        viewModelScope.launch {
            taskRepository.moveToToday(taskId)
            analytics.log(
                AnalyticsEvents.TASK_MOVED,
                mapOf(AnalyticsParams.TO_LOCATION to AnalyticsParams.LOCATION_TODAY),
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
            taskRepository.reorderTasks(TaskLocation.LATER, reordered.map { it.id })
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
            taskRepository.reorderTasks(TaskLocation.LATER, reordered.map { it.id })
        }
    }
}
