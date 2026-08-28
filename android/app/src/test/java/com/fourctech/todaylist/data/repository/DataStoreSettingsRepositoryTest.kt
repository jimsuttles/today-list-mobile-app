package com.fourctech.todaylist.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.model.WeekStart
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreSettingsRepositoryTest {
    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings_test_${System.nanoTime()}") },
        )
        repository = DataStoreSettingsRepository(dataStore)
    }

    @Test
    fun defaults_matchAppSettings() = runTest {
        val settings = repository.getSettings()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(settings.rolloverMode).isEqualTo(RolloverMode.ASK)
        assertThat(settings.weekStart).isEqualTo(WeekStart.SUNDAY)
        assertThat(settings.hapticsEnabled).isTrue()
        assertThat(settings.adsRemovedCached).isFalse()
        assertThat(settings.lastRolloverDate).isNull()
        assertThat(settings.notificationPermissionPrompted).isFalse()
    }

    @Test
    fun setters_persistAndEmit() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setRolloverMode(RolloverMode.AUTO_LATER)
        repository.setWeekStart(WeekStart.MONDAY)
        repository.setHapticsEnabled(false)
        repository.setAdsRemovedCached(true)
        repository.setLastRolloverDate(LocalDate.of(2026, 8, 27))
        repository.setNotificationPermissionPrompted(true)

        val settings = repository.observeSettings().first()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(settings.rolloverMode).isEqualTo(RolloverMode.AUTO_LATER)
        assertThat(settings.weekStart).isEqualTo(WeekStart.MONDAY)
        assertThat(settings.hapticsEnabled).isFalse()
        assertThat(settings.adsRemovedCached).isTrue()
        assertThat(settings.lastRolloverDate).isEqualTo(LocalDate.of(2026, 8, 27))
        assertThat(settings.notificationPermissionPrompted).isTrue()
    }

    @Test
    fun resetPreferencesKeepingEntitlement_clearsButKeepsAdsFlag() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setRolloverMode(RolloverMode.AUTO_TODAY)
        repository.setWeekStart(WeekStart.MONDAY)
        repository.setHapticsEnabled(false)
        repository.setAdsRemovedCached(true)
        repository.setLastRolloverDate(LocalDate.of(2026, 8, 27))
        repository.setNotificationPermissionPrompted(true)

        repository.resetPreferencesKeepingEntitlement()

        val settings = repository.getSettings()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(settings.rolloverMode).isEqualTo(RolloverMode.ASK)
        assertThat(settings.weekStart).isEqualTo(WeekStart.SUNDAY)
        assertThat(settings.hapticsEnabled).isTrue()
        assertThat(settings.adsRemovedCached).isTrue()
        assertThat(settings.lastRolloverDate).isNull()
        assertThat(settings.notificationPermissionPrompted).isFalse()
    }
}
