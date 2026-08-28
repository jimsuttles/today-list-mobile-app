package com.fourctech.todaylist.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.BuildConfig
import com.fourctech.todaylist.core.ads.AdsManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.EntryPointAccessors

/**
 * Adaptive banner for free users on Today / Later / History.
 * Collapses when [adsRemoved]. In release, also collapses on load failure.
 * In debug, keeps a visible placeholder if the test ad fails so layout is easy to verify.
 */
@Composable
fun AdBannerSlot(
    adsRemoved: Boolean,
    modifier: Modifier = Modifier,
) {
    if (adsRemoved) return

    val context = LocalContext.current
    val adsManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AdsEntryPoint::class.java,
        ).adsManager()
    }
    val adsReady by adsManager.initialized.collectAsStateWithLifecycle()
    if (!adsReady) return

    var loadFailed by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val widthDp = LocalConfiguration.current.screenWidthDp

    if (loadFailed) {
        if (BuildConfig.DEBUG) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Ad slot (load failed: ${loadError ?: "unknown"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .wrapContentHeight(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp),
                )
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Banner loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Banner failed: code=${error.code} ${error.message}")
                        loadError = error.message
                        loadFailed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AdsEntryPoint {
    fun adsManager(): AdsManager
}

private const val TAG = "AdBannerSlot"
