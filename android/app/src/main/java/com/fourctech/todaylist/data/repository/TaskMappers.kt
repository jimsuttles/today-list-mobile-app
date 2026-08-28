package com.fourctech.todaylist.data.repository

import com.fourctech.todaylist.data.local.entity.RecurrenceEntity
import com.fourctech.todaylist.data.local.entity.TaskEntity
import com.fourctech.todaylist.data.local.entity.TaskStatus
import com.fourctech.todaylist.domain.model.RecurrenceRule
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import java.time.DayOfWeek

internal fun TaskStatus.toLocationOrNull(): TaskLocation? =
    when (this) {
        TaskStatus.TODAY -> TaskLocation.TODAY
        TaskStatus.LATER -> TaskLocation.LATER
        TaskStatus.DELETED -> null
    }

internal fun TaskLocation.toStatus(): TaskStatus =
    when (this) {
        TaskLocation.TODAY -> TaskStatus.TODAY
        TaskLocation.LATER -> TaskStatus.LATER
    }

internal fun RecurrenceEntity.toDomain(): RecurrenceRule =
    RecurrenceRule(
        id = id,
        type = type,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        weekdays = weekdays.toDayOfWeekSet(),
        dayOfMonth = dayOfMonth,
    )

internal fun RecurrenceRule.toEntity(): RecurrenceEntity =
    RecurrenceEntity(
        id = id,
        type = type,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        weekdays = weekdays.toStorageString(),
        dayOfMonth = dayOfMonth,
    )

internal fun TaskEntity.toDomain(recurrence: RecurrenceRule?): Task? {
    val location = status.toLocationOrNull() ?: return null
    return Task(
        id = id,
        title = title,
        notes = notes,
        location = location,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        scheduledDate = scheduledDate,
        reminderAt = reminderAt,
        recurrence = recurrence,
    )
}

internal fun Task.toEntity(status: TaskStatus = location.toStatus()): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        notes = notes,
        status = status,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        scheduledDate = scheduledDate,
        reminderAt = reminderAt,
        recurrenceId = recurrence?.id,
    )

internal fun String?.toDayOfWeekSet(): Set<DayOfWeek> {
    if (this.isNullOrBlank()) return emptySet()
    return split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { DayOfWeek.valueOf(it) }
        .toSet()
}

internal fun Set<DayOfWeek>.toStorageString(): String? =
    if (isEmpty()) null else sorted().joinToString(",") { it.name }
