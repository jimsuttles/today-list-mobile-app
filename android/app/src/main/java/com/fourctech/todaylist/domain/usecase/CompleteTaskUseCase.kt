package com.fourctech.todaylist.domain.usecase

import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.analytics.AnalyticsParams
import com.fourctech.todaylist.core.analytics.toAnalyticsValue
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
    private val analytics: Analytics,
) {
    suspend operator fun invoke(
        taskId: String,
        source: String = AnalyticsParams.SOURCE_LIST,
    ): CompleteTaskResult? {
        val task = taskRepository.getTask(taskId) ?: return null
        val eventId = taskRepository.completeTask(taskId) ?: return null
        notificationScheduler.cancelReminder(taskId)
        analytics.log(
            AnalyticsEvents.TASK_COMPLETED,
            mapOf(
                AnalyticsParams.LOCATION to task.location.toAnalyticsValue(),
                AnalyticsParams.SOURCE to source,
            ),
        )
        return CompleteTaskResult(
            completionEventId = eventId,
            title = task.title,
            previousLocation = task.location,
        )
    }
}
