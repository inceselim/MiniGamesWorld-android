package com.eomer91.minigamesworld.ui.games.chess

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

enum class ChessLevel(val depth: Int, val noise: Int, val maxNodes: Int, val winScore: Int) {
    EASY(1, 160, 20_000, 200),
    NORMAL(3, 15, 150_000, 500),
    HARD(5, 0, 400_000, 1200),
}

enum class ChessPhase { LOADING, CHOOSING, PLAYING, FINISHED }

enum class ChessEnd { CHECKMATE, STALEMATE, INSUFFICIENT, FIFTY, REPETITION }

data class ChessUiState(
    val phase: ChessPhase = ChessPhase.LOADING,
    /** null = iki oyuncu */
    val level: ChessLevel? = ChessLevel.NORMAL,
    val board: List<Int> = ChessPosition.initial().toList(),
    val side: Int = Chess.WHITE,
    val selected: Int = -1,
    val targets: Set<Int> = emptySet(),
    val lastFrom: Int = -1,
    val lastTo: Int = -1,
    /** Sah tehdit altindaysa sahin karesi */
    val checkSquare: Int = -1,
    /** Terfi secimi bekleyen hamle (from, to) */
    val promotion: Pair<Int, Int>? = null,
    /** 1 beyaz, -1 siyah, 0 berabere */
    val winner: Int? = null,
    val end: ChessEnd? = null,
    val botThinking: Boolean = false,
    val undoUsed: Boolean = false,
    val wins: Int = 0,
    val losses: Int = 0,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
) {
    val vsBot: Boolean get() = level != null
    val canUndo: Boolean get() = vsBot && !undoUsed && phase == ChessPhase.PLAYING && !botThinking && lastFrom >= 0

    /** Alinan taslar: baslangica gore eksik olanlar (renk isaretli). */
    val captured: List<Int>
        get() {
            val start = intArrayOf(0, 8, 2, 2, 2, 1, 1)
            val out = ArrayList<Int>()
            for (s in intArrayOf(Chess.WHITE, Chess.BLACK)) {
                for (t in Chess.QUEEN downTo Chess.PAWN) {
                    val now = board.count { it == t * s }
                    repeat((start[t] - now).coerceAtLeast(0)) { out += t * s }
                }
            }
            return out
        }
}

@Serializable
data class ChessSave(
    val version: Int = 1,
    val level: Int,
    val board: List<Int>,
    val side: Int,
    val castle: Int,
    val ep: Int,
    val half: Int,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String?): ChessSave? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
                ?.takeIf { it.version == 1 && it.board.size == 64 }
    }
}

