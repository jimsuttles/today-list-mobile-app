package com.fourctech.todaylist.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fourctech.todaylist.ui.components.EmptyTasksState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            modifier = Modifier.align(Alignment.TopStart),
        )
        EmptyTasksState(
            title = "Settings come later",
            body = "Theme, rollover, and week start land in Phase 8.",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 56.dp),
        )
    }
}
