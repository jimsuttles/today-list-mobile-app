package com.fourctech.todaylist.data.local.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object SettingsKeys {
    val themeMode = stringPreferencesKey("theme_mode")
    val rolloverMode = stringPreferencesKey("rollover_mode")
    val weekStart = stringPreferencesKey("week_start")
    val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
    val adsRemovedCached = booleanPreferencesKey("ads_removed_cached")
    val lastRolloverDate = stringPreferencesKey("last_rollover_date")
    val notificationPermissionPrompted = booleanPreferencesKey("notification_permission_prompted")
}
