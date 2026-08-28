package com.fourctech.todaylist.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fourctech.todaylist.MainActivity
import com.fourctech.todaylist.core.notifications.ReminderIntents
import com.fourctech.todaylist.domain.model.Task
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class TodayGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TodayWidgetEntryPoint::class.java,
        )
        val repository = entryPoint.taskRepository()
        val initial = repository.observeTodayTasks().first()

        provideContent {
            val tasks by repository.observeTodayTasks().collectAsState(initial)
            GlanceTheme {
                TodayWidgetContent(tasks = tasks)
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 180.dp)
    }
}

@Composable
private fun TodayWidgetContent(tasks: List<Task>) {
    val size = LocalSize.current
    val isSmall = size.width < MEDIUM_BREAKPOINT
    val openApp = openAppAction()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Text(
            text = "Today",
            style = TextStyle(
                color = WidgetColors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.clickable(openApp),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        when {
            tasks.isEmpty() -> {
                Text(
                    text = "You're clear",
                    style = TextStyle(
                        color = WidgetColors.onBackground,
                        fontSize = 14.sp,
                    ),
                    modifier = GlanceModifier.clickable(openApp),
                )
            }
            isSmall -> {
                Text(
                    text = "${tasks.size} left",
                    style = TextStyle(
                        color = WidgetColors.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = GlanceModifier.clickable(openApp),
                )
            }
            else -> {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(
                        items = tasks,
                        itemId = { it.id.hashCode().toLong() },
                    ) { task ->
                        MediumTaskRow(task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediumTaskRow(task: Task) {
    val context = LocalContext.current
    val openTask = actionStartActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(ReminderIntents.taskDeepLink(task.id))).apply {
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
    )
    val complete = actionRunCallback<CompleteTodayTaskAction>(
        actionParametersOf(CompleteTodayTaskAction.TASK_ID_KEY to task.id),
    )

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "○",
            style = TextStyle(
                color = WidgetColors.primary,
                fontSize = 18.sp,
            ),
            modifier = GlanceModifier
                .padding(end = 8.dp)
                .clickable(complete),
        )
        Text(
            text = task.title,
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onBackground,
                fontSize = 14.sp,
            ),
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(openTask),
        )
    }
}

@Composable
private fun openAppAction() = actionStartActivity(
    Intent(LocalContext.current, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    },
)

private object WidgetColors {
    val background = ColorProvider(day = Color(0xFFF4F7FA), night = Color(0xFF1A2128))
    val onBackground = ColorProvider(day = Color(0xFF1A1F24), night = Color(0xFFE8EEF3))
    val primary = ColorProvider(day = Color(0xFF1B4F72), night = Color(0xFF5DADE2))
}

private val MEDIUM_BREAKPOINT = 180.dp
