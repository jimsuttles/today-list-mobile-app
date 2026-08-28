package com.fourctech.todaylist.core.analytics

/**
 * Crash / non-fatal reporting facade. Never attach titles or notes.
 */
interface CrashReporter {
    fun log(message: String)

    fun record(throwable: Throwable)

    fun setEnabled(enabled: Boolean)
}
