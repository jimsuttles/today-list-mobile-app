package com.fourctech.todaylist.domain.rollover

import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunDailyRolloverUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: ClockProvider,
) {
    suspend fun markComplete(today: LocalDate = clock.today()) {
        settingsRepository.setLastRolloverDate(today)
    }

    suspend fun keepAllOnToday(taskIds: List<String>, today: LocalDate = clock.today()) {
        taskRepository.keepOnTodayForNewDay(taskIds)
        markComplete(today)
    }

    suspend fun moveAllToLater(taskIds: List<String>, today: LocalDate = clock.today()) {
        taskIds.forEach { taskRepository.moveToLater(it) }
        markComplete(today)
    }

    /** Applies per-task decisions from the ASK review, then marks rollover complete. */
    suspend fun applyDecisions(
        decisions: Map<String, TaskLocation>,
        today: LocalDate = clock.today(),
    ) {
        val keepIds = decisions.filterValues { it == TaskLocation.TODAY }.keys.toList()
        val laterIds = decisions.filterValues { it == TaskLocation.LATER }.keys.toList()
        taskRepository.keepOnTodayForNewDay(keepIds)
        laterIds.forEach { taskRepository.moveToLater(it) }
        markComplete(today)
    }
}
