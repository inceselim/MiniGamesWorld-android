package com.eomer91.minigamesworld.ui.games.game2048

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

data class Game2048State(
    val board: Board,
    val score: Int = 0,
    /** Kayitli en iyi skor (canli). */
    val best: Int = 0,
    /** Bu oyun basladiginda kayitli olan en iyi skor; "yeni rekor" kontrolu icin. */
    val bestAtStart: Int = -1,
    val gameOver: Boolean = false,
    val reachedWinTile: Boolean = false,
    /** Bu oyunda kazanilan altin. */
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class Game2048ViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = Game2048Engine()

    private val goldTracker = GoldTracker()

    /** Puan 200'un katlarini gectikce altin verir; bu oyunda toplam kazanilan altini dondurur. */
    private fun awardGold(score: Int): Int {
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        return goldTracker.awarded
    }

    private val _state = MutableStateFlow(Game2048State(board = engine.initialBoard()))
    val state: StateFlow<Game2048State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update {
                    it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart)
                }
            }
        }
    }

    fun move(direction: Direction) {
        val current = _state.value
        if (current.gameOver) return

        val result = engine.move(current.board, direction)
        if (!result.moved) return

        val score = current.score + result.gainedScore
        val next = current.copy(
            board = result.board,
            score = score,
            goldEarned = awardGold(score),
            gameOver = !engine.canMove(result.board),
            reachedWinTile = current.reachedWinTile || engine.hasTile(result.board, Game2048Engine.WIN_TILE),
        )
        _state.value = next
        playMoveFeedback(current, next, result.gainedScore)

        // Rekor gecildiginde aninda kaydet: oyuncu oyunun ortasinda cikarsa da kaybolmaz.
        if (score > current.best) {
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        }
    }

    private fun playMoveFeedback(before: Game2048State, after: Game2048State, gained: Int) {
        when {
            after.gameOver -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
            }
            after.reachedWinTile && !before.reachedWinTile -> {
                feedback.play(Sfx.CLEAR)
                feedback.vibrate(Haptic.HEAVY)
            }
            // 128 ve ustu bir tas olustuysa "kombo" sesi
            gained >= BIG_MERGE -> {
                feedback.play(Sfx.COMBO)
                feedback.vibrate(Haptic.LIGHT)
            }
            gained > 0 -> feedback.play(Sfx.BLOCK, volume = 0.8f)
            else -> feedback.play(Sfx.DROP, volume = 0.5f)
        }
    }

    fun restart() {
        goldTracker.reset()
        _state.update { Game2048State(board = engine.initialBoard(), best = it.best, bestAtStart = it.best) }
    }

    companion object {
        const val GAME_ID = "2048"
        private const val BIG_MERGE = 128
    }
}
