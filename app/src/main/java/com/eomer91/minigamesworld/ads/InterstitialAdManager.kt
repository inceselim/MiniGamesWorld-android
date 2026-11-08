package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.os.SystemClock
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
 * Gecis (tam ekran) reklami, Unity Ads. Bir reklam onceden yuklenir, gosterilince yenisi yuklenir.
 * Cok sik gosterim kullaniciyi kacirir; iki gosterim arasinda en az [MIN_INTERVAL_MS] beklenir.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    adsManager: AdsManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val placementId = BuildConfig.UNITY_INTERSTITIAL_ID

    private var loaded = false
    private var isLoading = false
    private var lastShownAt = 0L
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
                Log.w(TAG, "Interstitial yuklenemedi: $error $message")
                scope.launch {
                    isLoading = false
                    // Reklam yoksa bir sure sonra tekrar dene (30 sn, 60 sn, ... en fazla 5 dk)
                    if (retries < 6) { delay(30_000L * (1 shl retries.coerceAtMost(3)).coerceAtMost(10)); retries++; preload() }
                }
            }
        })
    }

    /**
     * Hazir bir reklam varsa ve bekleme suresi dolduysa gosterir.
     * [onFinished] reklam kapaninca ya da reklam gosterilmezse hemen cagrilir.
     */
    fun showIfReady(activity: Activity, onFinished: () -> Unit) {
        val cooledDown = SystemClock.elapsedRealtime() - lastShownAt >= MIN_INTERVAL_MS
        if (!loaded || !cooledDown) {
            onFinished()
            preload()
            return
        }
        loaded = false
        var finished = false
        fun finish() = scope.launch {
            if (finished) return@launch
            finished = true
            onFinished()
            preload()
        }
        UnityAds.show(activity, placementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(id: String?) {
                scope.launch { lastShownAt = SystemClock.elapsedRealtime() }
            }

            override fun onUnityAdsShowClick(id: String?) {}

            override fun onUnityAdsShowComplete(id: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                finish()
            }

            override fun onUnityAdsShowFailure(id: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                Log.w(TAG, "Interstitial gosterilemedi: $error $message")
                finish()
            }
        })
    }

    /** Baska bir tam ekran reklam (ornek: odullu) gosterildi; bekleme suresi buradan baslar. */
    fun markFullScreenAdShown() {
        lastShownAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        const val TAG = "InterstitialAdManager"

        /** Iki gecis reklami arasindaki en kisa sure. */
        const val MIN_INTERVAL_MS = 60_000L
    }
}
