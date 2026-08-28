package com.fourctech.todaylist.core.notifications

import java.time.Instant

interface NotificationScheduler {
    suspend fun scheduleReminder(taskId: String, title: String, at: Instant)

    suspend fun cancelReminder(taskId: String)
}
