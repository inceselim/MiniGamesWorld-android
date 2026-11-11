package com.eomer91.minigamesworld.ui.games.checkers

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class CheckersLevel(val depth: Int, val noise: Int, val winScore: Int) {
    EASY(2, 80, 150),
    NORMAL(4, 12, 400),
    HARD(6, 0, 900),
}

enum class CheckersPhase { LOADING, CHOOSING, PLAYING, FINISHED }

data class CheckersUiState(
    val phase: CheckersPhase = CheckersPhase.LOADING,
    /** null = iki oyuncu ayni telefonda */
    val level: CheckersLevel? = CheckersLevel.NORMAL,
    val board: List<Int> = CheckersRules.initialBoard().toList(),
    val turn: Int = CheckersRules.WHITE,
    /** Secilen tasin su ana kadar secilen yolu (coklu almada adim adim). */
    val partial: List<Int> = emptyList(),
    /** Siradaki adim icin vurgulanan kareler. */
    val targets: Set<Int> = emptySet(),
    /** Secilen yolda su ana kadar alinan taslar (soluk cizilir). */
    val partialCaptured: List<Int> = emptyList(),
    /** Hamlesi olan taslar (secim kolayligi). */
    val movable: Set<Int> = emptySet(),
    val mustCapture: Boolean = false,
    val lastMove: CheckersMove? = null,
    /** 1 beyaz, -1 siyah, 0 berabere; null = devam ediyor */
    val winner: Int? = null,
    val botThinking: Boolean = false,
    val undoUsed: Boolean = false,
    val quietPlies: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
) {
    val vsBot: Boolean get() = level != null
    val canUndo: Boolean get() = vsBot && !undoUsed && phase == CheckersPhase.PLAYING && !botThinking && lastMove != null
}

@Serializable
data class CheckersSave(
    val version: Int = 1,
    val level: Int, // -1 = iki oyuncu
    val board: List<Int>,
    val turn: Int,
    val quietPlies: Int,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String?): CheckersSave? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
                ?.takeIf { it.version == 1 && it.board.size == 64 }
    }
}

