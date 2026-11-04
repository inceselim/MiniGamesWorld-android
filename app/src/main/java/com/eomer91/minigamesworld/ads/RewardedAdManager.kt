package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Odullu reklam (Unity Ads): oyuncu kendi istegiyle izler, sonunda bir odul alir
 * (ornek: Block Puzzle'da parcalari degistirme, hamle geri alma).
 * Odul, reklam sonuna kadar izlenince (COMPLETED) reklam kapanirken verilir.
 */
@Suppress("DEPRECATION") // Eski Unity Ads API'si (5.0'a kadar destekli); yenisi henuz deneysel.
@Singleton
class RewardedAdManager @Inject constructor(
    adsManager: AdsManager,
    private val interstitialAds: InterstitialAdManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val placementId = BuildConfig.UNITY_REWARDED_ID

    private var adsReady = false
    private var loaded = false
    private var isLoading = false
    private var failedLoads = 0
    private var pendingReward: (() -> Unit)? = null

    init {
        scope.launch {
            adsManager.adsReady.collect { ready ->
                adsReady = ready
                if (ready) preload()
            }
        }
    }

    private val loadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(placementId: String?) {
            scope.launch {
                loaded = true
                isLoading = false
                failedLoads = 0
            }
        }

        override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
            Log.w(TAG, "Rewarded failed to load: $error $message")
            scope.launch {
                isLoading = false
                failedLoads++
                delay(RETRY_BASE_MS * failedLoads.coerceAtMost(6))
                preload()
            }
        }
    }

    private val showListener = object : IUnityAdsShowListener {
        override fun onUnityAdsShowStart(placementId: String?) {
            // Odullu reklamdan hemen sonra bir de gecis reklami cikmasin.
            scope.launch { interstitialAds.markFullScreenAdShown() }
        }

        override fun onUnityAdsShowClick(placementId: String?) = Unit

        override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
            scope.launch {
                val cb = pendingReward
                pendingReward = null
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) cb?.invoke()
                preload()
            }
        }

        override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
            Log.w(TAG, "Rewarded failed to show: $error $message")
            scope.launch {
                pendingReward = null
                preload()
            }
        }
    }

    fun preload() {
        if (!adsReady || placementId.isBlank() || loaded || isLoading) return
        isLoading = true
        UnityAds.load(placementId, loadListener)
    }

    /**
     * Reklam hazirsa gosterir ve true doner. Oyuncu reklami sonuna kadar izlediyse
     * reklam kapaninca [onReward] cagrilir. Reklam hazir degilse false doner.
     */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        if (!loaded || pendingReward != null) {
            preload()
            return false
        }
        loaded = false
        pendingReward = onReward
        UnityAds.show(activity, placementId, UnityAdsShowOptions(), showListener)
        return true
    }

    private companion object {
        const val TAG = "RewardedAdManager"
        const val RETRY_BASE_MS = 10_000L
    }
}
