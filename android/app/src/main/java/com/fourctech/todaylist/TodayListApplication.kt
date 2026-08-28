package com.fourctech.todaylist

import android.app.Application
import android.util.Log
import com.fourctech.todaylist.core.ads.AdsManager
import com.fourctech.todaylist.core.notifications.ReminderNotifications
import com.fourctech.todaylist.widget.TodayWidgetRefresher
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodayListApplication : Application() {

    @Inject
    lateinit var widgetRefresher: TodayWidgetRefresher

    @Inject
    lateinit var adsManager: AdsManager

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
        initializeFirebase()
        widgetRefresher.start()
    }

    fun initializeAdsIfNeeded() {
        adsManager.initialize()
    }

    private fun initializeFirebase() {
        runCatching {
            FirebaseApp.initializeApp(this)
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
            }
        }.onFailure {
            Log.w(TAG, "Firebase not configured yet (add android/app/google-services.json).", it)
        }
    }

    companion object {
        private const val TAG = "TodayListApp"
    }
}
