package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.eomer91.minigamesworld.billing.BillingManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gecis (tam ekran) reklami. Bir reklam onceden yuklenir, gosterilince yenisi yuklenir.
 * Cok sik gosterim kullaniciyi kacirir ve AdMob politikalarina takilabilir;
 * bu yuzden iki gosterim arasinda en az Remote Config `ads_frequency` saniye beklenir.
 * Remote Config `interstitial_enabled` false ise reklam yuklenmez ve gosterilmez.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    adsManager: AdsManager,
    /** Premium abonede gecis reklami gosterilmez */
    private val billing: BillingManager,
    private val adConfig: AdConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val unitId = BuildConfig.ADMOB_INTERSTITIAL_ID

    private var ad: InterstitialAd? = null
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
                if (s.interstitialEnabled) preload() else ad = null
            }
        }
    }

    fun preload() {
        if (billing.isPremium.value || !adConfig.current.interstitialEnabled) return
        if (!adsReady || unitId.isBlank() || ad != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    isLoading = false
                    // Yukleme surerken Remote Config'ten kapatilmis olabilir.
                    if (adConfig.current.interstitialEnabled) ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    isLoading = false
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
        val current = ad
        val cooledDown = lastShownAt == 0L ||
            SystemClock.elapsedRealtime() - lastShownAt >= settings.interstitialIntervalMs
        if (current == null || !cooledDown) {
            onFinished()
            preload()
            return
        }

        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                onFinished()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                onFinished()
                preload()
            }
        }
        current.show(activity)
    }

    /** Baska bir tam ekran reklam (ornek: odullu) gosterildi; bekleme suresi buradan baslar. */
    fun markFullScreenAdShown() {
        lastShownAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        const val TAG = "InterstitialAdManager"
    }
}
