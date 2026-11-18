package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Odullu reklam (Unity Ads): oyuncu kendi istegiyle izler, sonunda bir odul alir
 * (ornek: Block Puzzle'da parcalari degistirme, hamle geri alma).
 * Remote Config `rewarded_enabled` false ise reklam yuklenmez; [show] false doner
 * ve cagiran ekran mevcut "reklam hazir degil" davranisini surdurur.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    adsManager: AdsManager,
    private val interstitialAds: InterstitialAdManager,
    private val adConfig: AdConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val placementId = BuildConfig.UNITY_REWARDED_ID

    private var loaded = false
    private var isLoading = false
    private var adsReady = false

    init {
        scope.launch {
            adsManager.adsReady.collect { ready ->
                adsReady = ready
                if (ready) preload()
            }
        }
        scope.launch {
            adConfig.settings.collect { s ->
                if (s.rewardedEnabled) preload() else loaded = false
            }
        }
    }

    fun preload() {
        if (!adConfig.current.rewardedEnabled) return
        if (!adsReady || placementId.isBlank() || loaded || isLoading) return
        isLoading = true
        UnityAds.load(
            placementId,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    scope.launch {
                        isLoading = false
                        loaded = adConfig.current.rewardedEnabled
                    }
                }

                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    Log.w(TAG, "Rewarded failed to load: $error $message")
                    scope.launch { isLoading = false }
                }
            },
        )
    }

    /**
     * Reklam hazirsa gosterir ve true doner. Oyuncu reklami sonuna kadar izlediyse
     * reklam kapaninca [onReward] cagrilir. Reklam hazir degilse false doner.
     */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        if (!adConfig.current.rewardedEnabled) return false
        if (!loaded) {
            preload()
            return false
        }
        loaded = false

        var done = false
        UnityAds.show(
            activity,
            placementId,
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String?) {
                    // Odullu reklamdan hemen sonra bir de gecis reklami cikmasin.
                    scope.launch { interstitialAds.markFullScreenAdShown() }
                }

                override fun onUnityAdsShowClick(placementId: String?) = Unit

                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    scope.launch {
                        if (done) return@launch
                        done = true
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) onReward()
                        preload()
                    }
                }

                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    Log.w(TAG, "Rewarded failed to show: $error $message")
                    scope.launch {
                        done = true
                        preload()
                    }
                }
            },
        )
        return true
    }

    private companion object {
        const val TAG = "RewardedAdManager"
    }
}
