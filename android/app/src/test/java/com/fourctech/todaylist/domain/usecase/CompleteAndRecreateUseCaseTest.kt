package com.fourctech.todaylist.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fourctech.todaylist.core.notifications.NoOpNotificationScheduler
import com.fourctech.todaylist.core.time.FakeClockProvider
import com.fourctech.todaylist.data.local.db.TodayListDatabase
import com.fourctech.todaylist.data.repository.RoomHistoryRepository
import com.fourctech.todaylist.data.repository.RoomTaskRepository
import com.fourctech.todaylist.domain.model.TaskLocation
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompleteAndRecreateUseCaseTest {
    private lateinit var database: TodayListDatabase
    private lateinit var taskRepository: RoomTaskRepository
    private lateinit var historyRepository: RoomHistoryRepository
    private lateinit var completeTask: CompleteTaskUseCase
    private lateinit var undoComplete: UndoCompleteTaskUseCase
    private lateinit var recreate: RecreateTaskFromHistoryUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TodayListDatabase::class.java)
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .allowMainThreadQueries()
            .build()
        taskRepository = RoomTaskRepository(
            database = database,
            taskDao = database.taskDao(),
            recurrenceDao = database.recurrenceDao(),
            occurrenceDao = database.occurrenceDao(),
            completionEventDao = database.completionEventDao(),
            clock = FakeClockProvider(),
        )
        historyRepository = RoomHistoryRepository(database.completionEventDao())
        completeTask = CompleteTaskUseCase(taskRepository, NoOpNotificationScheduler())
        undoComplete = UndoCompleteTaskUseCase(taskRepository)
        recreate = RecreateTaskFromHistoryUseCase(historyRepository, taskRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun complete_thenUndo_restoresToday() = runTest {
        val created = taskRepository.createTask(title = "Milk", location = TaskLocation.TODAY)
        val result = completeTask(created.id)!!

        assertThat(taskRepository.observeTodayTasks().first()).isEmpty()
        undoComplete(result.completionEventId, result.previousLocation)

        val today = taskRepository.observeTodayTasks().first()
        assertThat(today.map { it.title }).containsExactly("Milk")
    }

    @Test
    fun recreateFromHistory_createsNewTaskAndKeepsRecord() = runTest {
        val created = taskRepository.createTask(title = "Call mom", location = TaskLocation.TODAY)
        val result = completeTask(created.id)!!

        val recreated = recreate(result.completionEventId, TaskLocation.LATER)!!

        assertThat(recreated.title).isEqualTo("Call mom")
        assertThat(recreated.location).isEqualTo(TaskLocation.LATER)
        assertThat(historyRepository.observeHistory().first()).hasSize(1)
        assertThat(taskRepository.observeLaterTasks().first()).hasSize(1)
    }
}
