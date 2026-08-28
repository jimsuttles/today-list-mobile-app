package com.fourctech.todaylist.core.analytics

/**
 * Privacy-safe analytics facade.
 * Never send task titles, notes, or other free-text user content.
 */
interface Analytics {
    fun log(event: String, params: Map<String, Any?> = emptyMap())
}

object AnalyticsEvents {
    const val APP_OPEN = "app_open"
    const val TASK_CREATED = "task_created"
    const val TASK_COMPLETED = "task_completed"
    const val TASK_UNDONE = "task_undone"
    const val TASK_MOVED = "task_moved"
    const val TASK_DELETED = "task_deleted"
    const val REMINDER_SET = "reminder_set"
    const val REMINDER_CLEARED = "reminder_cleared"
    const val NOTIFICATION_PERMISSION = "notification_permission"
    const val REPEAT_SET = "repeat_set"
    const val ROLLOVER_REVIEW_SHOWN = "rollover_review_shown"
    const val ROLLOVER_REVIEW_COMPLETED = "rollover_review_completed"
    const val ROLLOVER_AUTO_APPLIED = "rollover_auto_applied"
    const val HISTORY_OPENED = "history_opened"
    const val HISTORY_RECREATED = "history_recreated"
    const val SETTINGS_THEME_CHANGED = "settings_theme_changed"
    const val SETTINGS_ROLLOVER_CHANGED = "settings_rollover_changed"
    const val SETTINGS_WEEK_START_CHANGED = "settings_week_start_changed"
    const val SETTINGS_HAPTICS_CHANGED = "settings_haptics_changed"
    const val DATA_CLEARED = "data_cleared"
}

object AnalyticsParams {
    const val LOCATION = "location"
    const val TO_LOCATION = "to_location"
    const val SOURCE = "source"
    const val SCOPE = "scope"
    const val REPEAT = "repeat"
    const val MODE = "mode"
    const val THEME = "theme"
    const val WEEK_START = "week_start"
    const val ENABLED = "enabled"
    const val GRANTED = "granted"
    const val TASK_COUNT = "task_count"
    const val MISSED_DAYS = "missed_days"
    const val KEPT_TODAY_COUNT = "kept_today_count"
    const val MOVED_LATER_COUNT = "moved_later_count"
    const val KIND = "kind"

    const val LOCATION_TODAY = "today"
    const val LOCATION_LATER = "later"
    const val SOURCE_LIST = "list"
    const val SOURCE_WIDGET = "widget"
    const val SOURCE_DETAIL = "detail"
    const val KIND_HISTORY = "history"
    const val KIND_ALL = "all"
}
