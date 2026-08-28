package com.fourctech.todaylist.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fourctech.todaylist.MainActivity
import com.fourctech.todaylist.R

object ReminderNotifications {
    /** v2: HIGH importance. Android won't upgrade an existing channel's importance. */
    const val CHANNEL_ID = "task_reminders_v2"
    private const val CHANNEL_NAME = "Task reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // Remove quiet v1 channel if present so Settings isn't confusing.
        runCatching { manager.deleteNotificationChannel("task_reminders") }
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for Today List tasks"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, taskId: String, title: String) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.w("ReminderNotifications", "Notifications disabled; not posting for $taskId")
            return
        }
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse(ReminderIntents.taskDeepLink(taskId))
            putExtra(ReminderIntents.EXTRA_TASK_ID, taskId)
        }
        val pending = PendingIntent.getActivity(
            context,
            ReminderIntents.requestCodeFor(taskId),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(title.ifBlank { context.getString(R.string.reminder_fallback_title) })
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        manager.notify(
            ReminderIntents.requestCodeFor(taskId),
            notification,
        )
        Log.i("ReminderNotifications", "Posted reminder notification for $taskId")
    }
}
