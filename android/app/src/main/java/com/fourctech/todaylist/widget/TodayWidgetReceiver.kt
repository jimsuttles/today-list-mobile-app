package com.fourctech.todaylist.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Ensure content is composed when the first instance is added.
    }
}