@HiltViewModel
class CheckersViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckersUiState())
    val state: StateFlow<CheckersUiState> = _state.asStateFlow()

    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var botJob: Job? = null
    /** Bilgisayara karsi geri alma: oyuncunun son hamlesinden onceki durum. */
    private var beforePlayerMove: CheckersUiState? = null
    private var legal: List<CheckersMove> = emptyList()

    init {
        viewModelScope.launch {
            val save = CheckersSave.fromJson(prefs.savedGame(GAME_ID).first())
            if (save != null) {
                val level = CheckersLevel.entries.getOrNull(save.level)
                _state.update {
                    it.copy(phase = CheckersPhase.PLAYING, level = level, board = save.board, turn = save.turn, quietPlies = save.quietPlies)
                }
                refresh()
                maybeBot()
            } else {
                _state.update { it.copy(phase = CheckersPhase.CHOOSING) }
            }
        }
    }

    // ------------------------------------------------------------ Akis

    fun start(level: CheckersLevel?) {
        botJob?.cancel()
        beforePlayerMove = null
        _state.update {
            CheckersUiState(phase = CheckersPhase.PLAYING, level = level, wins = it.wins, losses = it.losses, goldEarned = it.goldEarned)
        }
        refresh()
        persist()
        feedback.play(Sfx.CLEAR, volume = 0.5f)
    }

    fun rematch() = start(_state.value.level)

    fun backToChoosing() {
        botJob?.cancel()
        _state.update { it.copy(phase = CheckersPhase.CHOOSING, botThinking = false) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    /** Siradaki tarafin yasal hamlelerini ve vurgulari hesapla. */
    private fun refresh() {
        val s = _state.value
        legal = CheckersRules.legalMoves(s.board.toIntArray(), s.turn)
        _state.update {
            it.copy(
                partial = emptyList(),
                targets = emptySet(),
                partialCaptured = emptyList(),
                movable = legal.map { m -> m.from }.toSet(),
                mustCapture = legal.firstOrNull()?.captured?.isNotEmpty() == true,
            )
        }
    }

    // ------------------------------------------------------------ Dokunma

    fun onSquare(i: Int) {
        val s = _state.value
        if (s.phase != CheckersPhase.PLAYING || s.botThinking) return
        if (s.vsBot && s.turn != CheckersRules.WHITE) return

        // Yeni tas secimi
        if (i in s.movable && (s.partial.size <= 1)) {
            select(listOf(i))
            feedback.vibrate(Haptic.LIGHT)
            return
        }
        if (s.partial.isEmpty() || i !in s.targets) {
            if (s.partial.size <= 1) _state.update { it.copy(partial = emptyList(), targets = emptySet()) }
            return
        }
        val path = s.partial + i
        val candidates = legal.filter { it.path.size >= path.size && it.path.subList(0, path.size) == path }
        val exact = candidates.firstOrNull { it.path.size == path.size }
        if (exact != null && candidates.size == 1) {
            play(exact)
        } else if (exact != null && candidates.all { it.path.size == path.size }) {
            play(exact)
        } else {
            select(path)
            feedback.play(Sfx.BLOCK, volume = 0.4f)
        }
    }

    private fun select(path: List<Int>) {
        val next = legal
            .filter { it.path.size > path.size && it.path.subList(0, path.size) == path }
            .map { it.path[path.size] }
            .toSet()
        _state.update { it.copy(partial = path, targets = next, partialCaptured = capturedFor(path)) }
    }

    private fun capturedFor(path: List<Int>): List<Int> =
        legal.firstOrNull { it.path.size >= path.size && it.path.subList(0, path.size) == path }
            ?.captured?.take(path.size - 1).orEmpty()

    // ------------------------------------------------------------ Hamle

    private fun play(m: CheckersMove) {
        val s = _state.value
        if (s.vsBot && s.turn == CheckersRules.WHITE) beforePlayerMove = s
        val before = s.board.toIntArray()
        val moverWasMan = kotlin.math.abs(before[m.from]) == 1
        val board = CheckersRules.apply(before, m)
        val quiet = if (m.captured.isNotEmpty() || moverWasMan) 0 else s.quietPlies + 1
        val nextTurn = -s.turn
        _state.update { it.copy(board = board.toList(), turn = nextTurn, lastMove = m, quietPlies = quiet) }

        if (m.captured.isNotEmpty()) {
            feedback.play(if (m.captured.size >= 2) Sfx.COMBO else Sfx.CLEAR, volume = 0.8f)
            feedback.vibrate(Haptic.HEAVY)
        } else {
            feedback.play(Sfx.DROP, volume = 0.6f)
        }
        val promoted = kotlin.math.abs(board[m.to]) == 2 && kotlin.math.abs(before[m.from]) == 1
        if (promoted) feedback.play(Sfx.COMBO)

        refresh()
        val result = when {
            legal.isEmpty() -> s.turn // hamlesi kalmayan kaybeder
            quiet >= CheckersRules.DRAW_PLIES -> 0
            CheckersRules.count(board, 1) == 1 && CheckersRules.count(board, -1) == 1 -> 0
            else -> null
        }
        if (result != null) finish(result) else {
            persist()
            maybeBot()
        }
    }

    private fun maybeBot() {
        val s = _state.value
        val level = s.level ?: return
        if (s.phase != CheckersPhase.PLAYING || s.turn != CheckersRules.BLACK) return
        _state.update { it.copy(botThinking = true) }
        botJob = viewModelScope.launch {
            delay(450)
            val board = _state.value.board.toIntArray()
            val move = withContext(Dispatchers.Default) {
                CheckersRules.bestMove(board, CheckersRules.BLACK, level.depth, noise = level.noise)
            }
            _state.update { it.copy(botThinking = false) }
            if (move != null) {
                // Alma zincirini adim adim goster
                if (move.path.size > 2) {
                    for (k in 2 until move.path.size) {
                        _state.update { it.copy(partial = move.path.subList(0, k), targets = emptySet(), partialCaptured = move.captured.take(k - 1)) }
                        delay(260)
                    }
                }
                play(move)
            }
        }
    }

    /** Odullu reklam sonrasi: oyuncunun son hamlesini (ve bilgisayarin cevabini) geri al. */
    fun undo() {
        val snap = beforePlayerMove ?: return
        if (!_state.value.canUndo) return
        botJob?.cancel()
        beforePlayerMove = null
        _state.update { snap.copy(undoUsed = true, wins = it.wins, losses = it.losses, goldEarned = it.goldEarned, botThinking = false) }
        refresh()
        persist()
        feedback.play(Sfx.SWIPE)
    }

    private fun finish(winner: Int) {
        val s = _state.value
        var score = 0
        var wins = s.wins
        var losses = s.losses
        if (s.level != null) {
            when (winner) {
                CheckersRules.WHITE -> {
                    wins++
                    score = s.level.winScore + CheckersRules.count(s.board.toIntArray(), CheckersRules.WHITE) * 20
                }
                CheckersRules.BLACK -> losses++
            }
        }
        if (score > 0) {
            sessionScore += score
            val delta = goldTracker.onScore(sessionScore)
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        }
        _state.update {
            it.copy(
                phase = CheckersPhase.FINISHED, winner = winner, wins = wins, losses = losses,
                finalScore = score, goldEarned = goldTracker.awarded, partial = emptyList(), targets = emptySet(), partialCaptured = emptyList(),
            )
        }
        feedback.play(if (winner == CheckersRules.WHITE || s.level == null) Sfx.CLEAR else Sfx.GAME_OVER)
        feedback.vibrate(Haptic.HEAVY)
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    private fun persist() {
        val s = _state.value
        if (s.phase != CheckersPhase.PLAYING) return
        val json = CheckersSave(level = s.level?.ordinal ?: -1, board = s.board, turn = s.turn, quietPlies = s.quietPlies).toJson()
        viewModelScope.launch { prefs.saveGame(GAME_ID, json) }
    }

    companion object {
        const val GAME_ID = "checkers"
    }
}
