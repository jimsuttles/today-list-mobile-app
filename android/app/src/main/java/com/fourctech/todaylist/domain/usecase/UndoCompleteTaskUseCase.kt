package com.fourctech.todaylist.domain.usecase

import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.analytics.AnalyticsParams
import com.fourctech.todaylist.core.analytics.toAnalyticsValue
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import javax.inject.Inject

class UndoCompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val analytics: Analytics,
) {
    suspend operator fun invoke(
        completionEventId: String,
        restoreTo: TaskLocation,
    ) {
        taskRepository.uncompleteTask(completionEventId, restoreTo)
        analytics.log(
            AnalyticsEvents.TASK_UNDONE,
            mapOf(AnalyticsParams.LOCATION to restoreTo.toAnalyticsValue()),
        )
    }
}
