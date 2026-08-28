package com.fourctech.todaylist.domain.model

import java.time.LocalDate

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val rolloverMode: RolloverMode = RolloverMode.ASK,
    val weekStart: WeekStart = WeekStart.SUNDAY,
    val hapticsEnabled: Boolean = true,
    val adsRemovedCached: Boolean = false,
    val lastRolloverDate: LocalDate? = null,
    val notificationPermissionPrompted: Boolean = false,
)
