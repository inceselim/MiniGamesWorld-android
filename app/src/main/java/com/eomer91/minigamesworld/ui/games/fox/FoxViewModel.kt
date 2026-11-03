package com.eomer91.minigamesworld.ui.games.fox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FoxPhase { LOADING, HUB, PLAYING, PAUSED, DEAD, WON }

data class FoxUiState(
    val phase: FoxPhase = FoxPhase.LOADING,
    val profile: FoxProfile = FoxProfile(),
    val world: Int = 0,
    val level: Int = 0,
    /** Bu denemede reklamla yerinde dirilme kullanildi mi? */
    val reviveUsed: Boolean = false,
    val result: FoxResult? = null,
    val score: Int = 0,
    val goldEarned: Int = 0,
    val newBestStars: Boolean = false,
) {
    val hasNext: Boolean get() = world * FoxEngine.LEVELS_PER_WORLD + level < FoxEngine.WORLDS * FoxEngine.LEVELS_PER_WORLD - 1
}

@HiltViewModel
class FoxViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(FoxUiState())
    val state: StateFlow<FoxUiState> = _state.asStateFlow()

    var engine: FoxEngine? = null
        private set

    /** Her karede artar; Canvas yeniden cizilsin diye okunur. */
    var frame by mutableLongStateOf(0L)
        private set

    private var left = false
    private var right = false
    private val goldTracker = GoldTracker()

    // Kareler arasi fark (ses icin)
    private var lastCoins = 0
    private var lastStars = 0
    private var lastJumps = 0
    private var lastStomps = 0
    private var lastSprings = 0
    private var lastBreaks = 0
    private var lastHurts = 0
    private var lastCheckpoint = -1
    private var coinSfxCooldown = 0f

    init {
        viewModelScope.launch {
            val p = FoxProfile.fromJson(prefs.savedGame(PROFILE_KEY).first())
            val w = p.unlocked / FoxEngine.LEVELS_PER_WORLD
            _state.update { it.copy(phase = FoxPhase.HUB, profile = p, world = w.coerceAtMost(FoxEngine.WORLDS - 1)) }
        }
    }

    // ------------------------------------------------------------ Hub

    fun selectWorld(w: Int) {
        if (w in 0 until FoxEngine.WORLDS) _state.update { it.copy(world = w) }
    }

    fun start(world: Int, level: Int) {
        if (!_state.value.profile.isUnlocked(world, level)) return
        val e = FoxEngine(world, level, FoxLevels.worlds[world][level])
        engine = e
        left = false
        right = false
        lastCoins = 0; lastStars = 0; lastJumps = 0; lastStomps = 0; lastSprings = 0; lastBreaks = 0; lastHurts = 0
        lastCheckpoint = -1
        _state.update { it.copy(phase = FoxPhase.PLAYING, world = world, level = level, reviveUsed = false, result = null, score = 0, goldEarned = 0, newBestStars = false) }
        feedback.play(Sfx.CLEAR, volume = 0.5f)
    }

    fun retry() = start(_state.value.world, _state.value.level)

    fun next() {
        val s = _state.value
        val idx = s.world * FoxEngine.LEVELS_PER_WORLD + s.level + 1
        start(idx / FoxEngine.LEVELS_PER_WORLD, idx % FoxEngine.LEVELS_PER_WORLD)
    }

    fun toHub() {
        engine = null
        _state.update { it.copy(phase = FoxPhase.HUB) }
    }

    // ------------------------------------------------------------ Girdi

    fun setLeft(pressed: Boolean) { left = pressed }
    fun setRight(pressed: Boolean) { right = pressed }

    fun jumpDown() {
        if (_state.value.phase == FoxPhase.PLAYING) engine?.pressJump()
    }

    fun jumpUp() {
        engine?.releaseJump()
    }

    fun pause() {
        if (_state.value.phase == FoxPhase.PLAYING) {
            left = false; right = false
            engine?.releaseJump()
            _state.update { it.copy(phase = FoxPhase.PAUSED) }
        }
    }

    fun resume() {
        if (_state.value.phase == FoxPhase.PAUSED) _state.update { it.copy(phase = FoxPhase.PLAYING) }
    }

    // ------------------------------------------------------------ Dongu

    fun onFrame(dt: Float) {
        val e = engine ?: return
        val phase = _state.value.phase
        if (phase != FoxPhase.PLAYING) return
        val dir = (if (right) 1 else 0) - (if (left) 1 else 0)
        e.update(dt.coerceAtMost(0.033f), dir)
        frame++
        sounds(e, dt)

        if (e.dead && e.deathTimer > 1.1f) {
            left = false; right = false
            _state.update { it.copy(phase = FoxPhase.DEAD) }
        }
        if (e.won && e.winTimer > 1.2f) finish(e)
    }

    private fun sounds(e: FoxEngine, dt: Float) {
        coinSfxCooldown -= dt
        if (e.coins > lastCoins && coinSfxCooldown <= 0f) {
            feedback.play(Sfx.DROP, volume = 0.35f)
            coinSfxCooldown = 0.06f
        }
        if (e.stars > lastStars) {
            feedback.play(Sfx.COMBO)
            feedback.vibrate(Haptic.LIGHT)
        }
        if (e.jumpCount > lastJumps) feedback.play(Sfx.SWIPE, volume = 0.3f)
        if (e.stompCount > lastStomps) {
            feedback.play(Sfx.BLOCK, volume = 0.6f)
            feedback.vibrate(Haptic.LIGHT)
        }
        if (e.springCount > lastSprings) feedback.play(Sfx.SWIPE, volume = 0.7f)
        if (e.breakCount > lastBreaks) feedback.play(Sfx.BLOCK, volume = 0.5f)
        if (e.hurtCount > lastHurts) feedback.vibrate(Haptic.HEAVY)
        if (e.checkpointCol != lastCheckpoint && e.checkpointCol >= 0) feedback.play(Sfx.CLEAR, volume = 0.5f)
        if (e.dead && !deadSeen) {
            deadSeen = true
            feedback.play(Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
        }
        if (!e.dead) deadSeen = false
        if (e.won && !wonSeen) {
            wonSeen = true
            feedback.play(Sfx.CLEAR)
        }
        lastCoins = e.coins
        lastStars = e.stars
        lastJumps = e.jumpCount
        lastStomps = e.stompCount
        lastSprings = e.springCount
        lastBreaks = e.breakCount
        lastHurts = e.hurtCount
        lastCheckpoint = e.checkpointCol
    }

    private var deadSeen = false
    private var wonSeen = false

    // ------------------------------------------------------------ Olum

    /** Ucretsiz: son kontrol noktasindan devam. */
    fun continueFromCheckpoint() {
        val e = engine ?: return
        e.respawnAtCheckpoint()
        deadSeen = false
        _state.update { it.copy(phase = FoxPhase.PLAYING) }
    }

    /** Odullu reklam: olunen yerde tam canla devam (deneme basina bir kez). */
    fun reviveHere() {
        val e = engine ?: return
        e.reviveHere()
        deadSeen = false
        _state.update { it.copy(phase = FoxPhase.PLAYING, reviveUsed = true) }
    }

    // ------------------------------------------------------------ Bitis

    private fun finish(e: FoxEngine) {
        val s = _state.value
        if (s.phase == FoxPhase.WON) return
        wonSeen = false
        val r = e.result()
        val newStars = r.stars > s.profile.starsOf(s.world, s.level)
        val profile = s.profile.withResult(s.world, s.level, r)
        goldTracker.reset()
        val gold = goldTracker.onScore(r.score)
        _state.update { it.copy(phase = FoxPhase.WON, profile = profile, result = r, score = r.score, goldEarned = gold, newBestStars = newStars) }
        viewModelScope.launch {
            prefs.saveGame(PROFILE_KEY, profile.toJson())
            if (gold > 0) prefs.addGold(gold)
            prefs.submitScore(GAME_ID, r.score)
        }
    }

    companion object {
        const val GAME_ID = "fox-adventure"
        /** Katalog id'sinden farkli: detay ekraninda "devam" butonu cikmasin. */
        const val PROFILE_KEY = "fox-adventure-profile"
    }
}
