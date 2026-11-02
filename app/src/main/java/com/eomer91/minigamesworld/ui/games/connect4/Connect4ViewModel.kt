package com.eomer91.minigamesworld.ui.games.connect4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class C4Phase { CHOOSING, PLAYING, FINISHED }

/** [level] null ise 2 oyunculu mod. */
data class C4UiState(
    val phase: C4Phase = C4Phase.CHOOSING,
    val level: BotLevel? = BotLevel.NORMAL,
    val board: C4Board = List(Connect4Engine.COLS * Connect4Engine.ROWS) { 0 },
    /** Siradaki oyuncu: 1 ya da 2. */
    val turn: Int = 1,
    /** Son dusen tas (animasyon icin). */
    val lastDrop: Int? = null,
    val winner: Int = 0,
    val winningCells: List<Int> = emptyList(),
    val draw: Boolean = false,
    val botThinking: Boolean = false,
    val undoUsed: Boolean = false,
    /** Bu oturumdaki seri: sen - rakip. */
    val wins: Int = 0,
    val losses: Int = 0,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
) {
    val vsBot: Boolean get() = level != null
    val canUndo: Boolean get() = vsBot && !undoUsed && phase == C4Phase.PLAYING && !botThinking && board.count { it != 0 } >= 2
}

@HiltViewModel
class Connect4ViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = Connect4Engine()
    private val goldTracker = GoldTracker()
    private var botJob: Job? = null
    /** Bilgisayara karsi: oyuncunun son hamlesinden onceki tahta (geri alma icin). */
    private var beforePlayerMove: C4Board? = null
    /** Oturum boyunca toplanan puan (altin icin). */
    private var sessionScore = 0

    private val _state = MutableStateFlow(C4UiState())
    val state: StateFlow<C4UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun start(level: BotLevel?) {
        botJob?.cancel()
        beforePlayerMove = null
        _state.update {
            C4UiState(
                phase = C4Phase.PLAYING,
                level = level,
                wins = if (it.level == level) it.wins else 0,
                losses = if (it.level == level) it.losses else 0,
                goldEarned = it.goldEarned,
                best = it.best,
                bestAtStart = it.best,
            )
        }
    }

    /** Ayni modda yeni el. */
    fun rematch() = start(_state.value.level)

    fun backToChoosing() {
        botJob?.cancel()
        _state.update { C4UiState(phase = C4Phase.CHOOSING, level = it.level, goldEarned = it.goldEarned, best = it.best, bestAtStart = it.best) }
    }

    fun onColumnTap(col: Int) {
        val s = _state.value
        if (s.phase != C4Phase.PLAYING || s.botThinking || !engine.canPlay(s.board, col)) return
        if (s.vsBot && s.turn != 1) return
        if (s.vsBot) beforePlayerMove = s.board
        place(col, s.turn)
        val after = _state.value
        if (after.vsBot && after.phase == C4Phase.PLAYING) botTurn()
    }

    /** Odullu reklam sonrasi: son oyuncu + bilgisayar hamlesini geri al (bir kez). */
    fun undo() {
        val s = _state.value
        val previous = beforePlayerMove ?: return
        if (!s.canUndo) return
        botJob?.cancel()
        beforePlayerMove = null
        _state.value = s.copy(board = previous, turn = 1, lastDrop = null, undoUsed = true, botThinking = false)
        feedback.play(Sfx.SWIPE, volume = 0.5f)
    }

    private fun botTurn() {
        val level = _state.value.level ?: return
        _state.update { it.copy(botThinking = true) }
        botJob = viewModelScope.launch {
            val board = _state.value.board
            val col = withContext(Dispatchers.Default) { engine.botMove(board, level) }
            delay(350) // Dogal hissettirsin
            _state.update { it.copy(botThinking = false) }
            place(col, 2)
        }
    }

    private fun place(col: Int, player: Int) {
        val s = _state.value
        val row = engine.dropRow(s.board, col)
        val board = engine.play(s.board, col, player)
        val line = engine.winningLine(board)
        val draw = line == null && engine.isFull(board)
        var next = s.copy(board = board, turn = 3 - player, lastDrop = row * Connect4Engine.COLS + col)

        if (line != null || draw) {
            val winner = if (line != null) player else 0
            next = next.copy(phase = C4Phase.FINISHED, winner = winner, winningCells = line ?: emptyList(), draw = draw)
            if (s.vsBot) next = scoreResult(next, winner, draw)
            when {
                draw -> feedback.play(Sfx.DROP)
                !s.vsBot || winner == 1 -> {
                    feedback.play(Sfx.COMBO)
                    feedback.vibrate(Haptic.HEAVY)
                }
                else -> {
                    feedback.play(Sfx.GAME_OVER)
                    feedback.vibrate(Haptic.HEAVY)
                }
            }
        } else {
            feedback.play(Sfx.BLOCK, volume = 0.6f)
        }
        _state.value = next
    }

    private fun scoreResult(s: C4UiState, winner: Int, draw: Boolean): C4UiState {
        val level = s.level ?: return s
        val score = when {
            winner == 1 -> level.winScore
            draw -> DRAW_SCORE
            else -> 0
        }
        if (score > 0) {
            sessionScore += score
            val delta = goldTracker.onScore(sessionScore)
            if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        }
        return s.copy(
            finalScore = score,
            goldEarned = goldTracker.awarded,
            wins = s.wins + if (winner == 1) 1 else 0,
            losses = s.losses + if (winner == 2) 1 else 0,
        )
    }

    companion object {
        const val GAME_ID = "connect-four"
        const val DRAW_SCORE = 50
    }
}
