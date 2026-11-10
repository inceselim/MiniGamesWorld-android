package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Odullu reklam: oyuncu kendi istegiyle izler, sonunda bir odul alir
 * (ornek: Block Puzzle'da parcalari degistirme, hamle geri alma).
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    adsManager: AdsManager,
    private val interstitialAds: InterstitialAdManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val unitId = BuildConfig.ADMOB_REWARDED_ID

    private var ad: RewardedAd? = null
    private var isLoading = false
    private var adsReady = false

    init {
        scope.launch {
            adsManager.adsReady.collect { ready ->
                adsReady = ready
                if (ready) preload()
            }
        }
    }

    fun preload() {
        if (!adsReady || unitId.isBlank() || ad != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.message}")
                    isLoading = false
                }
            },
        )
    }

    /**
     * Reklam hazirsa gosterir ve true doner. Oyuncu reklami sonuna kadar izlediyse
     * reklam kapaninca [onReward] cagrilir. Reklam hazir degilse false doner.
     */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        val current = ad
        if (current == null) {
            preload()
            return false
        }
        ad = null

        var earned = false
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Odullu reklamdan hemen sonra bir de gecis reklami cikmasin.
                interstitialAds.markFullScreenAdShown()
            }

            override fun onAdDismissedFullScreenContent() {
                if (earned) onReward()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${error.message}")
                preload()
            }
        }
        current.show(activity) { earned = true }
        return true
    }

    private companion object {
        const val TAG = "RewardedAdManager"
    }
}
