package com.fourctech.todaylist.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.fourctech.todaylist.data.local.prefs.SettingsKeys
import com.fourctech.todaylist.domain.model.AppSettings
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.model.WeekStart
import com.fourctech.todaylist.domain.repository.SettingsRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map { prefs -> prefs.toAppSettings() }

    override suspend fun getSettings(): AppSettings =
        observeSettings().first()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[SettingsKeys.themeMode] = mode.name }
    }

    override suspend fun setRolloverMode(mode: RolloverMode) {
        dataStore.edit { it[SettingsKeys.rolloverMode] = mode.name }
    }

    override suspend fun setWeekStart(weekStart: WeekStart) {
        dataStore.edit { it[SettingsKeys.weekStart] = weekStart.name }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.hapticsEnabled] = enabled }
    }

    override suspend fun setAdsRemovedCached(removed: Boolean) {
        dataStore.edit { it[SettingsKeys.adsRemovedCached] = removed }
    }

    override suspend fun setLastRolloverDate(date: LocalDate?) {
        dataStore.edit { prefs ->
            if (date == null) {
                prefs.remove(SettingsKeys.lastRolloverDate)
            } else {
                prefs[SettingsKeys.lastRolloverDate] = date.toString()
            }
        }
    }

    override suspend fun setNotificationPermissionPrompted(prompted: Boolean) {
        dataStore.edit { it[SettingsKeys.notificationPermissionPrompted] = prompted }
    }

    override suspend fun resetPreferencesKeepingEntitlement() {
        dataStore.edit { prefs ->
            val entitled = prefs[SettingsKeys.adsRemovedCached] ?: false
            prefs.clear()
            prefs[SettingsKeys.adsRemovedCached] = entitled
            prefs[SettingsKeys.themeMode] = ThemeMode.SYSTEM.name
            prefs[SettingsKeys.rolloverMode] = RolloverMode.ASK.name
            prefs[SettingsKeys.weekStart] = WeekStart.SUNDAY.name
            prefs[SettingsKeys.hapticsEnabled] = true
            prefs[SettingsKeys.notificationPermissionPrompted] = false
        }
    }
}

private fun Preferences.toAppSettings(): AppSettings =
    AppSettings(
        themeMode = this[SettingsKeys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        rolloverMode = this[SettingsKeys.rolloverMode]?.let { runCatching { RolloverMode.valueOf(it) }.getOrNull() }
            ?: RolloverMode.ASK,
        weekStart = this[SettingsKeys.weekStart]?.let { runCatching { WeekStart.valueOf(it) }.getOrNull() }
            ?: WeekStart.SUNDAY,
        hapticsEnabled = this[SettingsKeys.hapticsEnabled] ?: true,
        adsRemovedCached = this[SettingsKeys.adsRemovedCached] ?: false,
        lastRolloverDate = this[SettingsKeys.lastRolloverDate]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        notificationPermissionPrompted = this[SettingsKeys.notificationPermissionPrompted] ?: false,
    )
