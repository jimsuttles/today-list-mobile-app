package com.fourctech.todaylist.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fourctech.todaylist.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderIntents.ACTION_FIRE) return
        val taskId = intent.getStringExtra(ReminderIntents.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(ReminderIntents.EXTRA_TASK_TITLE).orEmpty()
            .ifBlank { context.getString(R.string.reminder_fallback_title) }
        ReminderNotifications.show(context, taskId, title)
    }
}
