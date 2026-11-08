package com.eomer91.minigamesworld.ads

import android.content.Context
import android.util.Log
import com.eomer91.minigamesworld.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unity Ads baslatma. GDPR onayi Unity'nin kendi cozumuyle alinir: AB'deki kullaniciya ilk reklamda
 * kisisellestirilmis reklam secenegi sunulur, reklamdaki gizlilik simgesinden de degistirilebilir.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val initStarted = AtomicBoolean(false)

    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    fun start() {
        val gameId = BuildConfig.UNITY_GAME_ID
        if (gameId.isBlank()) {
            Log.w(TAG, "UNITY_GAME_ID bos; reklamlar kapali")
            return
        }
        if (!initStarted.compareAndSet(false, true)) return
        UnityAds.initialize(context, gameId, BuildConfig.UNITY_TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                _adsReady.value = true
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                Log.w(TAG, "Unity Ads baslatilamadi: $error $message")
                initStarted.set(false) // sonraki acilista tekrar denensin
            }
        })
    }

    private companion object {
        const val TAG = "AdsManager"
    }
}
