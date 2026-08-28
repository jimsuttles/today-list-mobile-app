package com.fourctech.todaylist.ui.later

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.ui.components.EmptyTasksState
import com.fourctech.todaylist.ui.components.TaskRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaterScreen(
    onOpenTask: (String) -> Unit,
    viewModel: LaterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Later", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
        if (state.tasks.isEmpty()) {
            EmptyTasksState(
                title = "Nothing waiting",
                body = "Park tasks here when they aren't for today.",
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp),
            ) {
                itemsIndexed(state.tasks, key = { _, task -> task.id }) { index, task ->
                    TaskRow(
                        task = task,
                        onComplete = { viewModel.completeTask(task.id) },
                        onOpen = { onOpenTask(task.id) },
                        onMove = { viewModel.moveToToday(task.id) },
                        onMoveUp = if (index > 0) ({ viewModel.moveUp(task.id) }) else null,
                        onMoveDown = if (index < state.tasks.lastIndex) {
                            ({ viewModel.moveDown(task.id) })
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
