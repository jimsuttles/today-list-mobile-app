package com.fourctech.todaylist.di

import android.content.Context
import androidx.room.Room
import com.fourctech.todaylist.data.local.dao.CompletionEventDao
import com.fourctech.todaylist.data.local.dao.OccurrenceDao
import com.fourctech.todaylist.data.local.dao.RecurrenceDao
import com.fourctech.todaylist.data.local.dao.TaskDao
import com.fourctech.todaylist.data.local.db.TodayListDatabase
import com.fourctech.todaylist.data.local.db.TodayListMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TodayListDatabase {
        val builder = Room.databaseBuilder(
            context,
            TodayListDatabase::class.java,
            "today_list.db",
        )
        // Future schema upgrades must add Migration objects to TodayListMigrations.ALL.
        // Do not enable fallbackToDestructiveMigration.
        val migrations = TodayListMigrations.ALL
        if (migrations.isNotEmpty()) {
            builder.addMigrations(*migrations)
        }
        return builder.build()
    }

    @Provides
    fun provideTaskDao(db: TodayListDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideRecurrenceDao(db: TodayListDatabase): RecurrenceDao = db.recurrenceDao()

    @Provides
    fun provideOccurrenceDao(db: TodayListDatabase): OccurrenceDao = db.occurrenceDao()

    @Provides
    fun provideCompletionEventDao(db: TodayListDatabase): CompletionEventDao = db.completionEventDao()
}
