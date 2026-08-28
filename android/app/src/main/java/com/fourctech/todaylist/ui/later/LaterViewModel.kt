package com.fourctech.todaylist.ui.later

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LaterUiState(
    val tasks: List<Task> = emptyList(),
)

@HiltViewModel
class LaterViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<LaterUiState> = taskRepository.observeLaterTasks()
        .map { LaterUiState(tasks = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LaterUiState())

    fun completeTask(taskId: String) {
        viewModelScope.launch { taskRepository.completeTask(taskId) }
    }

    fun moveToToday(taskId: String) {
        viewModelScope.launch { taskRepository.moveToToday(taskId) }
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
