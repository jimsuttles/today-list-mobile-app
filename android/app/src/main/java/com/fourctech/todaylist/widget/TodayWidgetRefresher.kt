package com.fourctech.todaylist.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.fourctech.todaylist.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps home-screen Today widgets in sync with Room. */
@Singleton
class TodayWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            taskRepository.observeTodayTasks()
                .map { tasks -> tasks.map { Triple(it.id, it.title, it.sortOrder) } }
                .distinctUntilChanged()
                .collect {
                    TodayGlanceWidget().updateAll(context)
                }
        }
    }
}
