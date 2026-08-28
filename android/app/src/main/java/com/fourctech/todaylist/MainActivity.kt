package com.fourctech.todaylist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.core.analytics.Analytics
import com.fourctech.todaylist.core.analytics.AnalyticsEvents
import com.fourctech.todaylist.core.notifications.ReminderIntents
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.ui.navigation.TodayListApp
import com.fourctech.todaylist.ui.rollover.RolloverReviewDialog
import com.fourctech.todaylist.ui.rollover.RolloverViewModel
import com.fourctech.todaylist.ui.theme.TodayListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var analytics: Analytics

    private val rolloverViewModel: RolloverViewModel by viewModels()
    private val deepLinkTaskIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkTaskIdState.value = taskIdFromIntent(intent)
        analytics.log(AnalyticsEvents.APP_OPEN)
        enableEdgeToEdge()

        // Start Mobile Ads after consent; banner waits for AdsManager.initialized.
        com.fourctech.todaylist.core.ads.AdsConsent.gather(this) {
            (application as TodayListApplication).initializeAdsIfNeeded()
        }
        // Also kick off init if consent no-ops quickly / already determined.
        // Safe to call twice — AdsManager dedupes.
        (application as TodayListApplication).initializeAdsIfNeeded()

        setContent {
            val settings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = null)
            val review by rolloverViewModel.review.collectAsStateWithLifecycle()
            var settingsReady by remember { mutableStateOf(false) }
            val deepLinkTaskId by deepLinkTaskIdState
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        settingsReady = true
                        rolloverViewModel.onForeground()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            TodayListTheme(
                themeMode = settings?.themeMode ?: ThemeMode.SYSTEM,
                hapticsEnabled = settings?.hapticsEnabled ?: true,
            ) {
                if (!settingsReady || settings == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TodayListApp(
                            deepLinkTaskId = deepLinkTaskId,
                            onDeepLinkConsumed = { deepLinkTaskIdState.value = null },
                        )
                        review?.let { state ->
                            RolloverReviewDialog(
                                state = state,
                                onSetDecision = rolloverViewModel::setDecision,
                                onKeepAllToday = rolloverViewModel::keepAllToday,
                                onMoveAllLater = rolloverViewModel::moveAllLater,
                                onConfirm = rolloverViewModel::confirm,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTaskIdState.value = taskIdFromIntent(intent)
    }

    companion object {
        fun taskIdFromIntent(intent: Intent?): String? {
            if (intent == null) return null
            intent.getStringExtra(ReminderIntents.EXTRA_TASK_ID)?.let { return it }
            val data: Uri = intent.data ?: return null
            if (data.scheme != ReminderIntents.SCHEME || data.host != ReminderIntents.HOST_TASK) {
                return null
            }
            return data.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
        }
    }
}
