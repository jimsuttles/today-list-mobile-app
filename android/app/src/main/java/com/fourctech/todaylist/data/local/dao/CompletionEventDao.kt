package com.fourctech.todaylist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fourctech.todaylist.data.local.entity.CompletionEventEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CompletionEventEntity)

    @Query(
        """
        SELECT * FROM completion_events
        ORDER BY completedAt DESC
        """,
    )
    fun observeAll(): Flow<List<CompletionEventEntity>>

    @Query(
        """
        SELECT * FROM completion_events
        WHERE completionDate = :date
        ORDER BY completedAt DESC
        """,
    )
    fun observeByDate(date: LocalDate): Flow<List<CompletionEventEntity>>

    @Query("SELECT * FROM completion_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CompletionEventEntity?

    @Query("DELETE FROM completion_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM completion_events")
    suspend fun deleteAll()
}
