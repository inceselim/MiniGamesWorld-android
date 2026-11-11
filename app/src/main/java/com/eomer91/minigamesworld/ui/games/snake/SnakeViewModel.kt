package com.eomer91.minigamesworld.ui.games.snake

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

data class SnakeUiState(
    val game: SnakeGame,
    /** Oyun ilk kaydirmada baslar. */
    val started: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = game.score > 0 && bestAtStart >= 0 && game.score > bestAtStart
}

@HiltViewModel
class SnakeViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = SnakeEngine()

    private val goldTracker = GoldTracker()

    /** Puan 200'un katlarini gectikce altin verir; bu oyunda toplam kazanilan altini dondurur. */
    private fun awardGold(score: Int): Int {
        val delta = goldTracker.onScore(score)
        return goldTracker.awarded
    }
    val gridSize: Int get() = engine.gridSize

    private val _state = MutableStateFlow(SnakeUiState(engine.newGame()))
    val state: StateFlow<SnakeUiState> = _state.asStateFlow()

    /**
     * Bir adimda birden fazla kaydirma gelebilir. Hepsini sirayla uyguluyoruz;
     * boylece hizli "yukari + sola" hareketi yilanin kendi boynuna donmesine yol acmaz.
     */
    private val pendingTurns = ArrayDeque<SnakeDirection>()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun onSwipe(direction: SnakeDirection) {
        val current = _state.value
        if (current.game.gameOver) return
        if (!current.started) _state.update { it.copy(started = true) }

        val last = pendingTurns.lastOrNull() ?: current.game.direction
        if (direction == last || direction.isOpposite(last)) return
        if (pendingTurns.size < MAX_PENDING_TURNS) pendingTurns.addLast(direction)
    }

    fun tickMillis(): Long = engine.tickMillis(_state.value.game)

    fun tick() {
        val current = _state.value
        if (!current.started || current.game.gameOver) return

        val direction = pendingTurns.removeFirstOrNull() ?: current.game.direction
        val next = engine.step(current.game, direction)
        _state.value = current.copy(game = next, goldEarned = awardGold(next.score))

        when {
            next.gameOver -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
            }
            next.foodsEaten > current.game.foodsEaten -> {
                feedback.play(Sfx.DROP)
                feedback.vibrate(Haptic.LIGHT)
            }
        }

        if (next.score > current.best) {
            viewModelScope.launch { prefs.submitScore(GAME_ID, next.score) }
        }
    }

    fun restart() {
        goldTracker.reset()
        pendingTurns.clear()
        _state.update { SnakeUiState(engine.newGame(), best = it.best, bestAtStart = it.best) }
    }

    companion object {
        const val GAME_ID = "snake"
        private const val MAX_PENDING_TURNS = 3
    }
}
