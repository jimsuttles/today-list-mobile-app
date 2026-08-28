package com.fourctech.todaylist.domain.repository

import com.fourctech.todaylist.domain.model.CompletionRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeHistory(): Flow<List<CompletionRecord>>

    fun observeHistoryByDate(date: LocalDate): Flow<List<CompletionRecord>>

    suspend fun getCompletion(id: String): CompletionRecord?
}
