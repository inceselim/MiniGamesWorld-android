package com.eomer91.minigamesworld.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** MainActivity tarafindan saglanir. */
val LocalInterstitialAds = staticCompositionLocalOf<InterstitialAdManager?> { null }

/**
 * Oyun ekranlarinda kullanim:
 * ```
 * val showInterstitial = rememberInterstitialLauncher()
 * Button(onClick = { showInterstitial { viewModel.restart() } })
 * ```
 */
@Composable
fun rememberInterstitialLauncher(): (onFinished: () -> Unit) -> Unit {
    val manager = LocalInterstitialAds.current
    val activity = LocalActivity.current
    return remember(manager, activity) {
        { onFinished: () -> Unit ->
            if (manager != null && activity != null) manager.showIfReady(activity, onFinished) else onFinished()
        }
    }
}
