package com.fourctech.todaylist.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val sessionCompletedCount: Int = 0,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val sessionCompleted = MutableStateFlow(0)

    val uiState: StateFlow<TodayUiState> = combine(
        taskRepository.observeTodayTasks(),
        sessionCompleted,
    ) { tasks, completed ->
        TodayUiState(tasks = tasks, sessionCompletedCount = completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
            sessionCompleted.value = sessionCompleted.value + 1
        }
    }

    fun moveToLater(taskId: String) {
        viewModelScope.launch { taskRepository.moveToLater(taskId) }
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
