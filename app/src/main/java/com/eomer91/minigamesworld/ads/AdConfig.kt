package com.eomer91.minigamesworld.ads

import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Remote Config'ten gelen reklam ayarlari. */
data class AdSettings(
    val interstitialEnabled: Boolean = DEFAULT_INTERSTITIAL_ENABLED,
    val rewardedEnabled: Boolean = DEFAULT_REWARDED_ENABLED,
    val bannerEnabled: Boolean = DEFAULT_BANNER_ENABLED,
    /** Iki gecis reklami arasindaki en kisa sure (saniye). */
    val adsFrequencySeconds: Long = DEFAULT_ADS_FREQUENCY_SECONDS,
) {
    val interstitialIntervalMs: Long
        get() = adsFrequencySeconds.coerceIn(MIN_FREQUENCY_SECONDS, MAX_FREQUENCY_SECONDS) * 1000L

    companion object {
        const val DEFAULT_INTERSTITIAL_ENABLED = true
        const val DEFAULT_REWARDED_ENABLED = true
        const val DEFAULT_BANNER_ENABLED = true
        const val DEFAULT_ADS_FREQUENCY_SECONDS = 80L

        /** AdMob politikalarina takilmamak icin Remote Config'ten gelse bile alt sinir. */
        const val MIN_FREQUENCY_SECONDS = 30L
        const val MAX_FREQUENCY_SECONDS = 3600L
    }
}

/**
 * Firebase Remote Config ile reklam acma/kapama ve gecis reklami sikligi.
 *
 * - Uygulama ilk acildiginda (ya da Firebase henuz yapilandirilmamissa) [AdSettings] varsayilanlari kullanilir.
 * - Arka planda fetchAndActivate yapilir; yeni degerler gelince [settings] guncellenir.
 * - Gercek zamanli guncelleme dinlenir: Console'dan yayinlanan degisiklik acik uygulamaya da ulasir.
 * - google-services.json yoksa Firebase baslatilamaz; bu durumda cokmeden varsayilanlarla devam edilir.
 */
@Singleton
class AdConfig @Inject constructor() {

    private val _settings = MutableStateFlow(AdSettings())
    val settings: StateFlow<AdSettings> = _settings.asStateFlow()

    val current: AdSettings get() = _settings.value

    private val remoteConfig: FirebaseRemoteConfig? = try {
        FirebaseRemoteConfig.getInstance()
    } catch (e: IllegalStateException) {
        Log.w(TAG, "Firebase not configured (google-services.json missing?), using default ad settings")
        null
    }

    init {
        remoteConfig?.let { rc ->
            rc.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else FETCH_INTERVAL_SECONDS
                },
            )
            rc.setDefaultsAsync(
                mapOf(
                    KEY_INTERSTITIAL to AdSettings.DEFAULT_INTERSTITIAL_ENABLED,
                    KEY_REWARDED to AdSettings.DEFAULT_REWARDED_ENABLED,
                    KEY_BANNER to AdSettings.DEFAULT_BANNER_ENABLED,
                    KEY_FREQUENCY to AdSettings.DEFAULT_ADS_FREQUENCY_SECONDS,
                ),
            ).addOnCompleteListener {
                fetch(rc)
                listenForUpdates(rc)
            }
        }
    }

    private fun fetch(rc: FirebaseRemoteConfig) {
        rc.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) Log.w(TAG, "Remote Config fetch failed", task.exception)
            // Basarisiz olsa da onceden etkinlestirilmis (ya da varsayilan) degerleri yayinla.
            publish(rc)
        }
    }

    private fun listenForUpdates(rc: FirebaseRemoteConfig) {
        try {
            rc.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    rc.activate().addOnCompleteListener { publish(rc) }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Remote Config realtime update error: ${error.code}", error)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config realtime listener unavailable", e)
        }
    }

    private fun publish(rc: FirebaseRemoteConfig) {
        val s = AdSettings(
            interstitialEnabled = rc.getBoolean(KEY_INTERSTITIAL),
            rewardedEnabled = rc.getBoolean(KEY_REWARDED),
            bannerEnabled = rc.getBoolean(KEY_BANNER),
            adsFrequencySeconds = rc.getLong(KEY_FREQUENCY),
        )
        Log.d(TAG, "Ad settings: $s")
        _settings.value = s
    }

    companion object {
        const val KEY_INTERSTITIAL = "interstitial_enabled"
        const val KEY_REWARDED = "rewarded_enabled"
        const val KEY_BANNER = "banner_enabled"
        const val KEY_FREQUENCY = "ads_frequency"

        private const val TAG = "AdConfig"
        private const val FETCH_INTERVAL_SECONDS = 3600L
    }
}
