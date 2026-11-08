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
 * Odullu reklam (Unity Ads): oyuncu kendi istegiyle izler, sonuna kadar izlerse reklam kapaninca odul alir
 * (ornek: Block Puzzle'da parcalari degistirme, Kulup Patronu'nda yatirimci destegi).
 */
@Singleton
class RewardedAdManager @Inject constructor(
    adsManager: AdsManager,
    private val interstitialAds: InterstitialAdManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val placementId = BuildConfig.UNITY_REWARDED_ID

    private var loaded = false
    private var isLoading = false
    private var adsReady = false
    private var retries = 0

    init {
        scope.launch {
            adsManager.adsReady.collect { ready ->
                adsReady = ready
                if (ready) preload()
            }
        }
    }

    fun preload() {
        if (!adsReady || placementId.isBlank() || loaded || isLoading) return
        isLoading = true
        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(id: String?) {
                scope.launch { loaded = true; isLoading = false; retries = 0 }
            }

            override fun onUnityAdsFailedToLoad(id: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.w(TAG, "Rewarded yuklenemedi: $error $message")
                scope.launch {
                    isLoading = false
                    if (retries < 6) { delay(30_000L * (1 shl retries.coerceAtMost(3)).coerceAtMost(10)); retries++; preload() }
                }
            }
        })
    }

    /**
     * Reklam hazirsa gosterir ve true doner. Oyuncu reklami sonuna kadar izlediyse
     * reklam kapaninca [onReward] cagrilir. Reklam hazir degilse false doner.
     */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        if (!loaded) {
            preload()
            return false
        }
        loaded = false
        UnityAds.show(activity, placementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(id: String?) {
                // Odullu reklamdan hemen sonra bir de gecis reklami cikmasin.
                scope.launch { interstitialAds.markFullScreenAdShown() }
            }

            override fun onUnityAdsShowClick(id: String?) {}

            override fun onUnityAdsShowComplete(id: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                scope.launch {
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) onReward()
                    preload()
                }
            }

            override fun onUnityAdsShowFailure(id: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                Log.w(TAG, "Rewarded gosterilemedi: $error $message")
                scope.launch { preload() }
            }
        })
        return true
    }

    private companion object {
        const val TAG = "RewardedAdManager"
    }
}