@HiltViewModel
class ChessViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(ChessUiState())
    val state: StateFlow<ChessUiState> = _state.asStateFlow()

    private var pos = ChessPosition()
    private var legal: List<Int> = emptyList()
    /** Tekrar kurali icin gorulen pozisyonlar */
    private val seen = HashMap<String, Int>()
    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var botJob: Job? = null
    /** Geri alma: oyuncunun son hamlesinden onceki pozisyon */
    private var beforePlayer: Pair<ChessPosition, ChessUiState>? = null

    init {
        viewModelScope.launch {
            val save = ChessSave.fromJson(prefs.savedGame(GAME_ID).first())
            if (save != null) {
                pos = ChessPosition(save.board.toIntArray(), save.side, save.castle, save.ep, save.half)
                _state.update { it.copy(phase = ChessPhase.PLAYING, level = ChessLevel.entries.getOrNull(save.level)) }
                refresh(null, null)
                maybeBot()
            } else {
                _state.update { it.copy(phase = ChessPhase.CHOOSING) }
            }
        }
    }

    // ------------------------------------------------------------ Akis

    fun start(level: ChessLevel?) {
        botJob?.cancel()
        pos = ChessPosition()
        seen.clear()
        beforePlayer = null
        _state.update { ChessUiState(phase = ChessPhase.PLAYING, level = level, wins = it.wins, losses = it.losses, goldEarned = it.goldEarned) }
        refresh(null, null)
        persist()
        feedback.play(Sfx.CLEAR, volume = 0.5f)
    }

    fun rematch() = start(_state.value.level)

    fun backToChoosing() {
        botJob?.cancel()
        _state.update { it.copy(phase = ChessPhase.CHOOSING, botThinking = false) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    private fun refresh(lastFrom: Int?, lastTo: Int?) {
        legal = pos.legalMoves()
        val check = if (pos.inCheck()) pos.kingSquare(pos.side) else -1
        _state.update {
            it.copy(
                board = pos.b.toList(),
                side = pos.side,
                selected = -1,
                targets = emptySet(),
                promotion = null,
                checkSquare = check,
                lastFrom = lastFrom ?: it.lastFrom,
                lastTo = lastTo ?: it.lastTo,
            )
        }
    }

    // ------------------------------------------------------------ Dokunma

    fun onSquare(i: Int) {
        val s = _state.value
        if (s.phase != ChessPhase.PLAYING || s.botThinking || s.promotion != null) return
        if (s.vsBot && s.side != Chess.WHITE) return
        val v = s.board[i]
        if (v * s.side > 0) {
            val targets = legal.filter { Chess.from(it) == i }.map { Chess.to(it) }.toSet()
            _state.update { it.copy(selected = if (it.selected == i) -1 else i, targets = if (it.selected == i) emptySet() else targets) }
            feedback.vibrate(Haptic.LIGHT)
            return
        }
        if (s.selected >= 0 && i in s.targets) {
            val candidates = legal.filter { Chess.from(it) == s.selected && Chess.to(it) == i }
            if (candidates.any { Chess.promo(it) != 0 }) {
                _state.update { it.copy(promotion = s.selected to i) }
            } else {
                play(candidates.first())
            }
            return
        }
        _state.update { it.copy(selected = -1, targets = emptySet()) }
    }

    fun promote(piece: Int) {
        val (from, to) = _state.value.promotion ?: return
        val m = legal.firstOrNull { Chess.from(it) == from && Chess.to(it) == to && Chess.promo(it) == piece } ?: return
        play(m)
    }

    fun cancelPromotion() {
        _state.update { it.copy(promotion = null) }
    }

    // ------------------------------------------------------------ Hamle

    private fun play(m: Int) {
        val s = _state.value
        if (s.vsBot && pos.side == Chess.WHITE) beforePlayer = pos.copy() to s
        val captured = pos.b[Chess.to(m)] != 0 || Chess.flag(m) == Chess.FLAG_EP
        pos.make(m)
        val key = pos.key()
        seen[key] = (seen[key] ?: 0) + 1
        refresh(Chess.from(m), Chess.to(m))

        when {
            _state.value.checkSquare >= 0 -> {
                feedback.play(Sfx.COMBO, volume = 0.7f)
                feedback.vibrate(Haptic.HEAVY)
            }
            captured -> {
                feedback.play(Sfx.CLEAR, volume = 0.7f)
                feedback.vibrate(Haptic.LIGHT)
            }
            else -> feedback.play(Sfx.DROP, volume = 0.6f)
        }

        val end = when {
            legal.isEmpty() && pos.inCheck() -> ChessEnd.CHECKMATE
            legal.isEmpty() -> ChessEnd.STALEMATE
            pos.insufficientMaterial() -> ChessEnd.INSUFFICIENT
            pos.half >= 100 -> ChessEnd.FIFTY
            (seen[key] ?: 0) >= 3 -> ChessEnd.REPETITION
            else -> null
        }
        if (end != null) {
            finish(end, if (end == ChessEnd.CHECKMATE) -pos.side else 0)
        } else {
            persist()
            maybeBot()
        }
    }

    private fun maybeBot() {
        val s = _state.value
        val level = s.level ?: return
        if (s.phase != ChessPhase.PLAYING || pos.side != Chess.BLACK) return
        _state.update { it.copy(botThinking = true) }
        val snapshot = pos.copy()
        botJob = viewModelScope.launch {
            delay(350)
            val move = withContext(Dispatchers.Default) {
                ChessAi.bestMove(snapshot, level.depth, noise = level.noise, maxNodes = level.maxNodes)
            }
            _state.update { it.copy(botThinking = false) }
            if (move != null) play(move)
        }
    }

    /** Odullu reklam sonrasi: oyuncunun son hamlesi ve bilgisayarin cevabi geri alinir. */
    fun undo() {
        val (p, snap) = beforePlayer ?: return
        if (!_state.value.canUndo) return
        botJob?.cancel()
        beforePlayer = null
        pos = p
        _state.update { snap.copy(undoUsed = true, wins = it.wins, losses = it.losses, goldEarned = it.goldEarned, botThinking = false) }
        refresh(snap.lastFrom, snap.lastTo)
        persist()
        feedback.play(Sfx.SWIPE)
    }

    private fun finish(end: ChessEnd, winner: Int) {
        val s = _state.value
        var score = 0
        var wins = s.wins
        var losses = s.losses
        if (s.level != null) {
            when (winner) {
                Chess.WHITE -> {
                    wins++
                    score = s.level.winScore
                }
                Chess.BLACK -> losses++
                else -> score = s.level.winScore / 4
            }
        }
        if (score > 0) {
            sessionScore += score
            val delta = goldTracker.onScore(sessionScore)
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
        }
        _state.update {
            it.copy(phase = ChessPhase.FINISHED, winner = winner, end = end, wins = wins, losses = losses, finalScore = score, goldEarned = goldTracker.awarded)
        }
        val good = winner == Chess.WHITE || s.level == null || winner == 0
        feedback.play(if (good) Sfx.CLEAR else Sfx.GAME_OVER)
        feedback.vibrate(Haptic.HEAVY)
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    private fun persist() {
        val s = _state.value
        if (s.phase != ChessPhase.PLAYING) return
        val json = ChessSave(level = s.level?.ordinal ?: -1, board = pos.b.toList(), side = pos.side, castle = pos.castle, ep = pos.ep, half = pos.half).toJson()
        viewModelScope.launch { prefs.saveGame(GAME_ID, json) }
    }

    companion object {
        const val GAME_ID = "chess"
    }
}
