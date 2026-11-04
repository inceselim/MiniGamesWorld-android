package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gecis (tam ekran) reklami, Unity LevelPlay uzerinden (Unity Ads + AdMob yarisir).
 * Tek bir reklam nesnesi tutulur; gosterildikten sonra ayni nesneyle yenisi yuklenir.
 * Cok sik gosterim kullaniciyi kacirir; iki gosterim arasinda en az [MIN_INTERVAL_MS] beklenir.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    adsManager: AdsManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val unitId = BuildConfig.LEVELPLAY_INTERSTITIAL_ID

    private var ad: LevelPlayInterstitialAd? = null
    private var isLoading = false
    private var lastShownAt = 0L
    private var failedLoads = 0

    /** O an gosterilen reklam kapaninca cagrilacak. */
    private var pendingFinish: (() -> Unit)? = null

    init {
        scope.launch {
            adsManager.adsReady.collect { ready ->
                if (ready && ad == null && unitId.isNotBlank()) {
                    ad = LevelPlayInterstitialAd(unitId).apply { setListener(listener) }
                    preload()
                }
            }
        }
    }

    private val listener = object : LevelPlayInterstitialAdListener {
        override fun onAdLoaded(levelPlayAdInfo: LevelPlayAdInfo) {
            isLoading = false
            failedLoads = 0
        }

        override fun onAdLoadFailed(levelPlayAdError: LevelPlayAdError) {
            Log.w(TAG, "Interstitial failed to load: $levelPlayAdError")
            isLoading = false
            retryLater()
        }

        override fun onAdDisplayed(levelPlayAdInfo: LevelPlayAdInfo) {
            lastShownAt = SystemClock.elapsedRealtime()
        }

        override fun onAdDisplayFailed(levelPlayAdError: LevelPlayAdError, levelPlayAdInfo: LevelPlayAdInfo) {
            Log.w(TAG, "Interstitial failed to show: $levelPlayAdError")
            finish()
        }

        override fun onAdClicked(levelPlayAdInfo: LevelPlayAdInfo) = Unit

        override fun onAdClosed(levelPlayAdInfo: LevelPlayAdInfo) = finish()

        override fun onAdInfoChanged(levelPlayAdInfo: LevelPlayAdInfo) = Unit
    }

    private fun finish() {
        val cb = pendingFinish
        pendingFinish = null
        cb?.invoke()
        preload()
    }

    /** Yukleme basarisizsa artan araliklarla tekrar dene (surekli istek atmamak icin). */
    private fun retryLater() {
        failedLoads++
        scope.launch {
            delay(RETRY_BASE_MS * failedLoads.coerceAtMost(6))
            preload()
        }
    }

    fun preload() {
        val current = ad ?: return
        if (isLoading || current.isAdReady()) return
        isLoading = true
        current.loadAd()
    }

    /**
     * Hazir bir reklam varsa ve bekleme suresi dolduysa gosterir.
     * [onFinished] reklam kapaninca ya da reklam gosterilmezse hemen cagrilir.
     */
    fun showIfReady(activity: Activity, onFinished: () -> Unit) {
        val current = ad
        val cooledDown = SystemClock.elapsedRealtime() - lastShownAt >= MIN_INTERVAL_MS
        if (current == null || !cooledDown || pendingFinish != null || !current.isAdReady()) {
            onFinished()
            preload()
            return
        }
        pendingFinish = onFinished
        current.showAd(activity)
    }

    /** Baska bir tam ekran reklam (ornek: odullu) gosterildi; bekleme suresi buradan baslar. */
    fun markFullScreenAdShown() {
        lastShownAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        const val TAG = "InterstitialAdManager"

        /** Iki gecis reklami arasindaki en kisa sure. */
        const val MIN_INTERVAL_MS = 60_000L
        const val RETRY_BASE_MS = 10_000L
    }
}
