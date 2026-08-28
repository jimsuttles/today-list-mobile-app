package com.fourctech.todaylist.domain.repository

import com.fourctech.todaylist.domain.model.RecurrenceRule
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTodayTasks(): Flow<List<Task>>
    fun observeLaterTasks(): Flow<List<Task>>
    fun observeTask(taskId: String): Flow<Task?>

    suspend fun getTask(taskId: String): Task?

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

    suspend fun completeTask(taskId: String)

    suspend fun uncompleteTask(completionEventId: String)

    suspend fun deleteTask(taskId: String)

    /** Reorders tasks within [location]; [orderedTaskIds] is front-to-back. */
    suspend fun reorderTasks(location: TaskLocation, orderedTaskIds: List<String>)
}
