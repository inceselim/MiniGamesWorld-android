package com.eomer91.minigamesworld.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener

/** Reklam SDK'si baslatildi ve reklam istenebilir mi? MainActivity saglar. */
val LocalAdsReady = compositionLocalOf { false }

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val unitId = BuildConfig.LEVELPLAY_BANNER_ID
    if (!LocalAdsReady.current || unitId.isBlank()) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val size = LevelPlayAdSize.createAdaptiveAdSize(ctx) ?: LevelPlayAdSize.BANNER
            val config = LevelPlayBannerAdView.Config.Builder().setAdSize(size).build()
            LevelPlayBannerAdView(ctx, unitId, config).apply {
                setBannerListener(object : LevelPlayBannerAdViewListener {
                    override fun onAdLoaded(adInfo: LevelPlayAdInfo) = Unit
                    override fun onAdLoadFailed(error: LevelPlayAdError) {
                        Log.w("AdBanner", "Banner failed to load: $error")
                    }
                })
                loadAd()
            }
        },
        onRelease = { it.destroy() },
    )
}
