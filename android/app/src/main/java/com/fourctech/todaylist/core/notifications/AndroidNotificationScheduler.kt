package com.fourctech.todaylist.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun scheduleReminder(taskId: String, title: String, at: Instant) {
        cancelReminder(taskId)
        val triggerAt = at.toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return

        ReminderNotifications.ensureChannel(context)
        val pending = broadcastPendingIntent(taskId, title)
        val manager = alarmManager ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms() -> {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            else -> {
                manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }

    override suspend fun cancelReminder(taskId: String) {
        val pending = broadcastPendingIntent(taskId, title = "")
        alarmManager?.cancel(pending)
        pending.cancel()
        NotificationManagerCompat.from(context).cancel(ReminderIntents.requestCodeFor(taskId))
    }

    private fun broadcastPendingIntent(taskId: String, title: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
            putExtra(ReminderIntents.EXTRA_TASK_ID, taskId)
            putExtra(ReminderIntents.EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderIntents.requestCodeFor(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
