package com.fourctech.todaylist.ui.later

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
import com.fourctech.todaylist.ui.components.AdBannerSlot
import com.fourctech.todaylist.ui.components.EmptyTasksState
import com.fourctech.todaylist.ui.components.TaskRow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaterScreen(
    onOpenTask: (String) -> Unit,
    viewModel: LaterViewModel = hiltViewModel(),
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
                title = { Text("Later", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
            if (state.tasks.isEmpty()) {
                EmptyTasksState(
                    title = "Nothing waiting",
                    body = "Park tasks here when they aren't for today.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
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
            AdBannerSlot(adsRemoved = state.adsRemoved)
        }
    }
}
