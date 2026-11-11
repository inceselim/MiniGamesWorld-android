package com.eomer91.minigamesworld.ui.games.stack

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

data class StackUiState(
    val game: StackGame,
    /** Oyun ilk dokunusta baslar. */
    val started: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
    /** "PERFECT" yazisini tetiklemek icin artan sayac. */
    val perfectEvent: Int = 0,
) {
    val isNewRecord: Boolean get() = game.score > 0 && bestAtStart >= 0 && game.score > bestAtStart
}

@HiltViewModel
class StackViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = StackEngine()
    private val goldTracker = GoldTracker()

    private val _state = MutableStateFlow(StackUiState(engine.newGame()))
    val state: StateFlow<StackUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    /** Her karede cagrilir (saniye cinsinden gecen sure). */
    fun onFrame(dt: Float) {
        val s = _state.value
        if (!s.started && s.game.debris.isEmpty()) return
        _state.value = s.copy(game = engine.update(s.game, dt.coerceAtMost(0.05f)))
    }

    fun onTap() {
        val s = _state.value
        if (s.game.gameOver) return
        if (!s.started) {
            _state.value = s.copy(started = true)
            return
        }

        val (next, result) = engine.drop(s.game)
        val delta = goldTracker.onScore(next.score)

        _state.value = s.copy(
            game = next,
            goldEarned = goldTracker.awarded,
            perfectEvent = if (result == DropResult.PERFECT) s.perfectEvent + 1 else s.perfectEvent,
        )

        when (result) {
            DropResult.MISSED -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
            }
            DropResult.PERFECT -> {
                feedback.play(if (next.combo >= StackEngine.GROW_AFTER_COMBO) Sfx.CLEAR else Sfx.COMBO, volume = 0.8f)
                feedback.vibrate(Haptic.LIGHT)
            }
            DropResult.PLACED -> feedback.play(Sfx.BLOCK, volume = 0.8f)
        }

        if (next.score > s.best) viewModelScope.launch { prefs.submitScore(GAME_ID, next.score) }
    }

    fun restart() {
        goldTracker.reset()
        _state.update { StackUiState(engine.newGame(), best = it.best, bestAtStart = it.best) }
    }

    companion object {
        const val GAME_ID = "stack"
    }
}
