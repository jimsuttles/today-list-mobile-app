package com.fourctech.todaylist.core.analytics

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCrashReporter @Inject constructor(
    @ApplicationContext context: Context,
) : CrashReporter {
    private val crashlytics: FirebaseCrashlytics? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: return@runCatching null
        }
        FirebaseCrashlytics.getInstance().also {
            it.isCrashlyticsCollectionEnabled = true
        }
    }.getOrNull()

    override fun log(message: String) {
        Log.d(TAG, message)
        crashlytics?.log(message)
    }

    override fun record(throwable: Throwable) {
        Log.w(TAG, "Non-fatal", throwable)
        crashlytics?.recordException(throwable)
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics?.isCrashlyticsCollectionEnabled = enabled
    }

    companion object {
        private const val TAG = "TodayListCrash"
    }
}
