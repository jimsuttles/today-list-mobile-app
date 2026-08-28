package com.fourctech.todaylist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fourctech.todaylist.data.local.entity.TaskOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {
    @Query("SELECT * FROM task_occurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskOccurrenceEntity?

    @Query(
        """
        SELECT * FROM task_occurrences
        WHERE taskId = :taskId
        ORDER BY occurrenceDate DESC
        """,
    )
    fun observeForTask(taskId: String): Flow<List<TaskOccurrenceEntity>>

    @Query(
        """
        SELECT * FROM task_occurrences
        WHERE taskId = :taskId AND completedAt IS NULL
        ORDER BY occurrenceDate ASC
        LIMIT 1
        """,
    )
    suspend fun getOpenOccurrence(taskId: String): TaskOccurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(occurrence: TaskOccurrenceEntity)

    @Update
    suspend fun update(occurrence: TaskOccurrenceEntity)

    @Query("DELETE FROM task_occurrences WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)
}
