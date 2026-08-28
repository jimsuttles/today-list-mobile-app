package com.fourctech.todaylist.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fourctech.todaylist.core.time.FakeClockProvider
import com.fourctech.todaylist.data.local.db.TodayListDatabase
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
class RoomTaskRepositoryTest {
    private lateinit var database: TodayListDatabase
    private lateinit var repository: RoomTaskRepository
    private val clock = FakeClockProvider()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TodayListDatabase::class.java)
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .allowMainThreadQueries()
            .build()
        repository = RoomTaskRepository(
            database = database,
            taskDao = database.taskDao(),
            recurrenceDao = database.recurrenceDao(),
            occurrenceDao = database.occurrenceDao(),
            completionEventDao = database.completionEventDao(),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createTask_appearsInToday() = runTest {
        val created = repository.createTask(title = "Buy milk", location = TaskLocation.TODAY)

        val today = repository.observeTodayTasks().first()
        assertThat(today).hasSize(1)
        assertThat(today.first().id).isEqualTo(created.id)
        assertThat(today.first().title).isEqualTo("Buy milk")
        assertThat(today.first().location).isEqualTo(TaskLocation.TODAY)
    }

    @Test
    fun moveToLater_removesFromToday() = runTest {
        val created = repository.createTask(title = "Call dentist", location = TaskLocation.TODAY)

        repository.moveToLater(created.id)

        assertThat(repository.observeTodayTasks().first()).isEmpty()
        val later = repository.observeLaterTasks().first()
        assertThat(later).hasSize(1)
        assertThat(later.first().title).isEqualTo("Call dentist")
    }

    @Test
    fun completeTask_storesTitleSnapshotAndRemovesFromToday() = runTest {
        val created = repository.createTask(title = "Original title", location = TaskLocation.TODAY)
        repository.updateTask(created.copy(title = "Edited title"))

        val eventId = repository.completeTask(created.id)

        assertThat(eventId).isNotNull()
        assertThat(repository.observeTodayTasks().first()).isEmpty()
        val events = database.completionEventDao().observeAll().first()
        assertThat(events).hasSize(1)
        assertThat(events.first().id).isEqualTo(eventId)
        assertThat(events.first().titleSnapshot).isEqualTo("Edited title")
        assertThat(events.first().taskId).isEqualTo(created.id)
    }

    @Test
    fun uncompleteTask_restoresToPreviousLocation() = runTest {
        val created = repository.createTask(title = "Parked", location = TaskLocation.LATER)

        val eventId = repository.completeTask(created.id)!!
        repository.uncompleteTask(eventId, TaskLocation.LATER)

        assertThat(repository.observeTodayTasks().first()).isEmpty()
        val later = repository.observeLaterTasks().first()
        assertThat(later).hasSize(1)
        assertThat(later.first().title).isEqualTo("Parked")
        assertThat(database.completionEventDao().observeAll().first()).isEmpty()
    }

    @Test
    fun reorderTasks_updatesSortOrder() = runTest {
        val a = repository.createTask(title = "A", location = TaskLocation.TODAY)
        val b = repository.createTask(title = "B", location = TaskLocation.TODAY)
        val c = repository.createTask(title = "C", location = TaskLocation.TODAY)

        repository.reorderTasks(TaskLocation.TODAY, listOf(c.id, a.id, b.id))

        val today = repository.observeTodayTasks().first()
        assertThat(today.map { it.title }).containsExactly("C", "A", "B").inOrder()
    }
}
