package com.fourctech.todaylist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fourctech.todaylist.domain.model.RecurrenceType
import java.time.LocalDate

@Entity(tableName = "recurrence_rules")
data class RecurrenceEntity(
    @PrimaryKey val id: String,
    val type: RecurrenceType,
    val interval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    /** Comma-separated weekdays, e.g. MON,TUE,WED,THU,FRI */
    val weekdays: String?,
    val dayOfMonth: Int?,
)
