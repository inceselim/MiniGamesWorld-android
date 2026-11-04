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
@Suppress("DEPRECATION") // Eski Unity Ads API'si (5.0'a kadar destekli); yenisi henuz deneysel.
@Singleton
class InterstitialAdManager @Inject constructor(
    adsManager: AdsManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val placementId = BuildConfig.UNITY_INTERSTITIAL_ID

    private var adsReady = false
    private var loaded = false
    private var isLoading = false
    private var lastShownAt = 0L
    private var failedLoads = 0

    /** O an gosterilen reklam kapaninca cagrilacak. */
    private var pendingFinish: (() -> Unit)? = null

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
            Log.w(TAG, "Interstitial failed to load: $error $message")
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
            scope.launch { lastShownAt = SystemClock.elapsedRealtime() }
        }

        override fun onUnityAdsShowClick(placementId: String?) = Unit

        override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
            scope.launch { finish() }
        }

        override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
            Log.w(TAG, "Interstitial failed to show: $error $message")
            scope.launch { finish() }
        }
    }

    private fun finish() {
        val cb = pendingFinish
        pendingFinish = null
        cb?.invoke()
        preload()
    }

    fun preload() {
        if (!adsReady || placementId.isBlank() || loaded || isLoading) return
        isLoading = true
        UnityAds.load(placementId, loadListener)
    }

    /**
     * Hazir bir reklam varsa ve bekleme suresi dolduysa gosterir.
     * [onFinished] reklam kapaninca ya da reklam gosterilmezse hemen cagrilir.
     */
    fun showIfReady(activity: Activity, onFinished: () -> Unit) {
        val cooledDown = SystemClock.elapsedRealtime() - lastShownAt >= MIN_INTERVAL_MS
        if (!loaded || !cooledDown || pendingFinish != null) {
            onFinished()
            preload()
            return
        }
        loaded = false // Unity'de yuklu reklam bir kez gosterilir.
        pendingFinish = onFinished
        UnityAds.show(activity, placementId, UnityAdsShowOptions(), showListener)
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
