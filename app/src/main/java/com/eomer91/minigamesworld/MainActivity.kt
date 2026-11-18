package com.eomer91.minigamesworld

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.ads.AdsManager
import com.eomer91.minigamesworld.ads.InterstitialAdManager
import com.eomer91.minigamesworld.ads.LocalInterstitialAds
import com.eomer91.minigamesworld.ads.LocalRewardedAds
import com.eomer91.minigamesworld.ads.RewardedAdManager
import com.eomer91.minigamesworld.ads.AdConfig
import com.eomer91.minigamesworld.ads.LocalAdsReady
import com.eomer91.minigamesworld.ads.LocalBannerEnabled
import com.eomer91.minigamesworld.ads.LocalPremium
import com.eomer91.minigamesworld.billing.BillingManager
import com.eomer91.minigamesworld.billing.LocalBilling
import com.eomer91.minigamesworld.ui.navigation.MiniGamesAppRoot
import com.eomer91.minigamesworld.ui.theme.MiniGamesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity: Android 12 ve altinda uygulama ici dil secimi icin gerekli.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var adsManager: AdsManager

    @Inject
    lateinit var interstitialAds: InterstitialAdManager

    @Inject
    lateinit var billing: BillingManager

    @Inject
    lateinit var rewardedAds: RewardedAdManager

    @Inject
    lateinit var adConfig: AdConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        adsManager.gatherConsent(this)
        billing.start()

        setContent {
            val adsReady by adsManager.adsReady.collectAsStateWithLifecycle()
            val privacyRequired by adsManager.privacyOptionsRequired.collectAsStateWithLifecycle()
            val premium by billing.isPremium.collectAsStateWithLifecycle()
            val adSettings by adConfig.settings.collectAsStateWithLifecycle()

            MiniGamesTheme {
                CompositionLocalProvider(
                    LocalAdsReady provides adsReady,
                    LocalPremium provides premium,
                    LocalBannerEnabled provides adSettings.bannerEnabled,
                    LocalBilling provides billing,
                    LocalInterstitialAds provides interstitialAds,
                    LocalRewardedAds provides rewardedAds,
                ) {
                    MiniGamesAppRoot(
                        privacyOptionsRequired = privacyRequired,
                        onShowPrivacyOptions = { adsManager.showPrivacyOptions(this@MainActivity) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Abonelik iptal/yenileme durumunu Play'den dogrula
        billing.refresh()
    }
}
