package com.fourctech.todaylist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fourctech.todaylist.data.local.entity.RecurrenceEntity

@Dao
interface RecurrenceDao {
    @Query("SELECT * FROM recurrence_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurrenceEntity)

    @Update
    suspend fun update(rule: RecurrenceEntity)

    @Query("DELETE FROM recurrence_rules WHERE id = :id")
    suspend fun delete(id: String)
}
