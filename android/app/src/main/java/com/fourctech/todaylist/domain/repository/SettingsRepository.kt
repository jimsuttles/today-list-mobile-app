package com.fourctech.todaylist.domain.repository

import com.fourctech.todaylist.domain.model.AppSettings
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.model.WeekStart
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun getSettings(): AppSettings

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setRolloverMode(mode: RolloverMode)

    suspend fun setWeekStart(weekStart: WeekStart)

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setAdsRemovedCached(removed: Boolean)

    suspend fun setLastRolloverDate(date: LocalDate?)

    suspend fun setNotificationPermissionPrompted(prompted: Boolean)

    /** Clears prefs but keeps [AppSettings.adsRemovedCached]. */
    suspend fun resetPreferencesKeepingEntitlement()
}
