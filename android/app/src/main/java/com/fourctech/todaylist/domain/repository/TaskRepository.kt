package com.fourctech.todaylist.domain.repository

import com.fourctech.todaylist.domain.model.DeleteScope
import com.fourctech.todaylist.domain.model.RecurrenceRule
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTodayTasks(): Flow<List<Task>>
    fun observeLaterTasks(): Flow<List<Task>>
    fun observeTask(taskId: String): Flow<Task?>

    suspend fun getTask(taskId: String): Task?

    suspend fun getTodayTasks(): List<Task>

    suspend fun createTask(
        title: String,
        notes: String? = null,
        location: TaskLocation = TaskLocation.TODAY,
        reminderAt: java.time.Instant? = null,
        scheduledDate: java.time.LocalDate? = null,
        recurrence: RecurrenceRule? = null,
    ): Task

    suspend fun updateTask(task: Task)

    suspend fun moveToToday(taskId: String)

    suspend fun moveToLater(taskId: String)

    /** Completes the task; returns the new completion event id, or null if missing. */
    suspend fun completeTask(taskId: String): String?

    suspend fun uncompleteTask(
        completionEventId: String,
        restoreTo: TaskLocation = TaskLocation.TODAY,
    )

    suspend fun deleteTask(
        taskId: String,
        scope: DeleteScope = DeleteScope.THIS_TASK,
    )

    /** Keeps tasks on Today and refreshes [scheduledDate] for the new day. */
    suspend fun keepOnTodayForNewDay(taskIds: List<String>)

    /** Reorders tasks within [location]; [orderedTaskIds] is front-to-back. */
    suspend fun reorderTasks(location: TaskLocation, orderedTaskIds: List<String>)
}
