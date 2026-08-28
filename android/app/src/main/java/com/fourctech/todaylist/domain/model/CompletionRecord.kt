package com.fourctech.todaylist.domain.model

import java.time.Instant
import java.time.LocalDate

data class CompletionRecord(
    val id: String,
    val taskId: String?,
    val occurrenceId: String?,
    val title: String,
    val completedAt: Instant,
    val completionDate: LocalDate,
)
