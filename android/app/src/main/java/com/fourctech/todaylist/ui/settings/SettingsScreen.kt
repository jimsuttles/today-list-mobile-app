package com.fourctech.todaylist.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.BuildConfig
import com.fourctech.todaylist.R
import com.fourctech.todaylist.domain.model.AppSettings
import com.fourctech.todaylist.domain.model.RolloverMode
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.model.WeekStart
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onRemoveAds: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    var showClearHistory by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)
    val feedbackEmail = stringResource(R.string.feedback_email)

    LaunchedEffect(toastMessage) {
        val message = toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeToast()
    }
    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "That page couldn't be opened.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Couldn't open system settings.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(
                Intent.EXTRA_TEXT,
                "\n\n---\nApp version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No email app available.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("Clear all history?") },
            text = { Text("Completed task history will be permanently removed. Active tasks stay.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistory = false
                        viewModel.clearHistory()
                    },
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete all local app data?") },
            text = {
                Text(
                    "Today, Later, History, and reminders will be permanently removed. " +
                        "A Remove Ads purchase will not be affected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAll = false
                        viewModel.deleteAllData()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
        SettingsContent(
            state = state,
            onThemeChange = viewModel::setThemeMode,
            onRolloverChange = viewModel::setRolloverMode,
            onWeekStartChange = viewModel::setWeekStart,
            onHapticsChange = viewModel::setHapticsEnabled,
            onOpenNotifications = ::openAppNotificationSettings,
            onRemoveAds = onRemoveAds,
            onRestorePurchases = viewModel::restorePurchases,
            onClearHistory = { showClearHistory = true },
            onDeleteAll = { showDeleteAll = true },
            onFeedback = { sendEmail("Today List Feedback") },
            onReportProblem = { sendEmail("Today List Problem Report") },
            onPrivacy = { openUrl(privacyUrl) },
            onTerms = { openUrl(termsUrl) },
        )
    }
}

@Composable
private fun SettingsContent(
    state: AppSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onRolloverChange: (RolloverMode) -> Unit,
    onWeekStartChange: (WeekStart) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestorePurchases: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteAll: () -> Unit,
    onFeedback: () -> Unit,
    onReportProblem: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionHeader("APPEARANCE")
        ThemeMode.entries.forEach { mode ->
            SettingsRadioRow(
                label = mode.displayLabel(),
                selected = state.themeMode == mode,
                onClick = { onThemeChange(mode) },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("UNFINISHED TASKS")
        Text(
            text = "When a new day starts with unfinished Today tasks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        RolloverMode.entries.forEach { mode ->
            SettingsRadioRow(
                label = mode.displayLabel(),
                selected = state.rolloverMode == mode,
                onClick = { onRolloverChange(mode) },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("NOTIFICATIONS")
        SettingsNavRow(label = "System notification settings", onClick = onOpenNotifications)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("WEEK STARTS")
        WeekStart.entries.forEach { start ->
            SettingsRadioRow(
                label = start.displayLabel(),
                selected = state.weekStart == start,
                onClick = { onWeekStartChange(start) },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("EXPERIENCE")
        SettingsSwitchRow(
            label = "Haptics",
            checked = state.hapticsEnabled,
            onCheckedChange = onHapticsChange,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("PREMIUM")
        if (state.adsRemovedCached) {
            Text(
                text = "Ads removed",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            SettingsNavRow(label = "Remove Ads", onClick = onRemoveAds)
        }
        SettingsNavRow(label = "Restore Purchases", onClick = onRestorePurchases)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("DATA")
        SettingsNavRow(
            label = "Clear History",
            onClick = onClearHistory,
            destructive = true,
        )
        SettingsNavRow(
            label = "Delete All App Data",
            onClick = onDeleteAll,
            destructive = true,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("SUPPORT")
        SettingsNavRow(label = "Send Feedback", onClick = onFeedback)
        SettingsNavRow(label = "Report a Problem", onClick = onReportProblem)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("LEGAL")
        SettingsNavRow(label = "Privacy Policy", onClick = onPrivacy)
        SettingsNavRow(label = "Terms of Use", onClick = onTerms)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("ABOUT")
        Text(
            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "© ${Calendar.getInstance().get(Calendar.YEAR)} ${BuildConfig.PUBLISHER_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavRow(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (destructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = label,
            )
            .padding(vertical = 12.dp),
    )
}

private fun ThemeMode.displayLabel(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun RolloverMode.displayLabel(): String = when (this) {
    RolloverMode.ASK -> "Ask each day"
    RolloverMode.AUTO_TODAY -> "Keep on Today"
    RolloverMode.AUTO_LATER -> "Move to Later"
}

private fun WeekStart.displayLabel(): String = when (this) {
    WeekStart.SUNDAY -> "Sunday"
    WeekStart.MONDAY -> "Monday"
}
