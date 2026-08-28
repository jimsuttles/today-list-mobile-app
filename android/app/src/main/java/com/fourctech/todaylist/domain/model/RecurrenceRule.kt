package com.fourctech.todaylist.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class RecurrenceRule(
    val id: String,
    val type: RecurrenceType,
    val interval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val weekdays: Set<DayOfWeek>,
    val dayOfMonth: Int?,
)
