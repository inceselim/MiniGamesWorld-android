package com.eomer91.minigamesworld.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
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
 * Reklam onayi (UMP / GDPR) ve Unity LevelPlay baslatma. Onay alinmadan reklam istenmez.
 * UMP ile alinan onay LevelPlay'e ve agi adaptorlerine otomatik aktarilir.
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
        if (BuildConfig.LEVELPLAY_APP_KEY.isBlank()) {
            Log.w(TAG, "LEVELPLAY_APP_KEY bos, reklamlar kapali")
            return
        }
        if (BuildConfig.DEBUG) LevelPlay.setMetaData("is_test_suite", "enable")
        startLevelPlay(attempt = 0)
    }

    private fun startLevelPlay(attempt: Int) {
        val request = LevelPlayInitRequest.Builder(BuildConfig.LEVELPLAY_APP_KEY).build()
        LevelPlay.init(context, request, object : LevelPlayInitListener {
            override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                _adsReady.value = true
            }

            override fun onInitFailed(error: LevelPlayInitError) {
                Log.w(TAG, "LevelPlay init failed: $error")
                // Genelde baglanti yoktur; artan araliklarla tekrar dene.
                if (attempt < MAX_INIT_RETRIES) {
                    scope.launch {
                        delay(RETRY_BASE_MS * (attempt + 1))
                        startLevelPlay(attempt + 1)
                    }
                }
            }
        })
    }

    /** Sadece debug: LevelPlay'in adaptor/reklam test ekranini acar. */
    fun launchTestSuite() {
        if (BuildConfig.DEBUG && _adsReady.value) LevelPlay.launchTestSuite(context)
    }

    private companion object {
        const val TAG = "AdsManager"
        const val MAX_INIT_RETRIES = 5
        const val RETRY_BASE_MS = 15_000L
    }
}
