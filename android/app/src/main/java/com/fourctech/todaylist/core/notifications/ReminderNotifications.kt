package com.fourctech.todaylist.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fourctech.todaylist.MainActivity
import com.fourctech.todaylist.R

object ReminderNotifications {
    const val CHANNEL_ID = "task_reminders"
    private const val CHANNEL_NAME = "Task reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders for Today List tasks"
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, taskId: String, title: String) {
        ensureChannel(context)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(title)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(
            ReminderIntents.requestCodeFor(taskId),
            notification,
        )
    }
}
