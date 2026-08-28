package com.fourctech.todaylist.core.notifications

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/** Test double that records schedule/cancel calls. */
class FakeNotificationScheduler : NotificationScheduler {
    data class Scheduled(val taskId: String, val title: String, val at: Instant)

    val scheduled = CopyOnWriteArrayList<Scheduled>()
    val cancelled = CopyOnWriteArrayList<String>()

    override suspend fun scheduleReminder(taskId: String, title: String, at: Instant) {
        cancelled.remove(taskId)
        scheduled.removeAll { it.taskId == taskId }
        scheduled.add(Scheduled(taskId, title, at))
    }

    override suspend fun cancelReminder(taskId: String) {
        scheduled.removeAll { it.taskId == taskId }
        cancelled.add(taskId)
    }
}
