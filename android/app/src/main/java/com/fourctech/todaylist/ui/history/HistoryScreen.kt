package com.fourctech.todaylist.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.domain.model.CompletionRecord
import com.fourctech.todaylist.ui.components.AdBannerSlot
import com.fourctech.todaylist.ui.components.EmptyTasksState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenCompletion: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("History", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
        if (state.groups.isEmpty()) {
            EmptyTasksState(
                title = "No completions yet",
                body = "Finished tasks show up here by day.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "header-${group.date}") {
                        HistoryDayHeader(date = group.date, formatter = dateFormatter)
                    }
                    items(group.items, key = { it.id }) { record ->
                        HistoryRow(
                            record = record,
                            onClick = { onOpenCompletion(record.id) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
        AdBannerSlot(adsRemoved = state.adsRemoved)
    }
}

@Composable
private fun HistoryDayHeader(
    date: LocalDate,
    formatter: DateTimeFormatter,
) {
    Text(
        text = date.format(formatter),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun HistoryRow(
    record: CompletionRecord,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "Open completed task ${record.title}",
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .semantics {
                contentDescription = "Completed: ${record.title}"
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = record.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
