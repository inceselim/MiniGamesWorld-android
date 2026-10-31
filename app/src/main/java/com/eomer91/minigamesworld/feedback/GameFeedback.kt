package com.eomer91.minigamesworld.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.UserPrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class Sfx(@RawRes val res: Int) {
    SWIPE(R.raw.sfx_swipe),
    BLOCK(R.raw.sfx_block),
    DROP(R.raw.sfx_drop),
    CLEAR(R.raw.sfx_clear),
    COMBO(R.raw.sfx_combo),
    GAME_OVER(R.raw.sfx_game_over),
}

enum class Haptic { LIGHT, HEAVY }

/**
 * Oyunlarin ses efektleri ve titresimi. Ayarlar ekranindaki
 * "Ses efektleri" ve "Titresim" anahtarlarina uyar.
 */
@Singleton
class GameFeedback @Inject constructor(
    @ApplicationContext context: Context,
    prefs: UserPrefsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var soundEnabled = true
    @Volatile private var vibrationEnabled = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = ConcurrentHashMap<Sfx, Int>()
    private val loadedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds += sampleId
        }
        Sfx.entries.forEach { soundIds[it] = soundPool.load(context, it.res, 1) }

        scope.launch { prefs.soundEnabled.collect { soundEnabled = it } }
        scope.launch { prefs.vibrationEnabled.collect { vibrationEnabled = it } }
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!soundEnabled) return
        val id = soundIds[sfx] ?: return
        if (id !in loadedIds) return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun vibrate(haptic: Haptic) {
        if (!vibrationEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> v.vibrate(
                VibrationEffect.createPredefined(
                    if (haptic == Haptic.LIGHT) VibrationEffect.EFFECT_TICK else VibrationEffect.EFFECT_HEAVY_CLICK,
                ),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> v.vibrate(
                VibrationEffect.createOneShot(haptic.durationMs, VibrationEffect.DEFAULT_AMPLITUDE),
            )
            else -> {
                @Suppress("DEPRECATION")
                v.vibrate(haptic.durationMs)
            }
        }
    }

    private val Haptic.durationMs: Long get() = if (this == Haptic.LIGHT) 15L else 60L

    private companion object {
        const val MAX_STREAMS = 6
    }
}
