package com.fourctech.todaylist.core.notifications

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 7 will schedule reminders. Phase 3 only cancels on complete.
 */
interface NotificationScheduler {
    suspend fun cancelReminder(taskId: String)
}

@Singleton
class NoOpNotificationScheduler @Inject constructor() : NotificationScheduler {
    override suspend fun cancelReminder(taskId: String) = Unit
}
