package com.fourctech.todaylist.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.fourctech.todaylist.domain.model.TaskLocation
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    LaunchedEffect(viewModel) {
        viewModel.recreated.collectLatest { location ->
            val label = when (location) {
                TaskLocation.TODAY -> "Today"
                TaskLocation.LATER -> "Later"
            }
            snackbarHostState.showSnackbar("Added to $label")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Completed task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.missing -> {
                Text(
                    text = "This completion is no longer available.",
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp),
                )
            }
            state.record != null -> {
                val record = state.record!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(record.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Completed ${record.completionDate.format(dateFormatter)} at " +
                            record.completedAt.atZone(java.time.ZoneId.systemDefault())
                                .toLocalTime()
                                .format(timeFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Recreate as a new task",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { viewModel.recreate(TaskLocation.TODAY) },
                            enabled = !state.recreating,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Today")
                        }
                        OutlinedButton(
                            onClick = { viewModel.recreate(TaskLocation.LATER) },
                            enabled = !state.recreating,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Later")
                        }
                    }
                }
            }
        }
    }
}
