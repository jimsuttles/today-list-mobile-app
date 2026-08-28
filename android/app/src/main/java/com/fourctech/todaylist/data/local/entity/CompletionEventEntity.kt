package com.fourctech.todaylist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "completion_events",
    indices = [Index("completionDate"), Index("taskId"), Index("occurrenceId")],
)
data class CompletionEventEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val occurrenceId: String?,
    /** Immutable title at completion time for History accuracy. */
    val titleSnapshot: String,
    val completedAt: Instant,
    val completionDate: LocalDate,
)
