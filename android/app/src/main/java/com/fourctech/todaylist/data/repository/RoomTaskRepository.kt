package com.fourctech.todaylist.data.repository

import androidx.room.withTransaction
import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.data.local.dao.CompletionEventDao
import com.fourctech.todaylist.data.local.dao.OccurrenceDao
import com.fourctech.todaylist.data.local.dao.RecurrenceDao
import com.fourctech.todaylist.data.local.dao.TaskDao
import com.fourctech.todaylist.data.local.db.TodayListDatabase
import com.fourctech.todaylist.data.local.entity.CompletionEventEntity
import com.fourctech.todaylist.data.local.entity.TaskEntity
import com.fourctech.todaylist.data.local.entity.TaskOccurrenceEntity
import com.fourctech.todaylist.data.local.entity.TaskStatus
import com.fourctech.todaylist.domain.model.RecurrenceRule
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@Singleton
class RoomTaskRepository @Inject constructor(
    private val database: TodayListDatabase,
    private val taskDao: TaskDao,
    private val recurrenceDao: RecurrenceDao,
    private val occurrenceDao: OccurrenceDao,
    private val completionEventDao: CompletionEventDao,
    private val clock: ClockProvider,
) : TaskRepository {

    override fun observeTodayTasks(): Flow<List<Task>> =
        taskDao.observeTodayTasks().mapLatest { entities -> entities.mapNotNull { it.toDomainTask() } }

    override fun observeLaterTasks(): Flow<List<Task>> =
        taskDao.observeLaterTasks().mapLatest { entities -> entities.mapNotNull { it.toDomainTask() } }

    override fun observeTask(taskId: String): Flow<Task?> =
        taskDao.observeTaskById(taskId).mapLatest { entity -> entity?.toDomainTask() }

    override suspend fun getTask(taskId: String): Task? =
        taskDao.getTaskById(taskId)?.toDomainTask()

    override suspend fun createTask(
        title: String,
        notes: String?,
        location: TaskLocation,
        reminderAt: java.time.Instant?,
        scheduledDate: java.time.LocalDate?,
        recurrence: RecurrenceRule?,
    ): Task {
        val now = clock.now()
        val id = UUID.randomUUID().toString()
        val sortOrder = taskDao.maxSortOrder(location.toStatus()) + 1
        val rule = recurrence?.let { r ->
            r.copy(id = r.id.ifBlank { UUID.randomUUID().toString() })
        }

        return database.withTransaction {
            if (rule != null) {
                recurrenceDao.insert(rule.toEntity())
            }
            val entity = com.fourctech.todaylist.data.local.entity.TaskEntity(
                id = id,
                title = title.trim(),
                notes = notes,
                status = location.toStatus(),
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now,
                scheduledDate = scheduledDate ?: if (location == TaskLocation.TODAY) clock.today() else null,
                reminderAt = reminderAt,
                recurrenceId = rule?.id,
            )
            taskDao.insertTask(entity)
            occurrenceDao.insert(
                TaskOccurrenceEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = id,
                    occurrenceDate = entity.scheduledDate ?: clock.today(),
                    completedAt = null,
                    movedToLater = location == TaskLocation.LATER,
                    createdAt = now,
                ),
            )
            entity.toDomain(rule)!!
        }
    }

    override suspend fun updateTask(task: Task) {
        database.withTransaction {
            val existing = taskDao.getTaskById(task.id) ?: return@withTransaction
            val updated = task.copy(updatedAt = clock.now())
            val rule = updated.recurrence
            if (rule != null) {
                recurrenceDao.insert(rule.toEntity())
            } else if (existing.recurrenceId != null) {
                recurrenceDao.delete(existing.recurrenceId)
            }
            val status = if (existing.status == TaskStatus.DELETED) {
                updated.location.toStatus()
            } else {
                existing.status
            }
            taskDao.updateTask(updated.toEntity(status))
        }
    }

    override suspend fun moveToToday(taskId: String) {
        val now = clock.now()
        database.withTransaction {
            val task = taskDao.getTaskById(taskId) ?: return@withTransaction
            val sortOrder = taskDao.maxSortOrder(TaskStatus.TODAY) + 1
            taskDao.updateTask(
                task.copy(
                    status = TaskStatus.TODAY,
                    sortOrder = sortOrder,
                    updatedAt = now,
                    scheduledDate = clock.today(),
                ),
            )
        }
    }

    override suspend fun moveToLater(taskId: String) {
        val now = clock.now()
        database.withTransaction {
            val task = taskDao.getTaskById(taskId) ?: return@withTransaction
            val sortOrder = taskDao.maxSortOrder(TaskStatus.LATER) + 1
            taskDao.updateTask(
                task.copy(
                    status = TaskStatus.LATER,
                    sortOrder = sortOrder,
                    updatedAt = now,
                ),
            )
            occurrenceDao.getOpenOccurrence(taskId)?.let { open ->
                occurrenceDao.update(open.copy(movedToLater = true))
            }
        }
    }

    override suspend fun completeTask(taskId: String): String? {
        val now = clock.now()
        val today = clock.today()
        return database.withTransaction {
            val task = taskDao.getTaskById(taskId) ?: return@withTransaction null
            if (task.status == TaskStatus.DELETED) return@withTransaction null

            val open = occurrenceDao.getOpenOccurrence(taskId)
            val occurrenceId = if (open != null) {
                occurrenceDao.update(open.copy(completedAt = now))
                open.id
            } else {
                val newId = UUID.randomUUID().toString()
                occurrenceDao.insert(
                    TaskOccurrenceEntity(
                        id = newId,
                        taskId = taskId,
                        occurrenceDate = task.scheduledDate ?: today,
                        completedAt = now,
                        movedToLater = task.status == TaskStatus.LATER,
                        createdAt = now,
                    ),
                )
                newId
            }

            val eventId = UUID.randomUUID().toString()
            completionEventDao.insert(
                CompletionEventEntity(
                    id = eventId,
                    taskId = taskId,
                    occurrenceId = occurrenceId,
                    titleSnapshot = task.title,
                    completedAt = now,
                    completionDate = today,
                ),
            )

            // Non-recurring: soft-delete from active lists. Recurring stays for next occurrence (engine later).
            if (task.recurrenceId == null) {
                taskDao.updateTask(
                    task.copy(status = TaskStatus.DELETED, updatedAt = now),
                )
            }
            eventId
        }
    }

    override suspend fun uncompleteTask(
        completionEventId: String,
        restoreTo: TaskLocation,
    ) {
        val now = clock.now()
        val status = restoreTo.toStatus()
        database.withTransaction {
            val event = completionEventDao.getById(completionEventId) ?: return@withTransaction
            val taskId = event.taskId
            if (taskId != null) {
                val task = taskDao.getTaskById(taskId)
                if (task != null) {
                    val sortOrder = taskDao.maxSortOrder(status) + 1
                    taskDao.updateTask(
                        task.copy(
                            status = status,
                            sortOrder = sortOrder,
                            updatedAt = now,
                            scheduledDate = if (restoreTo == TaskLocation.TODAY) clock.today() else task.scheduledDate,
                        ),
                    )
                }
                event.occurrenceId?.let { occurrenceId ->
                    occurrenceDao.getById(occurrenceId)?.let { occurrence ->
                        occurrenceDao.update(
                            occurrence.copy(
                                completedAt = null,
                                movedToLater = restoreTo == TaskLocation.LATER,
                            ),
                        )
                    }
                }
            }
            completionEventDao.delete(completionEventId)
        }
    }

    override suspend fun deleteTask(taskId: String) {
        database.withTransaction {
            val task = taskDao.getTaskById(taskId) ?: return@withTransaction
            val recurrenceId = task.recurrenceId
            taskDao.deleteTask(taskId)
            if (recurrenceId != null) {
                recurrenceDao.delete(recurrenceId)
            }
        }
    }

    override suspend fun reorderTasks(location: TaskLocation, orderedTaskIds: List<String>) {
        val now = clock.now()
        val status = location.toStatus()
        database.withTransaction {
            orderedTaskIds.forEachIndexed { index, id ->
                val task = taskDao.getTaskById(id) ?: return@forEachIndexed
                if (task.status != status) return@forEachIndexed
                taskDao.updateSortOrder(id, index, now)
            }
        }
    }

    private suspend fun TaskEntity.toDomainTask(): Task? {
        val rule = recurrenceId?.let { recurrenceDao.getById(it)?.toDomain() }
        return toDomain(rule)
    }
}
