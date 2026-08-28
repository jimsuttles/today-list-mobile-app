package com.fourctech.todaylist.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.ui.components.EmptyTasksState
import com.fourctech.todaylist.ui.components.TaskRow
import com.fourctech.todaylist.ui.components.TodayProgressHeader
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenTask: (String) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.undoEvents.collectLatest { result ->
            val snackResult = snackbarHostState.showSnackbar(
                message = "Completed “${result.title}”",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
                withDismissAction = true,
            )
            if (snackResult == SnackbarResult.ActionPerformed) {
                viewModel.undoComplete(result)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TopAppBar(
                title = { Text("Today", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
            TodayProgressHeader(
                completedCount = state.sessionCompletedCount,
                totalCount = state.tasks.size + state.sessionCompletedCount,
            )
            if (state.tasks.isEmpty()) {
                EmptyTasksState(
                    title = "You're clear",
                    body = "Add something for today, or pull a task in from Later.",
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
                            onMove = { viewModel.moveToLater(task.id) },
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
}
