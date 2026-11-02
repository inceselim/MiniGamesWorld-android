package com.eomer91.minigamesworld.ui.games.mines

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MinesPhase { LOADING, CHOOSING, PLAYING, WON, LOST }

data class MinesUiState(
    val phase: MinesPhase = MinesPhase.LOADING,
    val difficulty: MinesDifficulty = MinesDifficulty.EASY,
    val board: MineBoard? = null,
    /** true: dokunus bayrak koyar; false: dokunus acar. */
    val flagMode: Boolean = false,
    val elapsedSeconds: Int = 0,
    /** Kaybettiren mayin (ikinci sans icin). */
    val explodedAt: Int? = null,
    val secondChanceUsed: Boolean = false,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
) {
    val isNewRecord: Boolean get() = phase == MinesPhase.WON && bestAtStart >= 0 && finalScore > bestAtStart
}

@HiltViewModel
class MinesViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = MinesEngine()
    private val goldTracker = GoldTracker()

    private val _state = MutableStateFlow(MinesUiState())
    val state: StateFlow<MinesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { restore() }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun start(d: MinesDifficulty) {
        goldTracker.reset()
        _state.update { MinesUiState(phase = MinesPhase.PLAYING, difficulty = d, board = engine.empty(d), best = it.best, bestAtStart = it.best) }
    }

    fun backToChoosing() {
        _state.update { MinesUiState(phase = MinesPhase.CHOOSING, best = it.best, bestAtStart = it.best) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    fun toggleFlagMode() = _state.update { it.copy(flagMode = !it.flagMode) }

    fun tick() {
        val s = _state.value
        // Sure ilk dokunusla baslar.
        if (s.phase != MinesPhase.PLAYING || s.board?.minesPlaced != true) return
        _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        if (_state.value.elapsedSeconds % 10 == 0) persist()
    }

    fun onTap(i: Int) {
        val s = _state.value
        if (s.phase != MinesPhase.PLAYING) return
        val board = s.board ?: return
        if (s.flagMode && !board.cells[i].revealed) return flag(i)

        val result = if (board.cells[i].revealed) engine.chord(board, i) else engine.reveal(board, i)
        apply(result)
    }

    fun onLongPress(i: Int) {
        if (_state.value.phase != MinesPhase.PLAYING) return
        flag(i)
    }

    private fun flag(i: Int) {
        val s = _state.value
        val board = s.board ?: return
        if (board.cells[i].revealed) return
        _state.value = s.copy(board = engine.toggleFlag(board, i))
        feedback.vibrate(Haptic.LIGHT)
        feedback.play(Sfx.SWIPE, volume = 0.4f)
        persist()
    }

    private fun apply(result: RevealResult) {
        val s = _state.value
        if (result.hitMine != null) {
            _state.value = s.copy(board = engine.revealMines(result.board), phase = MinesPhase.LOST, explodedAt = result.hitMine)
            feedback.play(Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
            persist()
            return
        }
        if (engine.isWon(result.board)) {
            val score = minesScore(s.difficulty, s.elapsedSeconds, s.secondChanceUsed)
            val delta = goldTracker.onScore(score)
            if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
            _state.value = s.copy(board = result.board, phase = MinesPhase.WON, finalScore = score, goldEarned = goldTracker.awarded)
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
            feedback.play(Sfx.COMBO)
            feedback.vibrate(Haptic.HEAVY)
            persist()
            return
        }
        _state.value = s.copy(board = result.board)
        when {
            result.opened >= 10 -> feedback.play(Sfx.CLEAR, volume = 0.5f)
            result.opened > 0 -> feedback.play(Sfx.BLOCK, volume = 0.6f)
        }
        persist()
    }

    /** Odullu reklam sonrasi: patlayan mayin bayraklanir, oyun devam eder (bir kez). */
    fun secondChance() {
        val s = _state.value
        val exploded = s.explodedAt ?: return
        if (s.phase != MinesPhase.LOST || s.secondChanceUsed) return
        _state.value = s.copy(
            board = engine.secondChance(s.board ?: return, exploded),
            phase = MinesPhase.PLAYING,
            explodedAt = null,
            secondChanceUsed = true,
        )
        persist()
    }

    private suspend fun restore() {
        val save = prefs.savedGame(GAME_ID).first()?.let(MinesSave::fromJson)
        val board = save?.toBoard(engine)
        if (save == null || board == null) {
            _state.update { it.copy(phase = MinesPhase.CHOOSING) }
            return
        }
        _state.update {
            it.copy(
                phase = MinesPhase.PLAYING,
                difficulty = MinesDifficulty.valueOf(save.difficulty),
                board = board,
                elapsedSeconds = save.elapsedSeconds,
                secondChanceUsed = save.secondChanceUsed,
            )
        }
    }

    private fun persist() {
        val s = _state.value
        val board = s.board ?: return
        val json = when (s.phase) {
            MinesPhase.PLAYING -> MinesSave.from(s.difficulty, board, s.elapsedSeconds, s.secondChanceUsed).toJson()
            // Bitince kayit silinir (kaybettikten sonra cikip girerek mayini "geri almak" mumkun olmasin).
            MinesPhase.WON, MinesPhase.LOST -> null
            else -> return
        }
        viewModelScope.launch { if (json == null) prefs.clearSavedGame(GAME_ID) else prefs.saveGame(GAME_ID, json) }
    }

    companion object {
        const val GAME_ID = "minesweeper"
    }
}
