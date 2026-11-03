package com.eomer91.minigamesworld.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.eomer91.minigamesworld.R
import com.google.android.play.core.review.ReviewManagerFactory

/** Arkadas davet etme ve Play Store degerlendirmesi. */
object AppActions {
    /** Play Store paket adi (debug surumunde ".debug" eki olsa da magaza linki asil paketi gostermeli). */
    private const val STORE_PACKAGE = "com.eomer91.MiniGamesWorldincesoy"
    private const val STORE_URL = "https://play.google.com/store/apps/details?id=$STORE_PACKAGE"

    /** Android paylasim sayfasini acar (WhatsApp, Telegram, SMS...). */
    fun inviteFriend(context: Context) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invite_message, STORE_URL))
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.invite_friend)).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Google Play uygulama ici degerlendirme penceresi (yildiz + yorum).
     * Google kotasi nedeniyle pencere her seferinde cikmayabilir; cikmazsa ya da hata olursa
     * magaza sayfasi acilir ki kullanici yine de yorum yazabilsin.
     */
    fun rateApp(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            } else {
                openStorePage(activity)
            }
        }
    }

    fun openStorePage(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$STORE_PACKAGE")).apply {
            setPackage("com.android.vending")
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL)).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
