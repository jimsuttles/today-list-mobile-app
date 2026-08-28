package com.fourctech.todaylist.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fourctech.todaylist.data.local.converter.TodayListConverters
import com.fourctech.todaylist.data.local.dao.CompletionEventDao
import com.fourctech.todaylist.data.local.dao.OccurrenceDao
import com.fourctech.todaylist.data.local.dao.RecurrenceDao
import com.fourctech.todaylist.data.local.dao.TaskDao
import com.fourctech.todaylist.data.local.entity.CompletionEventEntity
import com.fourctech.todaylist.data.local.entity.RecurrenceEntity
import com.fourctech.todaylist.data.local.entity.TaskEntity
import com.fourctech.todaylist.data.local.entity.TaskOccurrenceEntity

@Database(
    entities = [
        RecurrenceEntity::class,
        TaskEntity::class,
        TaskOccurrenceEntity::class,
        CompletionEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(TodayListConverters::class)
abstract class TodayListDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun recurrenceDao(): RecurrenceDao
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun completionEventDao(): CompletionEventDao
}
