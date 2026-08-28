package com.fourctech.todaylist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = RecurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("recurrenceId"), Index("status"), Index("sortOrder")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val status: TaskStatus,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val scheduledDate: LocalDate?,
    val reminderAt: Instant?,
    val recurrenceId: String?,
)
