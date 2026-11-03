package com.eomer91.minigamesworld.ui.games.blockrain

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

enum class BlockRainPhase { LOADING, READY, PLAYING, PAUSED, OVER }

data class BlockRainUiState(
    val phase: BlockRainPhase = BlockRainPhase.LOADING,
    val score: Int = 0,
    val lines: Int = 0,
    val level: Int = 1,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
    /** Odullu reklamla kurtarma (oyun basina bir kez). */
    val rescueUsed: Boolean = false,
    /** Kayitli oyundan devam ediliyor. */
    val resumed: Boolean = false,
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class BlockRainViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    var engine = BlockRainEngine()
        private set

    /** Her karede artar; Canvas yeniden cizilsin diye okunur. */
    var frame by mutableLongStateOf(0L)
        private set

    private val _state = MutableStateFlow(BlockRainUiState())
    val state: StateFlow<BlockRainUiState> = _state.asStateFlow()

    private val goldTracker = GoldTracker()
    private var lastLocks = 0
    private var lastClears = 0
    private var submitted = 0

    init {
        viewModelScope.launch {
            val save = BlockRainSave.fromJson(prefs.savedGame(GAME_ID).first())
            if (save != null && engine.restore(save)) {
                goldTracker.restore(save.goldAwarded)
                syncCounters()
                _state.update {
                    it.copy(
                        phase = BlockRainPhase.PAUSED, score = engine.score, lines = engine.lines, level = engine.level,
                        goldEarned = save.goldAwarded, rescueUsed = save.rescueUsed, resumed = true,
                    )
                }
            } else {
                _state.update { it.copy(phase = BlockRainPhase.READY) }
            }
        }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    private fun syncCounters() {
        lastLocks = engine.lockCount
        lastClears = engine.clearEvents
    }

    private val playing get() = _state.value.phase == BlockRainPhase.PLAYING

    // ------------------------------------------------------------ Akis

    fun start() {
        when (_state.value.phase) {
            BlockRainPhase.READY, BlockRainPhase.PAUSED -> _state.update { it.copy(phase = BlockRainPhase.PLAYING) }
            else -> Unit
        }
    }

    fun pause() {
        if (playing) {
            engine.setSoftDrop(false)
            _state.update { it.copy(phase = BlockRainPhase.PAUSED) }
            persist()
        }
    }

    fun restart() {
        engine = BlockRainEngine()
        goldTracker.reset()
        syncCounters()
        submitted = 0
        _state.update {
            BlockRainUiState(phase = BlockRainPhase.PLAYING, best = it.best, bestAtStart = it.best)
        }
        frame++
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    /** Odullu reklam sonrasi: alt satirlar silinir, oyun devam eder. */
    fun rescue() {
        if (_state.value.phase != BlockRainPhase.OVER || _state.value.rescueUsed) return
        engine.rescue()
        syncCounters()
        _state.update { it.copy(phase = BlockRainPhase.PLAYING, rescueUsed = true) }
        feedback.play(Sfx.CLEAR)
        frame++
    }

    // ------------------------------------------------------------ Girdi

    fun move(dx: Int) {
        if (playing && engine.move(dx)) {
            frame++
            feedback.vibrate(Haptic.LIGHT)
        }
    }

    fun rotate(dir: Int = 1) {
        if (playing && engine.rotate(dir)) {
            frame++
            feedback.play(Sfx.SWIPE, volume = 0.35f)
        }
    }

    fun softDrop(on: Boolean) {
        if (!playing) return
        engine.setSoftDrop(on)
        if (on && engine.softDrop()) frame++
    }

    fun hardDrop() {
        if (!playing) return
        engine.hardDrop()
        feedback.vibrate(Haptic.HEAVY)
        afterChange()
    }

    fun hold() {
        if (playing && engine.holdPiece()) {
            frame++
            feedback.play(Sfx.SWIPE, volume = 0.5f)
        }
    }

    // ------------------------------------------------------------ Dongu

    fun onFrame(dt: Float) {
        val e = engine
        if (!playing) {
            if (e.flashTimer > 0f) {
                e.update(0f)
                frame++
            }
            return
        }
        e.update(dt.coerceAtMost(0.05f))
        frame++
        afterChange()
    }

    private fun afterChange() {
        val e = engine
        if (e.lockCount > lastLocks) {
            if (e.clearEvents > lastClears) {
                if (e.lastCleared >= 4) {
                    feedback.play(Sfx.COMBO)
                    feedback.vibrate(Haptic.HEAVY)
                } else {
                    feedback.play(Sfx.CLEAR, volume = 0.8f)
                    feedback.vibrate(Haptic.LIGHT)
                }
            } else {
                feedback.play(Sfx.BLOCK, volume = 0.5f)
            }
            syncCounters()
            persist()
        }
        val s = _state.value
        if (e.score != s.score || e.lines != s.lines || e.gameOver) {
            val delta = goldTracker.onScore(e.score)
            if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
            _state.update {
                it.copy(
                    score = e.score, lines = e.lines, level = e.level, goldEarned = goldTracker.awarded,
                    phase = if (e.gameOver) BlockRainPhase.OVER else it.phase,
                )
            }
        }
        if (e.gameOver && s.phase == BlockRainPhase.PLAYING) {
            feedback.play(Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
            submitScore()
            viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
        }
    }

    private fun submitScore() {
        val score = engine.score
        if (score > submitted) {
            submitted = score
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        }
    }

    private fun persist() {
        val e = engine
        if (e.gameOver || _state.value.phase == BlockRainPhase.LOADING) return
        val json = e.snapshot().copy(goldAwarded = goldTracker.awarded, rescueUsed = _state.value.rescueUsed).toJson()
        viewModelScope.launch { prefs.saveGame(GAME_ID, json) }
        if (e.score > _state.value.best) submitScore()
    }

    companion object {
        const val GAME_ID = "block-rain"
    }
}
