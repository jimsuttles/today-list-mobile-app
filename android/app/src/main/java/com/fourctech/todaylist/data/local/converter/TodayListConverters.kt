package com.fourctech.todaylist.data.local.converter

import androidx.room.TypeConverter
import com.fourctech.todaylist.data.local.entity.TaskStatus
import com.fourctech.todaylist.domain.model.RecurrenceType
import java.time.Instant
import java.time.LocalDate

class TodayListConverters {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus?): String? = value?.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? = value?.let(TaskStatus::valueOf)

    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType?): String? = value?.name

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType? = value?.let(RecurrenceType::valueOf)
}
