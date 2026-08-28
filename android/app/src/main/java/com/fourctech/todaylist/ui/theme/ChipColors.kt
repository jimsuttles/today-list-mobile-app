package com.fourctech.todaylist.ui.theme

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable

/** Selected chips use secondary (same coral as the Today progress bar). */
@Composable
fun todayListFilterChipColors(): SelectableChipColors =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondary,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondary,
    )
