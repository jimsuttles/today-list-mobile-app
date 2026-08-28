package com.fourctech.todaylist.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

class CompleteTodayTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TodayWidgetEntryPoint::class.java,
        )
        entryPoint.completeTaskUseCase().invoke(taskId)
        TodayGlanceWidget().update(context, glanceId)
    }

    companion object {
        val TASK_ID_KEY = ActionParameters.Key<String>("task_id")
    }
}
