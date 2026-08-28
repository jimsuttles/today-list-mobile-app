package com.fourctech.todaylist.domain.recurrence

import com.fourctech.todaylist.domain.model.RecurrenceRule
import com.fourctech.todaylist.domain.model.RecurrenceType
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

class DefaultRecurrenceEngineTest {
    private val engine = DefaultRecurrenceEngine()

    @Test
    fun daily_nextDay() {
        val rule = rule(RecurrenceType.DAILY, start = date(2026, 8, 27))
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 27)))
            .isEqualTo(date(2026, 8, 28))
    }

    @Test
    fun customDays_everyThreeDays() {
        val rule = rule(RecurrenceType.CUSTOM_DAYS, start = date(2026, 8, 27), interval = 3)
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 27)))
            .isEqualTo(date(2026, 8, 30))
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 30)))
            .isEqualTo(date(2026, 9, 2))
    }

    @Test
    fun weekdays_skipsWeekend() {
        val rule = rule(RecurrenceType.WEEKDAYS, start = date(2026, 8, 27)) // Thursday
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 28))) // after Friday
            .isEqualTo(date(2026, 8, 31)) // Monday
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 27)))
            .isEqualTo(date(2026, 8, 28)) // Friday
    }

    @Test
    fun weekly_sameWeekday() {
        val rule = rule(
            RecurrenceType.WEEKLY,
            start = date(2026, 8, 24), // Monday
            weekdays = setOf(DayOfWeek.MONDAY),
        )
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 24)))
            .isEqualTo(date(2026, 8, 31))
    }

    @Test
    fun weekly_multipleWeekdays() {
        val rule = rule(
            RecurrenceType.WEEKLY,
            start = date(2026, 8, 24),
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 24)))
            .isEqualTo(date(2026, 8, 26))
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 26)))
            .isEqualTo(date(2026, 8, 31))
    }

    @Test
    fun customWeeks_everyTwoWeeks() {
        val rule = rule(
            RecurrenceType.CUSTOM_WEEKS,
            start = date(2026, 8, 24), // Monday
            interval = 2,
            weekdays = setOf(DayOfWeek.MONDAY),
        )
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 24)))
            .isEqualTo(date(2026, 9, 7))
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 31))).isEqualTo(date(2026, 9, 7))
    }

    @Test
    fun monthly_clampsShortMonths() {
        val rule = rule(
            RecurrenceType.MONTHLY,
            start = date(2026, 1, 31),
            dayOfMonth = 31,
        )
        assertThat(engine.nextOccurrence(rule, date(2026, 1, 31)))
            .isEqualTo(date(2026, 2, 28))
        assertThat(engine.nextOccurrence(rule, date(2026, 2, 28)))
            .isEqualTo(date(2026, 3, 31))
        assertThat(engine.nextOccurrence(rule, date(2026, 3, 31)))
            .isEqualTo(date(2026, 4, 30))
    }

    @Test
    fun monthly_leapYearFebruary() {
        val rule = rule(
            RecurrenceType.MONTHLY,
            start = date(2024, 1, 31),
            dayOfMonth = 31,
        )
        assertThat(engine.nextOccurrence(rule, date(2024, 1, 31)))
            .isEqualTo(date(2024, 2, 29))
    }

    @Test
    fun customMonths_everyTwoMonths() {
        val rule = rule(
            RecurrenceType.CUSTOM_MONTHS,
            start = date(2026, 1, 15),
            interval = 2,
            dayOfMonth = 15,
        )
        assertThat(engine.nextOccurrence(rule, date(2026, 1, 15)))
            .isEqualTo(date(2026, 3, 15))
        assertThat(engine.nextOccurrence(rule, date(2026, 2, 15)))
            .isEqualTo(date(2026, 3, 15))
    }

    @Test
    fun endDate_stopsSeries() {
        val rule = rule(
            RecurrenceType.DAILY,
            start = date(2026, 8, 27),
            endDate = date(2026, 8, 28),
        )
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 27)))
            .isEqualTo(date(2026, 8, 28))
        assertThat(engine.nextOccurrence(rule, date(2026, 8, 28))).isNull()
    }

    @Test
    fun clampedDayOfMonth_helper() {
        assertThat(DefaultRecurrenceEngine.clampedDayOfMonth(2026, 2, 31)).isEqualTo(28)
        assertThat(DefaultRecurrenceEngine.clampedDayOfMonth(2024, 2, 31)).isEqualTo(29)
        assertThat(DefaultRecurrenceEngine.clampedDayOfMonth(2026, 4, 31)).isEqualTo(30)
        assertThat(DefaultRecurrenceEngine.clampedDayOfMonth(2026, 1, 15)).isEqualTo(15)
    }

    private fun date(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    private fun rule(
        type: RecurrenceType,
        start: LocalDate,
        interval: Int = 1,
        endDate: LocalDate? = null,
        weekdays: Set<DayOfWeek> = emptySet(),
        dayOfMonth: Int? = null,
    ) = RecurrenceRule(
        id = "rule",
        type = type,
        interval = interval,
        startDate = start,
        endDate = endDate,
        weekdays = weekdays,
        dayOfMonth = dayOfMonth,
    )
}
