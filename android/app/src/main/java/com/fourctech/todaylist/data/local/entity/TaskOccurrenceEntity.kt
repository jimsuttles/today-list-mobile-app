package com.fourctech.todaylist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "task_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("occurrenceDate")],
)
data class TaskOccurrenceEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val occurrenceDate: LocalDate,
    val completedAt: Instant?,
    val movedToLater: Boolean,
    val createdAt: Instant,
)
