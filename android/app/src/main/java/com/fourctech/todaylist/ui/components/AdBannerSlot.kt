package com.fourctech.todaylist.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.fourctech.todaylist.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Adaptive banner for free users on Today / Later / History.
 * Collapses when [adsRemoved] or when the ad fails to load.
 */
@Composable
fun AdBannerSlot(
    adsRemoved: Boolean,
    modifier: Modifier = Modifier,
) {
    if (adsRemoved) return

    var loadFailed by remember { mutableStateOf(false) }
    if (loadFailed) return

    val widthDp = LocalConfiguration.current.screenWidthDp

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp),
                )
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadFailed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
