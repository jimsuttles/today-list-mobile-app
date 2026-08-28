package com.fourctech.todaylist.ui.rollover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fourctech.todaylist.domain.model.Task
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.ui.theme.todayListFilterChipColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolloverReviewDialog(
    state: RolloverReviewUiState,
    onSetDecision: (String, TaskLocation) -> Unit,
    onKeepAllToday: () -> Unit,
    onMoveAllLater: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Unfinished tasks") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                Text(
                    text = reviewSubtitle(state.missedDays, state.tasks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onKeepAllToday) { Text("Keep all Today") }
                    TextButton(onClick = onMoveAllLater) { Text("Move all Later") }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        RolloverTaskRow(
                            task = task,
                            decision = state.decisions[task.id] ?: TaskLocation.TODAY,
                            onDecision = { location -> onSetDecision(task.id, location) },
                        )
                    }
                }
                Button(
                    onClick = onConfirm,
                    enabled = !state.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun RolloverTaskRow(
    task: Task,
    decision: TaskLocation,
    onDecision: (TaskLocation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = decision == TaskLocation.TODAY,
                onClick = { onDecision(TaskLocation.TODAY) },
                label = { Text("Today") },
                colors = todayListFilterChipColors(),
            )
            FilterChip(
                selected = decision == TaskLocation.LATER,
                onClick = { onDecision(TaskLocation.LATER) },
                label = { Text("Later") },
                colors = todayListFilterChipColors(),
            )
        }
    }
}

private fun reviewSubtitle(missedDays: Long, count: Int): String {
    val gap = if (missedDays > 1) {
        "After $missedDays days away, "
    } else {
        ""
    }
    return "${gap}choose where each unfinished task should go ($count)."
}
