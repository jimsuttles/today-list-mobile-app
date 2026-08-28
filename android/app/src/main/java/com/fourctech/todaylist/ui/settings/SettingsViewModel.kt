package com.fourctech.todaylist.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.analytics.AnalyticsParams
import com.fourctech.todaylist.core.analytics.toAnalyticsValue
import com.fourctech.todaylist.core.notifications.NotificationScheduler
import com.fourctech.todaylist.domain.model.AppSettings
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.model.WeekStart
import com.fourctech.todaylist.domain.repository.HistoryRepository
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val taskRepository: TaskRepository,
    private val notificationScheduler: NotificationScheduler,
    private val analytics: Analytics,
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            analytics.log(
                AnalyticsEvents.SETTINGS_THEME_CHANGED,
                mapOf(AnalyticsParams.THEME to mode.toAnalyticsValue()),
            )
        }
    }

    fun setRolloverMode(mode: RolloverMode) {
        viewModelScope.launch {
            settingsRepository.setRolloverMode(mode)
            analytics.log(
                AnalyticsEvents.SETTINGS_ROLLOVER_CHANGED,
                mapOf(AnalyticsParams.MODE to mode.toAnalyticsValue()),
            )
        }
    }

    fun setWeekStart(weekStart: WeekStart) {
        viewModelScope.launch {
            settingsRepository.setWeekStart(weekStart)
            analytics.log(
                AnalyticsEvents.SETTINGS_WEEK_START_CHANGED,
                mapOf(AnalyticsParams.WEEK_START to weekStart.toAnalyticsValue()),
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
            analytics.log(
                AnalyticsEvents.SETTINGS_HAPTICS_CHANGED,
                mapOf(AnalyticsParams.ENABLED to enabled),
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            analytics.log(
                AnalyticsEvents.DATA_CLEARED,
                mapOf(AnalyticsParams.KIND to AnalyticsParams.KIND_HISTORY),
            )
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            taskRepository.getAllTaskIds().forEach { id ->
                notificationScheduler.cancelReminder(id)
            }
            taskRepository.deleteAllUserContent()
            settingsRepository.resetPreferencesKeepingEntitlement()
            analytics.log(
                AnalyticsEvents.DATA_CLEARED,
                mapOf(AnalyticsParams.KIND to AnalyticsParams.KIND_ALL),
            )
        }
    }
}
