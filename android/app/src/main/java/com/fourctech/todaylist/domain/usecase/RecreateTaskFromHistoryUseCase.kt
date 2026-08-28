package com.fourctech.todaylist.domain.usecase

import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.HistoryRepository
import com.fourctech.todaylist.domain.repository.TaskRepository
import javax.inject.Inject

/** Creates a fresh active task from a history title snapshot; leaves the completion record. */
class RecreateTaskFromHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        completionEventId: String,
        location: TaskLocation,
    ): Task? {
        val record = historyRepository.getCompletion(completionEventId) ?: return null
        return taskRepository.createTask(title = record.title, location = location)
    }
}
