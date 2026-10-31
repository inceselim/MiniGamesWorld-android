package com.eomer91.minigamesworld.ui.games.game2048

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.UserPrefsRepository
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
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class Game2048ViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
) : ViewModel() {

    private val engine = Game2048Engine()

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
            gameOver = !engine.canMove(result.board),
            reachedWinTile = current.reachedWinTile || engine.hasTile(result.board, Game2048Engine.WIN_TILE),
        )
        _state.value = next

        // Rekor gecildiginde aninda kaydet: oyuncu oyunun ortasinda cikarsa da kaybolmaz.
        if (score > current.best) {
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        }
    }

    fun restart() {
        _state.update { Game2048State(board = engine.initialBoard(), best = it.best, bestAtStart = it.best) }
    }

    companion object {
        const val GAME_ID = "2048"
    }
}
