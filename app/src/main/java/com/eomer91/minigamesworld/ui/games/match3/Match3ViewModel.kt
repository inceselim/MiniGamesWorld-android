package com.eomer91.minigamesworld.ui.games.match3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Match3UiState(
    val board: GemBoard,
    val score: Int = 0,
    val movesLeft: Int = Match3Engine.START_MOVES,
    /** Secili hucre (dokunarak takas icin). */
    val selected: Int? = null,
    /** Su an patlayan taslarin id'leri (kuculme animasyonu). */
    val clearing: Set<Long> = emptySet(),
    /** Gecersiz takasta sallanma animasyonu icin. */
    val rejected: Set<Long> = emptySet(),
    /** Animasyon surerken giris kabul edilmez. */
    val busy: Boolean = false,
    /** Su anki zincir sirasi (2+ ise ekranda "×2" gosterilir). */
    val chain: Int = 0,
    val shuffling: Boolean = false,
    val gameOver: Boolean = false,
    /** "+5 hamle" odullu reklami bu oyunda kullanildi mi. */
    val bonusUsed: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class Match3ViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = Match3Engine()
    val boardSize: Int get() = engine.size

    private val goldTracker = GoldTracker()
    private var turnJob: Job? = null

    private val _state = MutableStateFlow(Match3UiState(board = engine.newBoard()))
    val state: StateFlow<Match3UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    /** Dokunarak oynama: ilk dokunus secer, komsu bir tasa ikinci dokunus takas eder. */
    fun onTap(cell: Int) {
        val s = _state.value
        if (s.busy || s.gameOver) return
        val sel = s.selected
        when {
            sel == null -> _state.value = s.copy(selected = cell)
            sel == cell -> _state.value = s.copy(selected = null)
            engine.areAdjacent(sel, cell) -> trySwap(sel, cell)
            else -> _state.value = s.copy(selected = cell)
        }
    }

    /** Kaydirarak oynama: hucreden bir yone. */
    fun onSwipe(cell: Int, dRow: Int, dCol: Int) {
        val r = engine.row(cell) + dRow
        val c = engine.col(cell) + dCol
        if (r !in 0 until engine.size || c !in 0 until engine.size) return
        trySwap(cell, engine.index(r, c))
    }

    private fun trySwap(a: Int, b: Int) {
        val s = _state.value
        if (s.busy || s.gameOver || !engine.areAdjacent(a, b)) return

        turnJob = viewModelScope.launch {
            val swapped = engine.swap(s.board, a, b)
            _state.value = s.copy(board = swapped, selected = null, busy = true)
            delay(SWAP_MS)

            var match = engine.findMatches(swapped)
            if (match.cells.isEmpty()) {
                // Gecersiz takas: geri al, hamle sayilmaz.
                val ids = setOfNotNull(swapped[a]?.id, swapped[b]?.id)
                _state.update { it.copy(board = s.board, rejected = ids) }
                feedback.play(Sfx.SWIPE, volume = 0.4f)
                delay(SWAP_MS)
                _state.update { it.copy(rejected = emptySet(), busy = false) }
                return@launch
            }

            _state.update { it.copy(movesLeft = it.movesLeft - 1) }
            var board = swapped
            var chain = 1
            while (match.cells.isNotEmpty()) {
                val gained = engine.scoreFor(match, chain)
                val clearingIds = match.cells.mapNotNull { board[it]?.id }.toSet()
                _state.update { it.copy(clearing = clearingIds, chain = chain, score = it.score + gained) }
                playMatchFeedback(match, chain)
                awardProgress()
                delay(CLEAR_MS)

                board = engine.collapse(board, match.cells)
                _state.update { it.copy(board = board, clearing = emptySet()) }
                delay(FALL_MS)

                chain++
                match = engine.findMatches(board)
            }

            if (!engine.hasPossibleMove(board)) {
                _state.update { it.copy(shuffling = true) }
                delay(SHUFFLE_NOTICE_MS)
                board = engine.shuffle(board)
                _state.update { it.copy(board = board, shuffling = false) }
                delay(FALL_MS)
            }

            val outOfMoves = _state.value.movesLeft <= 0
            _state.update { it.copy(busy = false, chain = 0, gameOver = outOfMoves) }
            if (outOfMoves) {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
            }
        }
    }

    /** Odullu reklam sonrasi: oyun bitmisse +5 hamle ile devam (oyun basina bir kez). */
    fun addBonusMoves() {
        _state.update {
            if (!it.gameOver || it.bonusUsed) it
            else it.copy(movesLeft = Match3Engine.BONUS_MOVES, gameOver = false, bonusUsed = true)
        }
    }

    fun restart() {
        turnJob?.cancel()
        goldTracker.reset()
        _state.update { Match3UiState(board = engine.newBoard(), best = it.best, bestAtStart = it.best) }
    }

    private fun awardProgress() {
        val score = _state.value.score
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        _state.update { it.copy(goldEarned = goldTracker.awarded) }
        if (score > _state.value.best) viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
    }

    private fun playMatchFeedback(match: MatchResult, chain: Int) {
        val big = match.runs.any { it >= 5 } || chain >= 3
        when {
            big -> {
                feedback.play(Sfx.COMBO)
                feedback.vibrate(Haptic.HEAVY)
            }
            chain == 2 || match.runs.any { it == 4 } -> {
                feedback.play(Sfx.CLEAR, volume = 0.7f)
                feedback.vibrate(Haptic.LIGHT)
            }
            else -> feedback.play(Sfx.BLOCK, volume = 0.8f)
        }
    }

    companion object {
        const val GAME_ID = "match-3"
        const val SWAP_MS = 180L
        const val CLEAR_MS = 220L
        const val FALL_MS = 280L
        const val SHUFFLE_NOTICE_MS = 700L
    }
}
