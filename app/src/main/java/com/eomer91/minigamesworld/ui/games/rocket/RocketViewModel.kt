package com.eomer91.minigamesworld.ui.games.rocket

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

data class RocketUiState(
    val game: RocketGame,
    val reviveUsed: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = game.score > 0 && bestAtStart >= 0 && game.score > bestAtStart
}

@HiltViewModel
class RocketViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = RocketEngine()
    private val goldTracker = GoldTracker()

    private val _state = MutableStateFlow(RocketUiState(engine.newGame()))
    val state: StateFlow<RocketUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun onTap() {
        val s = _state.value
        if (s.game.gameOver) return
        _state.value = s.copy(game = engine.flap(s.game))
        feedback.play(Sfx.SWIPE, volume = 0.25f)
    }

    fun onFrame(dt: Float) {
        val s = _state.value
        if (s.game.gameOver) return
        val (next, ev) = engine.update(s.game, dt.coerceAtMost(0.05f))

        var goldEarned = s.goldEarned
        if (ev.passed > 0 || ev.star) {
            val delta = goldTracker.onScore(next.score)
            goldEarned = goldTracker.awarded
        }
        _state.value = s.copy(game = next, goldEarned = goldEarned)

        when {
            ev.crashed -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
                if (next.score > s.best) viewModelScope.launch { prefs.submitScore(GAME_ID, next.score) }
            }
            ev.star -> feedback.play(Sfx.COMBO, volume = 0.7f)
            ev.passed > 0 -> feedback.play(Sfx.BLOCK, volume = 0.5f)
        }
    }

    /** Odullu reklam sonrasi: kalkanla devam (bir kez). */
    fun revive() {
        _state.update {
            if (!it.game.gameOver || it.reviveUsed) it else it.copy(game = engine.revive(it.game), reviveUsed = true)
        }
    }

    fun restart() {
        goldTracker.reset()
        _state.update { RocketUiState(engine.newGame(), best = it.best, bestAtStart = it.best) }
    }

    companion object {
        const val GAME_ID = "rocket-hop"
    }
}
