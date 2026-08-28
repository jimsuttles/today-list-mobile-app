package com.fourctech.todaylist.domain.rollover

import com.fourctech.todaylist.domain.model.Task
import java.time.LocalDate

sealed class RolloverEvaluation {
    /** Already evaluated for [today]; nothing to do. */
    data class UpToDate(val today: LocalDate) : RolloverEvaluation()

    /** Auto mode applied (or ASK with no unfinished tasks). */
    data class AppliedSilently(
        val today: LocalDate,
        val movedToLaterCount: Int = 0,
        val keptOnTodayCount: Int = 0,
    ) : RolloverEvaluation()

    /**
     * ASK mode with unfinished Today tasks.
     * [missedDays] is days since last rollover (1 = yesterday only; >1 = multi-day gap).
     */
    data class NeedsReview(
        val today: LocalDate,
        val unfinished: List<Task>,
        val missedDays: Long,
        val lastRolloverDate: LocalDate?,
    ) : RolloverEvaluation()
}
