package com.fourctech.todaylist.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class RepeatOption {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY,
}

fun RecurrenceRule?.toRepeatOption(): RepeatOption =
    when (this?.type) {
        null -> RepeatOption.NONE
        RecurrenceType.DAILY -> RepeatOption.DAILY
        RecurrenceType.WEEKDAYS -> RepeatOption.WEEKDAYS
        RecurrenceType.WEEKLY -> RepeatOption.WEEKLY
        RecurrenceType.MONTHLY -> RepeatOption.MONTHLY
        RecurrenceType.CUSTOM_DAYS,
        RecurrenceType.CUSTOM_WEEKS,
        RecurrenceType.CUSTOM_MONTHS,
        -> RepeatOption.DAILY
    }

fun RepeatOption.toRecurrenceRule(
    existingId: String?,
    startDate: LocalDate,
): RecurrenceRule? =
    when (this) {
        RepeatOption.NONE -> null
        RepeatOption.DAILY -> RecurrenceRule(
            id = existingId ?: UUID.randomUUID().toString(),
            type = RecurrenceType.DAILY,
            interval = 1,
            startDate = startDate,
            endDate = null,
            weekdays = emptySet(),
            dayOfMonth = null,
        )
        RepeatOption.WEEKDAYS -> RecurrenceRule(
            id = existingId ?: UUID.randomUUID().toString(),
            type = RecurrenceType.WEEKDAYS,
            interval = 1,
            startDate = startDate,
            endDate = null,
            weekdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            dayOfMonth = null,
        )
        RepeatOption.WEEKLY -> RecurrenceRule(
            id = existingId ?: UUID.randomUUID().toString(),
            type = RecurrenceType.WEEKLY,
            interval = 1,
            startDate = startDate,
            endDate = null,
            weekdays = setOf(startDate.dayOfWeek),
            dayOfMonth = null,
        )
        RepeatOption.MONTHLY -> RecurrenceRule(
            id = existingId ?: UUID.randomUUID().toString(),
            type = RecurrenceType.MONTHLY,
            interval = 1,
            startDate = startDate,
            endDate = null,
            weekdays = emptySet(),
            dayOfMonth = startDate.dayOfMonth,
        )
    }
