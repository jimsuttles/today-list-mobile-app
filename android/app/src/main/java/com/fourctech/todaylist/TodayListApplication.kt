package com.fourctech.todaylist

import android.app.Application
import com.fourctech.todaylist.core.notifications.ReminderNotifications
import com.fourctech.todaylist.widget.TodayWidgetRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodayListApplication : Application() {

    @Inject
    lateinit var widgetRefresher: TodayWidgetRefresher

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
        widgetRefresher.start()
    }
}
