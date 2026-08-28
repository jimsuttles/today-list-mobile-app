package com.fourctech.todaylist

import android.app.Application
import com.fourctech.todaylist.core.notifications.ReminderNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TodayListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
    }
}
