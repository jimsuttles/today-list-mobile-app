package com.fourctech.todaylist.core.notifications

object ReminderIntents {
    const val ACTION_FIRE = "com.fourctech.todaylist.action.FIRE_REMINDER"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val SCHEME = "todaylist"
    const val HOST_TASK = "task"

    fun taskDeepLink(taskId: String): String = "$SCHEME://$HOST_TASK/$taskId"

    fun requestCodeFor(taskId: String): Int = taskId.hashCode()
}
