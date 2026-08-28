package com.fourctech.todaylist.domain.model

import java.time.Instant
import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val location: TaskLocation,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val scheduledDate: LocalDate?,
    val reminderAt: Instant?,
    val recurrence: RecurrenceRule?,
)
