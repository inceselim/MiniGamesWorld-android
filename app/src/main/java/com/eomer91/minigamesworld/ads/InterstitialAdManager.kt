package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.eomer91.minigamesworld.billing.BillingManager
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
 * Gecis (tam ekran) reklami, Unity Ads. Bir reklam onceden yuklenir, gosterilince yenisi yuklenir.
 * Cok sik gosterim kullaniciyi kacirir; bu yuzden iki gosterim arasinda en az
 * Remote Config `ads_frequency` saniye beklenir.
 * Remote Config `interstitial_enabled` false ise reklam yuklenmez ve gosterilmez.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    adsManager: AdsManager,
    /** Premium abonede gecis reklami gosterilmez */
    private val billing: BillingManager,
    private val adConfig: AdConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val placementId = BuildConfig.UNITY_INTERSTITIAL_ID

    /** Gosterime hazir bir reklam var mi (Unity Ads reklami kendi icinde tutar). */
    private var loaded = false
    private var isLoading = false
    private var lastShownAt = 0L
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
                if (s.interstitialEnabled) preload() else loaded = false
            }
        }
    }

    fun preload() {
        if (billing.isPremium.value || !adConfig.current.interstitialEnabled) return
        if (!adsReady || placementId.isBlank() || loaded || isLoading) return
        isLoading = true
        UnityAds.load(
            placementId,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    scope.launch {
                        isLoading = false
                        // Yukleme surerken Remote Config'ten kapatilmis olabilir.
                        loaded = adConfig.current.interstitialEnabled
                    }
                }

                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    Log.w(TAG, "Interstitial failed to load: $error $message")
                    scope.launch { isLoading = false }
                }
            },
        )
    }

    /**
     * Hazir bir reklam varsa ve bekleme suresi dolduysa gosterir.
     * [onFinished] reklam kapaninca ya da reklam gosterilmezse hemen cagrilir.
     */
    fun showIfReady(activity: Activity, onFinished: () -> Unit) {
        val settings = adConfig.current
        if (billing.isPremium.value || !settings.interstitialEnabled) { onFinished(); return }
        val cooledDown = lastShownAt == 0L ||
            SystemClock.elapsedRealtime() - lastShownAt >= settings.interstitialIntervalMs
        if (!loaded || !cooledDown) {
            onFinished()
            preload()
            return
        }

        loaded = false
        var finished = false
        val finish = {
            scope.launch {
                if (!finished) {
                    finished = true
                    onFinished()
                    preload()
                }
            }
        }
        UnityAds.show(
            activity,
            placementId,
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String?) {
                    scope.launch { lastShownAt = SystemClock.elapsedRealtime() }
                }

                override fun onUnityAdsShowClick(placementId: String?) = Unit

                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    finish()
                }

                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    Log.w(TAG, "Interstitial failed to show: $error $message")
                    finish()
                }
            },
        )
    }

    /** Baska bir tam ekran reklam (ornek: odullu) gosterildi; bekleme suresi buradan baslar. */
    fun markFullScreenAdShown() {
        lastShownAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        const val TAG = "InterstitialAdManager"
    }
}
