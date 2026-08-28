package com.fourctech.todaylist.core.analytics

class FakeAnalytics : Analytics {
    data class Event(val name: String, val params: Map<String, Any?>)

    val events = mutableListOf<Event>()

    override fun log(event: String, params: Map<String, Any?>) {
        requirePrivacySafe(params)
        events += Event(event, params)
    }
}
