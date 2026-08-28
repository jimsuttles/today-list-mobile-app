package com.fourctech.todaylist.domain.recurrence

import com.fourctech.todaylist.domain.model.RecurrenceRule
import java.time.LocalDate

interface RecurrenceEngine {
    /**
     * Returns the next occurrence strictly after [after], or null if the series has ended.
     */
    fun nextOccurrence(rule: RecurrenceRule, after: LocalDate): LocalDate?
}
