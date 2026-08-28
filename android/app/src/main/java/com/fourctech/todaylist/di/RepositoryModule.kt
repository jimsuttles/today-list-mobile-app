package com.fourctech.todaylist.di

import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.core.time.SystemClockProvider
import com.fourctech.todaylist.data.repository.RoomTaskRepository
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
    abstract fun bindClockProvider(impl: SystemClockProvider): ClockProvider
}
