package com.fourctech.todaylist.data.repository

import com.fourctech.todaylist.data.local.dao.CompletionEventDao
import com.fourctech.todaylist.data.local.entity.CompletionEventEntity
import com.fourctech.todaylist.domain.model.CompletionRecord
import com.fourctech.todaylist.domain.repository.HistoryRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomHistoryRepository @Inject constructor(
    private val completionEventDao: CompletionEventDao,
) : HistoryRepository {

    override fun observeHistory(): Flow<List<CompletionRecord>> =
        completionEventDao.observeAll().map { events -> events.map { it.toDomain() } }

    override fun observeHistoryByDate(date: LocalDate): Flow<List<CompletionRecord>> =
        completionEventDao.observeByDate(date).map { events -> events.map { it.toDomain() } }

    override suspend fun getCompletion(id: String): CompletionRecord? =
        completionEventDao.getById(id)?.toDomain()
}

private fun CompletionEventEntity.toDomain(): CompletionRecord =
    CompletionRecord(
        id = id,
        taskId = taskId,
        occurrenceId = occurrenceId,
        title = titleSnapshot,
        completedAt = completedAt,
        completionDate = completionDate,
    )
