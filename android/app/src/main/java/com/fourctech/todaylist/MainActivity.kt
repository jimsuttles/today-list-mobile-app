package com.fourctech.todaylist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.domain.model.ThemeMode
import com.fourctech.todaylist.domain.repository.SettingsRepository
import com.fourctech.todaylist.domain.rollover.RolloverStub
import com.fourctech.todaylist.ui.navigation.TodayListApp
import com.fourctech.todaylist.ui.theme.TodayListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var rolloverStub: RolloverStub

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = null)
            var ready by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                settingsRepository.getSettings()
                rolloverStub.evaluateOnLaunch()
                ready = true
            }

            TodayListTheme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                if (!ready || settings == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    TodayListApp()
                }
            }
        }
    }
}
