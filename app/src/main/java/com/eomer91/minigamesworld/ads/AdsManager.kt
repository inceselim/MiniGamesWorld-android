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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reklam onayi (Google UMP / GDPR) ve Unity Ads baslatma. Onay alinmadan reklam istenmez.
 * UMP'nin verdigi onay Unity Ads'e "gdpr.consent" olarak aktarilir.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
                    applyConsent(consentInfo)
                    if (consentInfo.canRequestAds()) initializeAds()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info error: ${requestError.message}")
                if (consentInfo.canRequestAds()) initializeAds()
            },
        )

        // Onceki oturumdan onay varsa beklemeden baslat.
        applyConsent(consentInfo)
        if (consentInfo.canRequestAds()) initializeAds()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy form error: ${formError.message}")
            // Kullanici tercihini degistirmis olabilir.
            applyConsent(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun updatePrivacyStatus(consentInfo: ConsentInformation) {
        _privacyOptionsRequired.value = consentInfo.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * GDPR bolgesinde (UMP onay istemis ve kullanici yanit vermisse) kullanicinin
     * TCF "Amac 1" (cihazda bilgi saklama/erisme) iznini Unity Ads'e iletir.
     * Onay gerekmeyen bolgelerde bir sey ayarlanmaz.
     */
    private fun applyConsent(consentInfo: ConsentInformation) {
        if (consentInfo.consentStatus != ConsentInformation.ConsentStatus.OBTAINED) return
        // UMP, TCF degerlerini uygulamanin varsayilan SharedPreferences dosyasina yazar.
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        val gdprApplies = prefs.getInt("IABTCF_gdprApplies", 0) == 1
        val purposes = prefs.getString("IABTCF_PurposeConsents", null)
        if (!gdprApplies) return
        val consent = purposes?.firstOrNull() == '1'
        MetaData(context).apply {
            set("gdpr.consent", consent)
            commit()
        }
    }

    private fun initializeAds() {
        val gameId = BuildConfig.UNITY_GAME_ID
        if (gameId.isBlank()) {
            Log.w(TAG, "UNITY_GAME_ID is empty, ads disabled")
            return
        }
        if (!initStarted.compareAndSet(false, true)) return
        UnityAds.initialize(
            context,
            gameId,
            BuildConfig.UNITY_TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    _adsReady.value = true
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                    Log.w(TAG, "Unity Ads init failed: $error $message")
                    // Bir sonraki onay/acilis denemesinde tekrar baslatilabilsin.
                    initStarted.set(false)
                }
            },
        )
    }

    private companion object {
        const val TAG = "AdsManager"
    }
}
