package com.fourctech.todaylist.domain.rollover

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fourctech.todaylist.core.time.FakeClockProvider
import com.fourctech.todaylist.data.local.db.TodayListDatabase
import com.fourctech.todaylist.data.repository.DataStoreSettingsRepository
import com.fourctech.todaylist.data.repository.RoomTaskRepository
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.TaskLocation
import com.google.common.truth.Truth.assertThat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import java.time.Instant
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
class RolloverManagerTest {
    private lateinit var database: TodayListDatabase
    private lateinit var taskRepository: RoomTaskRepository
    private lateinit var settingsRepository: DataStoreSettingsRepository
    private lateinit var clock: FakeClockProvider
    private lateinit var runDailyRollover: RunDailyRolloverUseCase
    private lateinit var manager: RolloverManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TodayListDatabase::class.java)
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .allowMainThreadQueries()
            .build()
        clock = FakeClockProvider(Instant.parse("2026-08-27T12:00:00Z"))
        taskRepository = RoomTaskRepository(
            database = database,
            taskDao = database.taskDao(),
            recurrenceDao = database.recurrenceDao(),
            occurrenceDao = database.occurrenceDao(),
            completionEventDao = database.completionEventDao(),
            clock = clock,
        )
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("rollover_test_${System.nanoTime()}") },
        )
        settingsRepository = DataStoreSettingsRepository(dataStore)
        runDailyRollover = RunDailyRolloverUseCase(taskRepository, settingsRepository, clock)
        manager = RolloverManager(settingsRepository, taskRepository, clock, runDailyRollover)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun askMode_withUnfinished_needsReviewWithoutMarkingComplete() = runTest {
        settingsRepository.setRolloverMode(RolloverMode.ASK)
        settingsRepository.setLastRolloverDate(clock.today().minusDays(1))
        taskRepository.createTask(title = "Leftover", location = TaskLocation.TODAY)

        val result = manager.evaluate()

        assertThat(result).isInstanceOf(RolloverEvaluation.NeedsReview::class.java)
        val review = result as RolloverEvaluation.NeedsReview
        assertThat(review.unfinished).hasSize(1)
        assertThat(review.missedDays).isEqualTo(1)
        assertThat(settingsRepository.getSettings().lastRolloverDate)
            .isEqualTo(clock.today().minusDays(1))
    }

    @Test
    fun askMode_multiDayGap_isOneConsolidatedReview() = runTest {
        settingsRepository.setRolloverMode(RolloverMode.ASK)
        settingsRepository.setLastRolloverDate(clock.today().minusDays(4))
        taskRepository.createTask(title = "A", location = TaskLocation.TODAY)
        taskRepository.createTask(title = "B", location = TaskLocation.TODAY)

        val result = manager.evaluate() as RolloverEvaluation.NeedsReview

        assertThat(result.missedDays).isEqualTo(4)
        assertThat(result.unfinished).hasSize(2)
    }

    @Test
    fun autoLater_movesUnfinishedAndMarksComplete() = runTest {
        settingsRepository.setRolloverMode(RolloverMode.AUTO_LATER)
        settingsRepository.setLastRolloverDate(clock.today().minusDays(1))
        taskRepository.createTask(title = "Park me", location = TaskLocation.TODAY)

        val result = manager.evaluate() as RolloverEvaluation.AppliedSilently

        assertThat(result.movedToLaterCount).isEqualTo(1)
        assertThat(taskRepository.observeTodayTasks().first()).isEmpty()
        assertThat(taskRepository.observeLaterTasks().first()).hasSize(1)
        assertThat(settingsRepository.getSettings().lastRolloverDate).isEqualTo(clock.today())
    }

    @Test
    fun autoToday_keepsTasksAndMarksComplete() = runTest {
        settingsRepository.setRolloverMode(RolloverMode.AUTO_TODAY)
        settingsRepository.setLastRolloverDate(clock.today().minusDays(2))
        val created = taskRepository.createTask(title = "Stay", location = TaskLocation.TODAY)

        val result = manager.evaluate() as RolloverEvaluation.AppliedSilently

        assertThat(result.keptOnTodayCount).isEqualTo(1)
        val today = taskRepository.observeTodayTasks().first()
        assertThat(today).hasSize(1)
        assertThat(today.first().id).isEqualTo(created.id)
        assertThat(today.first().scheduledDate).isEqualTo(clock.today())
        assertThat(settingsRepository.getSettings().lastRolloverDate).isEqualTo(clock.today())
    }

    @Test
    fun alreadyUpToDate_isNoOp() = runTest {
        settingsRepository.setLastRolloverDate(clock.today())
        taskRepository.createTask(title = "Fresh", location = TaskLocation.TODAY)

        val result = manager.evaluate()

        assertThat(result).isInstanceOf(RolloverEvaluation.UpToDate::class.java)
        assertThat(taskRepository.observeTodayTasks().first()).hasSize(1)
    }

    @Test
    fun applyDecisions_splitsTodayAndLater() = runTest {
        settingsRepository.setLastRolloverDate(clock.today().minusDays(1))
        val a = taskRepository.createTask(title = "A", location = TaskLocation.TODAY)
        val b = taskRepository.createTask(title = "B", location = TaskLocation.TODAY)

        runDailyRollover.applyDecisions(
            mapOf(a.id to TaskLocation.TODAY, b.id to TaskLocation.LATER),
        )

        assertThat(taskRepository.observeTodayTasks().first().map { it.id }).containsExactly(a.id)
        assertThat(taskRepository.observeLaterTasks().first().map { it.id }).containsExactly(b.id)
        assertThat(settingsRepository.getSettings().lastRolloverDate).isEqualTo(clock.today())
    }
}
