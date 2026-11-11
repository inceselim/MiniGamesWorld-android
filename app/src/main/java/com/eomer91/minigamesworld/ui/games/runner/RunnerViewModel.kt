package com.eomer91.minigamesworld.ui.games.runner

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RunnerPhase { READY, PLAYING, PAUSED, CRASHED, OVER }

data class RunnerUiState(
    val phase: RunnerPhase = RunnerPhase.READY,
    val reviveUsed: Boolean = false,
    val score: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class RunnerViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(RunnerUiState())
    val state: StateFlow<RunnerUiState> = _state.asStateFlow()

    var engine = RunnerEngine()
        private set

    var frame by mutableLongStateOf(0L)
        private set

    private val goldTracker = GoldTracker()
    private var lastCoins = 0
    private var lastJumps = 0
    private var crashTimer = 0f

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun start() {
        if (_state.value.phase == RunnerPhase.READY || _state.value.phase == RunnerPhase.PAUSED) {
            _state.update { it.copy(phase = RunnerPhase.PLAYING) }
        }
    }

    fun restart() {
        engine = RunnerEngine()
        goldTracker.reset()
        lastCoins = 0
        lastJumps = 0
        _state.update { RunnerUiState(phase = RunnerPhase.PLAYING, best = it.best, bestAtStart = it.best) }
        frame++
    }

    fun pause() {
        if (_state.value.phase == RunnerPhase.PLAYING) _state.update { it.copy(phase = RunnerPhase.PAUSED) }
    }

    fun swipe(dir: Int) {
        val s = _state.value
        if (s.phase == RunnerPhase.READY || s.phase == RunnerPhase.PAUSED) start()
        if (_state.value.phase != RunnerPhase.PLAYING) return
        when (dir) {
            0 -> engine.left()
            1 -> engine.right()
            2 -> engine.jump()
            3 -> engine.duck()
        }
        feedback.play(Sfx.SWIPE, volume = 0.25f)
    }

    fun onFrame(dt: Float) {
        val s = _state.value
        val e = engine
        if (s.phase == RunnerPhase.CRASHED) {
            e.update(dt)
            crashTimer -= dt
            frame++
            return
        }
        if (s.phase != RunnerPhase.PLAYING) return
        e.update(dt.coerceAtMost(0.04f))
        frame++
        if (e.coins > lastCoins) {
            feedback.play(Sfx.DROP, volume = 0.4f)
            lastCoins = e.coins
        }
        if (e.jumpEvents > lastJumps) lastJumps = e.jumpEvents
        val delta = goldTracker.onScore(e.score)
        if (e.dead) {
            feedback.play(Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
            crashTimer = 0.8f
            _state.update { it.copy(phase = if (it.reviveUsed) RunnerPhase.OVER else RunnerPhase.CRASHED, score = e.score, goldEarned = goldTracker.awarded) }
            if (_state.value.phase == RunnerPhase.OVER) finish()
        } else if (frame % 15 == 0L) {
            _state.update { it.copy(score = e.score) }
        }
    }

    /** Odullu reklam sonrasi devam. */
    fun revive() {
        if (_state.value.phase != RunnerPhase.CRASHED) return
        engine.revive()
        _state.update { it.copy(phase = RunnerPhase.PAUSED, reviveUsed = true) }
    }

    fun giveUp() {
        if (_state.value.phase != RunnerPhase.CRASHED) return
        _state.update { it.copy(phase = RunnerPhase.OVER) }
        finish()
    }

    private fun finish() {
        val score = engine.score
        viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        _state.update { it.copy(score = score, goldEarned = goldTracker.awarded) }
    }

    companion object {
        const val GAME_ID = "mine-run"
    }
}
