package com.fourctech.todaylist.core.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugAnalytics @Inject constructor() : Analytics {
    override fun log(event: String, params: Map<String, Any?>) {
        requirePrivacySafe(params)
        Log.d(TAG, "$event $params")
    }

    companion object {
        private const val TAG = "TodayListAnalytics"
    }
}

internal fun requirePrivacySafe(params: Map<String, Any?>) {
    val forbidden = params.keys.filter { key ->
        val lower = key.lowercase()
        lower.contains("title") ||
            lower.contains("note") ||
            lower.contains("body") ||
            lower == "text" ||
            lower == "name"
    }
    check(forbidden.isEmpty()) {
        "Refusing to log privacy-sensitive analytics keys: $forbidden"
    }
}
