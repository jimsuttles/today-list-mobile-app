package com.fourctech.todaylist.core.analytics

import com.fourctech.todaylist.domain.model.DeleteScope
import com.fourctech.todaylist.domain.model.RepeatOption
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.model.WeekStart

fun TaskLocation.toAnalyticsValue(): String = when (this) {
    TaskLocation.TODAY -> AnalyticsParams.LOCATION_TODAY
    TaskLocation.LATER -> AnalyticsParams.LOCATION_LATER
}

fun DeleteScope.toAnalyticsValue(): String = name.lowercase()

fun RepeatOption.toAnalyticsValue(): String = name.lowercase()

fun RolloverMode.toAnalyticsValue(): String = name.lowercase()

fun ThemeMode.toAnalyticsValue(): String = name.lowercase()

fun WeekStart.toAnalyticsValue(): String = name.lowercase()
