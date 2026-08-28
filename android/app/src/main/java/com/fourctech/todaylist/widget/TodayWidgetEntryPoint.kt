package com.fourctech.todaylist.widget

import com.fourctech.todaylist.domain.repository.TaskRepository
import com.fourctech.todaylist.domain.usecase.CompleteTaskUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TodayWidgetEntryPoint {
    fun taskRepository(): TaskRepository

    fun completeTaskUseCase(): CompleteTaskUseCase
}
