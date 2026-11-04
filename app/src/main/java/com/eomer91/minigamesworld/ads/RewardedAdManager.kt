package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.rewarded.LevelPlayReward
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Odullu reklam (Unity LevelPlay): oyuncu kendi istegiyle izler, sonunda bir odul alir
 * (ornek: Block Puzzle'da parcalari degistirme, hamle geri alma).
 *
 * LevelPlay'de odul ([LevelPlayRewardedAdListener.onAdRewarded]) kapanmadan once ya da sonra
 * gelebilir; odul, ikisi de gelince (reklam kapaninca) tek sefer verilir.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    adsManager: AdsManager,
    private val interstitialAds: InterstitialAdManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val unitId = BuildConfig.LEVELPLAY_REWARDED_ID

    private var ad: LevelPlayRewardedAd? = null
    private var isLoading = false
    private var failedLoads = 0

    // O anki gosterim
    private var pendingReward: (() -> Unit)? = null
    private var earned = false
    private var closed = false

    init {
        scope.launch {
            adsManager.adsReady.collect { ready ->
                if (ready && ad == null && unitId.isNotBlank()) {
                    ad = LevelPlayRewardedAd(unitId).apply { setListener(listener) }
                    preload()
                }
            }
        }
    }

    private val listener = object : LevelPlayRewardedAdListener {
        override fun onAdLoaded(levelPlayAdInfo: LevelPlayAdInfo) {
            isLoading = false
            failedLoads = 0
        }

        override fun onAdLoadFailed(levelPlayAdError: LevelPlayAdError) {
            Log.w(TAG, "Rewarded failed to load: $levelPlayAdError")
            isLoading = false
            failedLoads++
            scope.launch {
                delay(RETRY_BASE_MS * failedLoads.coerceAtMost(6))
                preload()
            }
        }

        override fun onAdDisplayed(levelPlayAdInfo: LevelPlayAdInfo) {
            // Odullu reklamdan hemen sonra bir de gecis reklami cikmasin.
            interstitialAds.markFullScreenAdShown()
        }

        override fun onAdRewarded(reward: LevelPlayReward, adInfo: LevelPlayAdInfo) {
            earned = true
            deliverIfDone()
        }

        override fun onAdDisplayFailed(levelPlayAdError: LevelPlayAdError, levelPlayAdInfo: LevelPlayAdInfo) {
            Log.w(TAG, "Rewarded failed to show: $levelPlayAdError")
            pendingReward = null
            preload()
        }

        override fun onAdClicked(levelPlayAdInfo: LevelPlayAdInfo) = Unit

        override fun onAdClosed(levelPlayAdInfo: LevelPlayAdInfo) {
            closed = true
            deliverIfDone()
            // Odul gelmeden kapandiysa (izlemeyi yarida biraktiysa) biraz bekle, gec gelebilir.
            val shownFor = pendingReward
            scope.launch {
                delay(LATE_REWARD_WAIT_MS)
                if (pendingReward === shownFor) pendingReward = null
            }
            preload()
        }

        override fun onAdInfoChanged(levelPlayAdInfo: LevelPlayAdInfo) = Unit
    }

    private fun deliverIfDone() {
        if (!earned || !closed) return
        val cb = pendingReward ?: return
        pendingReward = null
        cb()
    }

    fun preload() {
        val current = ad ?: return
        if (isLoading || current.isAdReady()) return
        isLoading = true
        current.loadAd()
    }

    /**
     * Reklam hazirsa gosterir ve true doner. Oyuncu reklami sonuna kadar izlediyse
     * reklam kapaninca [onReward] cagrilir. Reklam hazir degilse false doner.
     */
    fun show(activity: Activity, onReward: () -> Unit): Boolean {
        val current = ad
        if (current == null || !current.isAdReady() || pendingReward != null) {
            preload()
            return false
        }
        pendingReward = onReward
        earned = false
        closed = false
        current.showAd(activity)
        return true
    }

    private companion object {
        const val TAG = "RewardedAdManager"
        const val RETRY_BASE_MS = 10_000L
        const val LATE_REWARD_WAIT_MS = 3_000L
    }
}
