package com.fourctech.todaylist.ui.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.notifications.NotificationScheduler
import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.domain.model.DeleteScope
import com.fourctech.todaylist.domain.model.RepeatOption
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.model.toRecurrenceRule
import com.fourctech.todaylist.domain.model.toRepeatOption
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.domain.repository.TaskRepository
import com.fourctech.todaylist.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val title: String = "",
    val notes: String = "",
    val location: TaskLocation = TaskLocation.TODAY,
    val reminderEnabled: Boolean = false,
    val reminderAt: Instant? = null,
    val repeat: RepeatOption = RepeatOption.NONE,
    val isRecurring: Boolean = false,
    val saving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val needsNotificationPermission: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: NotificationScheduler,
    private val clock: ClockProvider,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle[Route.TaskDetail.arg])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TaskDetailEvent> = _events.asSharedFlow()

    private var baseline: Task? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            if (task == null) {
                _uiState.value = TaskDetailUiState(loading = false, missing = true)
                return@launch
            }
            baseline = task
            _uiState.value = task.toUiState()
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
        scheduleAutoSave()
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
        scheduleAutoSave()
    }

    fun onLocationChange(location: TaskLocation) {
        _uiState.update { it.copy(location = location) }
        scheduleAutoSave(immediate = true)
    }

    fun onRepeatChange(repeat: RepeatOption) {
        _uiState.update { it.copy(repeat = repeat, isRecurring = repeat != RepeatOption.NONE) }
        scheduleAutoSave(immediate = true)
    }

    fun onReminderEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val settings = settingsRepository.getSettings()
                val alreadyPrompted = settings.notificationPermissionPrompted
                val default = defaultReminderInstant()
                _uiState.update {
                    it.copy(
                        reminderEnabled = true,
                        reminderAt = it.reminderAt ?: default,
                        needsNotificationPermission = !alreadyPrompted,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        reminderEnabled = false,
                        reminderAt = null,
                        needsNotificationPermission = false,
                    )
                }
            }
            scheduleAutoSave(immediate = true)
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationPermissionPrompted(true)
            _uiState.update { it.copy(needsNotificationPermission = false) }
            if (!granted) {
                _events.emit(TaskDetailEvent.NotificationPermissionDenied)
            }
        }
    }

    fun onReminderDateSelected(date: LocalDate) {
        val current = _uiState.value.reminderAt ?: defaultReminderInstant()
        val zone = clock.zoneId()
        val time = LocalDateTime.ofInstant(current, zone).toLocalTime()
        val updated = LocalDateTime.of(date, time).atZone(zone).toInstant()
        _uiState.update { it.copy(reminderEnabled = true, reminderAt = updated) }
        scheduleAutoSave(immediate = true)
    }

    fun onReminderTimeSelected(hour: Int, minute: Int) {
        val current = _uiState.value.reminderAt ?: defaultReminderInstant()
        val zone = clock.zoneId()
        val date = LocalDateTime.ofInstant(current, zone).toLocalDate()
        val updated = LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant()
        _uiState.update { it.copy(reminderEnabled = true, reminderAt = updated) }
        scheduleAutoSave(immediate = true)
    }

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDelete(scope: DeleteScope) {
        viewModelScope.launch {
            saveJob?.cancel()
            flushSave()
            notificationScheduler.cancelReminder(taskId)
            taskRepository.deleteTask(taskId, scope)
            _events.emit(TaskDetailEvent.Deleted)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            saveJob?.cancel()
            flushSave()
            _events.emit(TaskDetailEvent.NavigateBack)
        }
    }

    private fun scheduleAutoSave(immediate: Boolean = false) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            if (!immediate) delay(450)
            flushSave()
        }
    }

    private suspend fun flushSave() {
        val current = _uiState.value
        val original = baseline ?: return
        if (current.missing || current.loading) return
        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isEmpty()) return

        val startDate = original.scheduledDate ?: clock.today()
        val recurrence = current.repeat.toRecurrenceRule(
            existingId = original.recurrence?.id,
            startDate = startDate,
        )
        val draft = original.copy(
            title = trimmedTitle,
            notes = current.notes.trim().ifEmpty { null },
            location = current.location,
            reminderAt = if (current.reminderEnabled) current.reminderAt else null,
            recurrence = recurrence,
        )
        val meaningfulChange =
            draft.title != original.title ||
                draft.notes != original.notes ||
                draft.location != original.location ||
                draft.reminderAt != original.reminderAt ||
                draft.recurrence != original.recurrence
        if (!meaningfulChange) return

        _uiState.update { it.copy(saving = true) }
        taskRepository.updateTask(draft)
        syncReminderAlarm(draft)
        baseline = taskRepository.getTask(taskId) ?: draft
        _uiState.update { state ->
            baseline?.toUiState()?.copy(
                saving = false,
                showDeleteDialog = state.showDeleteDialog,
                needsNotificationPermission = state.needsNotificationPermission,
            ) ?: state.copy(saving = false)
        }
    }

    private suspend fun syncReminderAlarm(task: Task) {
        val at = task.reminderAt
        if (at == null) {
            notificationScheduler.cancelReminder(task.id)
        } else {
            notificationScheduler.scheduleReminder(task.id, task.title, at)
        }
    }

    private fun defaultReminderInstant(): Instant {
        // Near-term default so reminders are easy to verify; users can edit date/time.
        return clock.now().plusSeconds(120)
    }

    private fun Task.toUiState(): TaskDetailUiState =
        TaskDetailUiState(
            loading = false,
            missing = false,
            title = title,
            notes = notes.orEmpty(),
            location = location,
            reminderEnabled = reminderAt != null,
            reminderAt = reminderAt,
            repeat = recurrence.toRepeatOption(),
            isRecurring = recurrence != null,
        )
}

sealed class TaskDetailEvent {
    data object NavigateBack : TaskDetailEvent()
    data object Deleted : TaskDetailEvent()
    data object NotificationPermissionDenied : TaskDetailEvent()
}
