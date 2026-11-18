package com.eomer91.minigamesworld.ads

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/** Reklam SDK'si (Unity Ads) baslatildi ve reklam istenebilir mi? MainActivity saglar. */
val LocalAdsReady = compositionLocalOf { false }

/** Remote Config `banner_enabled`. MainActivity saglar. */
val LocalBannerEnabled = compositionLocalOf { AdSettings.DEFAULT_BANNER_ENABLED }

/** Reklamsiz Premium abone mi? MainActivity saglar. */
val LocalPremium = compositionLocalOf { false }

private const val BANNER_W = 320
private const val BANNER_H = 50

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val placementId = BuildConfig.UNITY_BANNER_ID
    val activity = LocalActivity.current ?: return
    if (!LocalAdsReady.current || !LocalBannerEnabled.current || placementId.isBlank() || LocalPremium.current) return

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.size(BANNER_W.dp, BANNER_H.dp),
            factory = {
                BannerView(activity, placementId, UnityBannerSize(BANNER_W, BANNER_H)).apply {
                    listener = object : BannerView.Listener() {
                        override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                            Log.w("AdBanner", "Banner failed to load: ${errorInfo?.errorMessage}")
                        }
                    }
                    load()
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
