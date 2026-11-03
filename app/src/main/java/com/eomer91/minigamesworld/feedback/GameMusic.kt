package com.eomer91.minigamesworld.feedback

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eomer91.minigamesworld.data.UserPrefsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Oyun fon muzigi (dongu). Ayarlardaki "Muzik" anahtarina uyar.
 * Muzikler tools/music/compose.py ile uretilir.
 */
@Singleton
class GameMusic @Inject constructor(
    @ApplicationContext private val context: Context,
    prefs: UserPrefsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    @RawRes private var track = 0
    private var wanted = false
    private var enabled = true

    init {
        scope.launch {
            prefs.musicEnabled.collect {
                enabled = it
                apply()
            }
        }
    }

    /** [res] parcasini cal (ayni parca caliyorsa kaldigi yerden devam). */
    fun play(@RawRes res: Int) {
        wanted = true
        if (track != res) {
            release()
            track = res
        }
        apply()
    }

    fun pause() {
        wanted = false
        apply()
    }

    fun stop() {
        wanted = false
        release()
        track = 0
    }

    private fun apply() {
        if (wanted && enabled && track != 0) {
            val p = player ?: runCatching {
                MediaPlayer.create(context, track)?.apply {
                    isLooping = true
                    setVolume(VOLUME, VOLUME)
                }
            }.getOrNull().also { player = it } ?: return
            if (!p.isPlaying) p.start()
        } else {
            player?.let { if (it.isPlaying) it.pause() }
        }
    }

    private fun release() {
        player?.runCatching { stop(); release() }
        player = null
    }

    private companion object {
        const val VOLUME = 0.5f
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GameMusicEntryPoint {
    fun gameMusic(): GameMusic
}

/**
 * Ekran acikken [playing] true ise [track] calar; oyun durunca, uygulama arka plana gecince
 * (reklam acilinca da) durur, ekrandan cikinca biter.
 */
@Composable
fun GameMusicEffect(@RawRes track: Int, playing: Boolean) {
    val context = LocalContext.current
    val music = remember { EntryPointAccessors.fromApplication(context.applicationContext, GameMusicEntryPoint::class.java).gameMusic() }
    val owner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            music.stop()
        }
    }
    LaunchedEffect(track, playing && resumed) {
        if (playing && resumed) music.play(track) else music.pause()
    }
}
