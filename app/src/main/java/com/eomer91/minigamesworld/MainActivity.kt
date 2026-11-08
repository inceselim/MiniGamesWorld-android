package com.eomer91.minigamesworld

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import com.eomer91.minigamesworld.ads.AdsManager
import com.eomer91.minigamesworld.ads.InterstitialAdManager
import com.eomer91.minigamesworld.ads.LocalInterstitialAds
import com.eomer91.minigamesworld.ads.LocalRewardedAds
import com.eomer91.minigamesworld.ads.RewardedAdManager
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
    lateinit var rewardedAds: RewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        adsManager.start()

        setContent {
            MiniGamesTheme {
                CompositionLocalProvider(
                    LocalInterstitialAds provides interstitialAds,
                    LocalRewardedAds provides rewardedAds,
                ) {
                    MiniGamesAppRoot()
                }
            }
        }
    }
}
