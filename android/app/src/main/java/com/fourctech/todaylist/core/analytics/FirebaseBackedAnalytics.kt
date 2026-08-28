package com.fourctech.todaylist.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.fourctech.todaylist.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends events to Firebase Analytics when [google-services.json] is present and Firebase
 * initializes; mirrors to Logcat in debug builds.
 */
@Singleton
class FirebaseBackedAnalytics @Inject constructor(
    @ApplicationContext context: Context,
) : Analytics {
    private val firebase: FirebaseAnalytics? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: return@runCatching null
        }
        FirebaseAnalytics.getInstance(context)
    }.getOrNull()

    override fun log(event: String, params: Map<String, Any?>) {
        requirePrivacySafe(params)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "$event $params")
        }
        val analytics = firebase ?: return
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(event, bundle)
    }

    private companion object {
        const val TAG = "TodayListAnalytics"
    }
}
