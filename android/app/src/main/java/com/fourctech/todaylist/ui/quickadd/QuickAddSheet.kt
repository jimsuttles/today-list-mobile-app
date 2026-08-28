package com.fourctech.todaylist.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.domain.model.TaskLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    defaultLocation: TaskLocation,
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(defaultLocation) {
        viewModel.reset(defaultLocation)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Quick add", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Task") },
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { { Text(it) } },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.location == TaskLocation.TODAY,
                    onClick = { viewModel.onLocationChange(TaskLocation.TODAY) },
                    label = { Text("Today") },
                    modifier = Modifier.semantics {
                        contentDescription = if (state.location == TaskLocation.TODAY) {
                            "List Today, selected"
                        } else {
                            "List Today"
                        }
                    },
                )
                FilterChip(
                    selected = state.location == TaskLocation.LATER,
                    onClick = { viewModel.onLocationChange(TaskLocation.LATER) },
                    label = { Text("Later") },
                    modifier = Modifier.semantics {
                        contentDescription = if (state.location == TaskLocation.LATER) {
                            "List Later, selected"
                        } else {
                            "List Later"
                        }
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { viewModel.save(onSaved = onDismiss) },
                    enabled = !state.isSaving,
                ) {
                    Text("Add")
                }
            }
        }
    }
}
