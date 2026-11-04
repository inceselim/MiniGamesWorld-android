package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.metadata.MetaData
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reklam onayi (UMP / GDPR) ve Unity Ads baslatma. Onay alinmadan reklam istenmez.
 * UMP'nin kaydettigi IAB TCF onayi Unity Ads'e "gdpr.consent" olarak da iletilir.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val initStarted = AtomicBoolean(false)

    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun gatherConsent(activity: Activity) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form error: ${formError.message}")
                    updatePrivacyStatus(consentInfo)
                    if (consentInfo.canRequestAds()) initializeAds()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info error: ${requestError.message}")
                if (consentInfo.canRequestAds()) initializeAds()
            },
        )

        // Onceki oturumdan onay varsa beklemeden baslat.
        if (consentInfo.canRequestAds()) initializeAds()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy form error: ${formError.message}")
        }
    }

    private fun updatePrivacyStatus(consentInfo: ConsentInformation) {
        _privacyOptionsRequired.value = consentInfo.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeAds() {
        if (!initStarted.compareAndSet(false, true)) return
        if (BuildConfig.UNITY_GAME_ID.isBlank()) {
            Log.w(TAG, "UNITY_GAME_ID bos, reklamlar kapali")
            return
        }
        applyGdprConsent()
        startUnityAds(attempt = 0)
    }

    /**
     * GDPR gecerliyse (AB/IKA) UMP formundaki secimi Unity'ye bildir:
     * cihazda bilgi saklama/erisim (TCF amac 1) izni verildiyse kisisellestirilmis reklam acik.
     */
    private fun applyGdprConsent() {
        // TCF degerleri varsayilan SharedPreferences dosyasinda tutulur.
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        val gdprApplies = runCatching { prefs.getInt("IABTCF_gdprApplies", 0) }.getOrDefault(0) == 1
        if (!gdprApplies) return
        val purposes = prefs.getString("IABTCF_PurposeConsents", "").orEmpty()
        val consent = purposes.firstOrNull() == '1'
        @Suppress("DEPRECATION")
        MetaData(context).apply {
            set("gdpr.consent", consent)
            commit()
        }
    }

    @Suppress("DEPRECATION") // Yeni API (4.19+) henuz deneysel; eski API 5.0'a kadar destekleniyor.
    private fun startUnityAds(attempt: Int) {
        UnityAds.initialize(
            context,
            BuildConfig.UNITY_GAME_ID,
            BuildConfig.DEBUG, // debug'da test reklamlari
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    _adsReady.value = true
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                    Log.w(TAG, "Unity Ads init failed: $error $message")
                    // Genelde baglanti yoktur; artan araliklarla tekrar dene.
                    if (attempt < MAX_INIT_RETRIES) {
                        scope.launch {
                            delay(RETRY_BASE_MS * (attempt + 1))
                            startUnityAds(attempt + 1)
                        }
                    }
                }
            },
        )
    }

    private companion object {
        const val TAG = "AdsManager"
        const val MAX_INIT_RETRIES = 5
        const val RETRY_BASE_MS = 15_000L
    }
}
