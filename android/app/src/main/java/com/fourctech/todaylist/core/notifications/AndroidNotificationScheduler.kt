package com.fourctech.todaylist.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.fourctech.todaylist.MainActivity
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
        val manager = alarmManager ?: return
        val fireIntent = broadcastPendingIntent(taskId, title)
        // setAlarmClock is the reliable user-facing path (not deferred like setAndAllowWhileIdle).
        val showIntent = PendingIntent.getActivity(
            context,
            ReminderIntents.requestCodeFor(taskId),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse(ReminderIntents.taskDeepLink(taskId))
                putExtra(ReminderIntents.EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), fireIntent)
    }

    override suspend fun cancelReminder(taskId: String) {
        val pending = broadcastPendingIntent(taskId, title = "")
        alarmManager?.cancel(pending)
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
