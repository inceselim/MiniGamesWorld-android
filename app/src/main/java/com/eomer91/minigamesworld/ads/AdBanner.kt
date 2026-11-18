package com.eomer91.minigamesworld.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.eomer91.minigamesworld.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** AdMob baslatildi ve reklam istenebilir mi? MainActivity saglar. */
val LocalAdsReady = compositionLocalOf { false }

/** Remote Config `banner_enabled`. MainActivity saglar. */
val LocalBannerEnabled = compositionLocalOf { AdSettings.DEFAULT_BANNER_ENABLED }

/** Reklamsiz Premium abone mi? MainActivity saglar. */
val LocalPremium = compositionLocalOf { false }

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val unitId = BuildConfig.ADMOB_BANNER_ID
    if (!LocalAdsReady.current || !LocalBannerEnabled.current || unitId.isBlank() || LocalPremium.current) return

    val widthDp = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                adUnitId = unitId
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
