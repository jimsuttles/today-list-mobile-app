package com.fourctech.todaylist.di

import com.fourctech.todaylist.data.repository.DataStoreSettingsRepository
import com.fourctech.todaylist.data.repository.RoomHistoryRepository
import com.fourctech.todaylist.data.repository.RoomTaskRepository
import com.fourctech.todaylist.domain.repository.HistoryRepository
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: RoomTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
