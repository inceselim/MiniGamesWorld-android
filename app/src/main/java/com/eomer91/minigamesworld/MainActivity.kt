package com.eomer91.minigamesworld

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.ads.AdsManager
import com.eomer91.minigamesworld.ads.LocalAdsReady
import com.eomer91.minigamesworld.ui.navigation.MiniGamesAppRoot
import com.eomer91.minigamesworld.ui.theme.MiniGamesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity: Android 12 ve altinda uygulama ici dil secimi icin gerekli.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var adsManager: AdsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        adsManager.gatherConsent(this)

        setContent {
            val adsReady by adsManager.adsReady.collectAsStateWithLifecycle()
            val privacyRequired by adsManager.privacyOptionsRequired.collectAsStateWithLifecycle()

            MiniGamesTheme {
                CompositionLocalProvider(LocalAdsReady provides adsReady) {
                    MiniGamesAppRoot(
                        privacyOptionsRequired = privacyRequired,
                        onShowPrivacyOptions = { adsManager.showPrivacyOptions(this@MainActivity) },
                    )
                }
            }
        }
    }
}
