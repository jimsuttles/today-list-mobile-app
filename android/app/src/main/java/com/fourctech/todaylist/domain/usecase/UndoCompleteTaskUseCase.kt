package com.fourctech.todaylist.domain.usecase

import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.TaskRepository
import javax.inject.Inject

class UndoCompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        completionEventId: String,
        restoreTo: TaskLocation,
    ) {
        taskRepository.uncompleteTask(completionEventId, restoreTo)
    }
}
