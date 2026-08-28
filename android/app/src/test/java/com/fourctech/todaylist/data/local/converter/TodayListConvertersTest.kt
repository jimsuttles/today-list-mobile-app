package com.fourctech.todaylist.data.local.converter

import com.fourctech.todaylist.data.local.entity.TaskStatus
import com.fourctech.todaylist.domain.model.RecurrenceType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class TodayListConvertersTest {
    private val converters = TodayListConverters()

    @Test
    fun instant_roundTrips() {
        val value = Instant.parse("2026-08-27T15:30:00Z")
        assertThat(converters.toInstant(converters.fromInstant(value))).isEqualTo(value)
        assertThat(converters.fromInstant(null)).isNull()
        assertThat(converters.toInstant(null)).isNull()
    }

    @Test
    fun localDate_roundTrips() {
        val value = LocalDate.of(2026, 8, 27)
        assertThat(converters.toLocalDate(converters.fromLocalDate(value))).isEqualTo(value)
        assertThat(converters.fromLocalDate(null)).isNull()
        assertThat(converters.toLocalDate(null)).isNull()
    }

    @Test
    fun taskStatus_roundTrips() {
        TaskStatus.entries.forEach { status ->
            assertThat(converters.toTaskStatus(converters.fromTaskStatus(status))).isEqualTo(status)
        }
    }

    @Test
    fun recurrenceType_roundTrips() {
        RecurrenceType.entries.forEach { type ->
            assertThat(converters.toRecurrenceType(converters.fromRecurrenceType(type))).isEqualTo(type)
        }
    }
}
