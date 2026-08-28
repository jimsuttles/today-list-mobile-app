package com.fourctech.todaylist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fourctech.todaylist.data.local.entity.TaskEntity
import com.fourctech.todaylist.data.local.entity.TaskStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'TODAY'
        ORDER BY sortOrder ASC
        """,
    )
    fun observeTodayTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'LATER'
        ORDER BY sortOrder ASC
        """,
    )
    fun observeLaterTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun observeTaskById(id: String): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: TaskStatus, updatedAt: Instant)

    @Query("UPDATE tasks SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Instant)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'TODAY'
        ORDER BY sortOrder ASC
        """,
    )
    suspend fun getTodayTasks(): List<TaskEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks WHERE status = :status")
    suspend fun maxSortOrder(status: TaskStatus): Int
}
