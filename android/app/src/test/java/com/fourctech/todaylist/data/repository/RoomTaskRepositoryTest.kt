package com.fourctech.todaylist.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fourctech.todaylist.core.time.FakeClockProvider
import com.fourctech.todaylist.data.local.db.TodayListDatabase
import com.fourctech.todaylist.data.local.entity.TaskStatus
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.recurrence.DefaultRecurrenceEngine
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
            recurrenceEngine = DefaultRecurrenceEngine(),
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
    fun updateTask_movesLocationAndSavesNotes() = runTest {
        val created = repository.createTask(title = "Draft", location = TaskLocation.TODAY)

        repository.updateTask(
            created.copy(
                title = "Ready",
                notes = "Details",
                location = TaskLocation.LATER,
            ),
        )

        assertThat(repository.observeTodayTasks().first()).isEmpty()
        val later = repository.observeLaterTasks().first()
        assertThat(later).hasSize(1)
        assertThat(later.first().title).isEqualTo("Ready")
        assertThat(later.first().notes).isEqualTo("Details")
        assertThat(later.first().location).isEqualTo(TaskLocation.LATER)
    }

    @Test
    fun deleteTask_thisTask_leavesRecurrenceRowWhenSeriesKept() = runTest {
        val created = repository.createTask(
            title = "Repeat me",
            location = TaskLocation.TODAY,
            recurrence = com.fourctech.todaylist.domain.model.RecurrenceRule(
                id = "rule-1",
                type = com.fourctech.todaylist.domain.model.RecurrenceType.DAILY,
                interval = 1,
                startDate = clock.today(),
                endDate = null,
                weekdays = emptySet(),
                dayOfMonth = null,
            ),
        )

        repository.deleteTask(created.id, com.fourctech.todaylist.domain.model.DeleteScope.THIS_TASK)

        assertThat(repository.getTask(created.id)).isNull()
        assertThat(database.recurrenceDao().getById("rule-1")).isNotNull()
    }

    @Test
    fun deleteTask_entireSeries_removesRecurrence() = runTest {
        val created = repository.createTask(
            title = "Series",
            location = TaskLocation.TODAY,
            recurrence = com.fourctech.todaylist.domain.model.RecurrenceRule(
                id = "rule-2",
                type = com.fourctech.todaylist.domain.model.RecurrenceType.WEEKLY,
                interval = 1,
                startDate = clock.today(),
                endDate = null,
                weekdays = setOf(java.time.DayOfWeek.MONDAY),
                dayOfMonth = null,
            ),
        )

        repository.deleteTask(created.id, com.fourctech.todaylist.domain.model.DeleteScope.ENTIRE_SERIES)

        assertThat(repository.getTask(created.id)).isNull()
        assertThat(database.recurrenceDao().getById("rule-2")).isNull()
    }

    @Test
    fun completeRecurringTask_createsNextOccurrenceOnToday() = runTest {
        val created = repository.createTask(
            title = "Daily stretch",
            location = TaskLocation.TODAY,
            recurrence = com.fourctech.todaylist.domain.model.RecurrenceRule(
                id = "daily-rule",
                type = com.fourctech.todaylist.domain.model.RecurrenceType.DAILY,
                interval = 1,
                startDate = clock.today(),
                endDate = null,
                weekdays = emptySet(),
                dayOfMonth = null,
            ),
        )

        repository.completeTask(created.id)

        // Next occurrence is tomorrow — not due on Today yet.
        assertThat(repository.observeTodayTasks().first()).isEmpty()
        val stored = database.taskDao().getTaskById(created.id)
        assertThat(stored?.status).isEqualTo(TaskStatus.TODAY)
        assertThat(stored?.scheduledDate).isEqualTo(clock.today().plusDays(1))
        assertThat(database.completionEventDao().observeAll().first()).hasSize(1)
        assertThat(database.occurrenceDao().getOpenOccurrence(created.id)?.occurrenceDate)
            .isEqualTo(clock.today().plusDays(1))
    }

    @Test
    fun completeRecurringTask_endsWhenPastEndDate() = runTest {
        val created = repository.createTask(
            title = "Finite",
            location = TaskLocation.TODAY,
            scheduledDate = clock.today(),
            recurrence = com.fourctech.todaylist.domain.model.RecurrenceRule(
                id = "finite-rule",
                type = com.fourctech.todaylist.domain.model.RecurrenceType.DAILY,
                interval = 1,
                startDate = clock.today(),
                endDate = clock.today(),
                weekdays = emptySet(),
                dayOfMonth = null,
            ),
        )

        repository.completeTask(created.id)

        assertThat(repository.observeTodayTasks().first()).isEmpty()
        assertThat(database.completionEventDao().observeAll().first()).hasSize(1)
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

    @Test
    fun deleteAllUserContent_clearsTasksAndHistory() = runTest {
        val created = repository.createTask(title = "Wipe me", location = TaskLocation.TODAY)
        repository.completeTask(created.id)
        repository.createTask(title = "Still open", location = TaskLocation.LATER)

        assertThat(repository.getAllTaskIds()).isNotEmpty()
        repository.deleteAllUserContent()

        assertThat(repository.getAllTaskIds()).isEmpty()
        assertThat(repository.observeTodayTasks().first()).isEmpty()
        assertThat(repository.observeLaterTasks().first()).isEmpty()
        assertThat(database.completionEventDao().observeAll().first()).isEmpty()
    }
}
