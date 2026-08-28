package com.fourctech.todaylist.domain.rollover

import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 stub: records that launch evaluated the day boundary.
 * Full unfinished-task review lands in Phase 4.
 */
@Singleton
class RolloverStub @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val clock: ClockProvider,
) {
    suspend fun evaluateOnLaunch() {
        val today = clock.today()
        val last = settingsRepository.getSettings().lastRolloverDate
        if (last == null || last.isBefore(today)) {
            settingsRepository.setLastRolloverDate(today)
        }
    }
}
