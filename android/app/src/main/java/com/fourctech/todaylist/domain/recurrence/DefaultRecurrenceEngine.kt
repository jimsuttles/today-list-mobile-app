package com.fourctech.todaylist.domain.recurrence

import com.fourctech.todaylist.domain.model.RecurrenceRule
import com.fourctech.todaylist.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class DefaultRecurrenceEngine @Inject constructor() : RecurrenceEngine {

    override fun nextOccurrence(rule: RecurrenceRule, after: LocalDate): LocalDate? {
        val interval = rule.interval.coerceAtLeast(1)
        val start = rule.startDate
        val end = rule.endDate

        // Series not started yet relative to cursor: first valid on/after start, still > after.
        var candidate = maxOf(after.plusDays(1), start)
        // Hard cap to avoid runaway loops on bad rules.
        val limit = candidate.plusYears(5)

        while (!candidate.isAfter(limit)) {
            if (end != null && candidate.isAfter(end)) return null
            if (matches(rule, candidate, interval, start)) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun matches(
        rule: RecurrenceRule,
        date: LocalDate,
        interval: Int,
        start: LocalDate,
    ): Boolean {
        if (date.isBefore(start)) return false
        return when (rule.type) {
            RecurrenceType.DAILY,
            RecurrenceType.CUSTOM_DAYS,
            -> {
                val days = ChronoUnit.DAYS.between(start, date)
                days >= 0 && days % interval == 0L
            }

            RecurrenceType.WEEKDAYS -> {
                date.dayOfWeek in WEEKDAYS && weeksAligned(start, date, interval = 1)
            }

            RecurrenceType.WEEKLY,
            RecurrenceType.CUSTOM_WEEKS,
            -> {
                val days = rule.weekdays.ifEmpty { setOf(start.dayOfWeek) }
                date.dayOfWeek in days && weeksAligned(start, date, interval)
            }

            RecurrenceType.MONTHLY,
            RecurrenceType.CUSTOM_MONTHS,
            -> {
                val targetDay = (rule.dayOfMonth ?: start.dayOfMonth).coerceIn(1, 31)
                date.dayOfMonth == clampedDayOfMonth(date.year, date.monthValue, targetDay) &&
                    monthsAligned(start, date, interval, targetDay)
            }
        }
    }

    private fun weeksAligned(start: LocalDate, date: LocalDate, interval: Int): Boolean {
        val startWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeks = ChronoUnit.WEEKS.between(startWeek, dateWeek)
        return weeks >= 0 && weeks % interval == 0L
    }

    private fun monthsAligned(
        start: LocalDate,
        date: LocalDate,
        interval: Int,
        targetDay: Int,
    ): Boolean {
        val startMonthIndex = start.year * 12L + (start.monthValue - 1)
        val dateMonthIndex = date.year * 12L + (date.monthValue - 1)
        val months = dateMonthIndex - startMonthIndex
        if (months < 0 || months % interval != 0L) return false
        // Anchor month uses the clamped day for that month.
        return date.dayOfMonth == clampedDayOfMonth(date.year, date.monthValue, targetDay)
    }

    companion object {
        private val WEEKDAYS = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )

        fun clampedDayOfMonth(year: Int, month: Int, dayOfMonth: Int): Int {
            val length = LocalDate.of(year, month, 1).lengthOfMonth()
            return min(dayOfMonth.coerceAtLeast(1), length)
        }
    }
}
