package com.fourctech.todaylist.domain.usecase

import com.fourctech.todaylist.core.notifications.NotificationScheduler
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import javax.inject.Inject

data class CompleteTaskResult(
    val completionEventId: String,
    val title: String,
    val previousLocation: TaskLocation,
)

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val notificationScheduler: NotificationScheduler,
) {
    suspend operator fun invoke(taskId: String): CompleteTaskResult? {
        val task = taskRepository.getTask(taskId) ?: return null
        val eventId = taskRepository.completeTask(taskId) ?: return null
        notificationScheduler.cancelReminder(taskId)
        return CompleteTaskResult(
            completionEventId = eventId,
            title = task.title,
            previousLocation = task.location,
        )
    }
}
