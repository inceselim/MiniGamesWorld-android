package com.eomer91.minigamesworld.ads

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/** Reklam SDK'si baslatildi ve reklam istenebilir mi? MainActivity saglar. */
val LocalAdsReady = compositionLocalOf { false }

@Suppress("DEPRECATION") // Eski Unity Ads banner API'si (5.0'a kadar destekli).
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val placementId = BuildConfig.UNITY_BANNER_ID
    val activity = LocalActivity.current
    if (!LocalAdsReady.current || placementId.isBlank() || activity == null) return

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                BannerView(activity, placementId, UnityBannerSize(320, 50)).apply {
                    listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerAdView: BannerView?) = Unit
                        override fun onBannerShown(bannerAdView: BannerView?) = Unit
                        override fun onBannerClick(bannerAdView: BannerView?) = Unit
                        override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                            Log.w("AdBanner", "Banner failed to load: $errorInfo")
                        }
                        override fun onBannerLeftApplication(bannerView: BannerView?) = Unit
                    }
                    load()
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
