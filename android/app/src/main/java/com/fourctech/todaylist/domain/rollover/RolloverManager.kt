package com.fourctech.todaylist.domain.rollover

import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.domain.repository.TaskRepository
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates day-boundary rollover on launch/foreground (not a midnight alarm).
 * Multi-day gaps produce one consolidated review, not per-day prompts.
 */
@Singleton
class RolloverManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val clock: ClockProvider,
    private val runDailyRollover: RunDailyRolloverUseCase,
) {
    suspend fun evaluate(): RolloverEvaluation {
        val today = clock.today()
        val settings = settingsRepository.getSettings()
        val last = settings.lastRolloverDate
        if (last != null && !last.isBefore(today)) {
            return RolloverEvaluation.UpToDate(today)
        }

        val unfinished = taskRepository.getTodayTasks()
        val missedDays = if (last == null) {
            1L
        } else {
            ChronoUnit.DAYS.between(last, today).coerceAtLeast(1L)
        }

        return when (settings.rolloverMode) {
            RolloverMode.ASK -> {
                if (unfinished.isEmpty()) {
                    runDailyRollover.markComplete(today)
                    RolloverEvaluation.AppliedSilently(today = today)
                } else {
                    RolloverEvaluation.NeedsReview(
                        today = today,
                        unfinished = unfinished,
                        missedDays = missedDays,
                        lastRolloverDate = last,
                    )
                }
            }
            RolloverMode.AUTO_TODAY -> {
                runDailyRollover.keepAllOnToday(unfinished.map { it.id }, today)
                RolloverEvaluation.AppliedSilently(
                    today = today,
                    keptOnTodayCount = unfinished.size,
                )
            }
            RolloverMode.AUTO_LATER -> {
                unfinished.forEach { taskRepository.moveToLater(it.id) }
                runDailyRollover.markComplete(today)
                RolloverEvaluation.AppliedSilently(
                    today = today,
                    movedToLaterCount = unfinished.size,
                )
            }
        }
    }
}
