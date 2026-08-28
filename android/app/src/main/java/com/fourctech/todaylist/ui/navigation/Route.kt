package com.fourctech.todaylist.ui.navigation

sealed class Route(val path: String) {
    data object Today : Route("today")
    data object Later : Route("later")
    data object History : Route("history")
    data object Settings : Route("settings")

    companion object {
        val bottomBar = listOf(Today, Later, History, Settings)
    }
}
