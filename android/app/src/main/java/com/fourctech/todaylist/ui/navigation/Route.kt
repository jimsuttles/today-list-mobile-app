package com.fourctech.todaylist.ui.navigation

sealed class Route(val path: String) {
    data object Today : Route("today")
    data object Later : Route("later")
    data object History : Route("history")
    data object Settings : Route("settings")
    data object RemoveAds : Route("settings/remove-ads")

    data class HistoryDetail(val completionId: String) : Route("history/$completionId") {
        companion object {
            const val pattern = "history/{completionId}"
            const val arg = "completionId"
        }
    }

    data class TaskDetail(val taskId: String) : Route("task/$taskId") {
        companion object {
            const val pattern = "task/{taskId}"
            const val arg = "taskId"
        }
    }

    companion object {
        val bottomBar = listOf(Today, Later, History, Settings)
    }
}
