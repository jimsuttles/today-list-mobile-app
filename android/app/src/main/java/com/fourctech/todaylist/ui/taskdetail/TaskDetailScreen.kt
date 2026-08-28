package com.fourctech.todaylist.ui.taskdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.domain.model.DeleteScope
import com.fourctech.todaylist.domain.model.RepeatOption
import com.fourctech.todaylist.domain.model.TaskLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.onNotificationPermissionHandled()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                TaskDetailEvent.NavigateBack,
                TaskDetailEvent.Deleted,
                -> onBack()
            }
        }
    }

    LaunchedEffect(state.needsNotificationPermission) {
        if (!state.needsNotificationPermission) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.onNotificationPermissionHandled()
            return@LaunchedEffect
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onNotificationPermissionHandled()
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (state.showDeleteDialog) {
        DeleteTaskDialog(
            isRecurring = state.isRecurring,
            onDismiss = viewModel::dismissDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }

    if (showDatePicker) {
        val initialMillis = state.reminderAt?.toEpochMilli()
            ?: LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            viewModel.onReminderDateSelected(date)
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val local = (state.reminderAt ?: Instant.now()).atZone(zone)
        val timeState = rememberTimePickerState(
            initialHour = local.hour,
            initialMinute = local.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onReminderTimeSelected(timeState.hour, timeState.minute)
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.missing && !state.loading) {
                        IconButton(onClick = viewModel::requestDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
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
            state.loading -> {
                Text(
                    text = "Loading…",
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp),
                )
            }
            state.missing -> {
                Text(
                    text = "This task is no longer available.",
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        label = { Text("Notes") },
                    )

                    Text("List", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.location == TaskLocation.TODAY,
                            onClick = { viewModel.onLocationChange(TaskLocation.TODAY) },
                            label = { Text("Today") },
                        )
                        FilterChip(
                            selected = state.location == TaskLocation.LATER,
                            onClick = { viewModel.onLocationChange(TaskLocation.LATER) },
                            label = { Text("Later") },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Reminder", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = viewModel::onReminderEnabledChange,
                        )
                    }
                    if (state.reminderEnabled) {
                        val reminderAt = state.reminderAt
                        if (reminderAt != null) {
                            val zone = ZoneId.systemDefault()
                            val zoned = reminderAt.atZone(zone)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showDatePicker = true }) {
                                    Text(
                                        zoned.toLocalDate()
                                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                    )
                                }
                                TextButton(onClick = { showTimePicker = true }) {
                                    Text(
                                        zoned.toLocalTime()
                                            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                                    )
                                }
                            }
                        }
                    }

                    Text("Repeat", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatOption.entries.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { option ->
                                    FilterChip(
                                        selected = state.repeat == option,
                                        onClick = { viewModel.onRepeatChange(option) },
                                        label = { Text(option.label()) },
                                    )
                                }
                            }
                        }
                    }

                    if (state.saving) {
                        Text(
                            text = "Saving…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteTaskDialog(
    isRecurring: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (DeleteScope) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete task?") },
        text = {
            Text(
                if (isRecurring) {
                    "This task repeats. Delete only this task, or the entire series?"
                } else {
                    "This can’t be undone."
                },
            )
        },
        confirmButton = {
            if (isRecurring) {
                TextButton(onClick = { onConfirm(DeleteScope.ENTIRE_SERIES) }) {
                    Text("Delete series")
                }
            } else {
                TextButton(onClick = { onConfirm(DeleteScope.THIS_TASK) }) {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            if (isRecurring) {
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(DeleteScope.THIS_TASK) }) {
                        Text("This task")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun RepeatOption.label(): String =
    when (this) {
        RepeatOption.NONE -> "None"
        RepeatOption.DAILY -> "Daily"
        RepeatOption.WEEKDAYS -> "Weekdays"
        RepeatOption.WEEKLY -> "Weekly"
        RepeatOption.MONTHLY -> "Monthly"
    }
