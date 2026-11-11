package com.eomer91.minigamesworld.ui.games.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SudokuPhase { LOADING, CHOOSING, GENERATING, PLAYING, WON, LOST }

data class SudokuUiState(
    val phase: SudokuPhase = SudokuPhase.LOADING,
    val difficulty: SudokuDifficulty = SudokuDifficulty.EASY,
    val givens: List<Int> = List(SudokuRules.CELLS) { 0 },
    val solution: List<Int> = List(SudokuRules.CELLS) { 0 },
    /** Oyuncunun yazdigi rakamlar (0 = bos). Yanlis rakamlar da burada durur, kirmizi gosterilir. */
    val entries: List<Int> = List(SudokuRules.CELLS) { 0 },
    val notes: List<Set<Int>> = List(SudokuRules.CELLS) { emptySet() },
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Int = 0,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
) {
    fun valueAt(i: Int): Int = givens[i].takeIf { it != 0 } ?: entries[i]
    fun isGiven(i: Int): Boolean = givens[i] != 0
    fun isWrong(i: Int): Boolean = !isGiven(i) && entries[i] != 0 && entries[i] != solution[i]
    fun isCorrect(i: Int): Boolean = valueAt(i) != 0 && valueAt(i) == solution[i]

    /** Bir rakamin tahtada dogru yerlestirilmis kopya sayisi (9 olunca tus soluklasir). */
    fun placedCount(digit: Int): Int = (0 until SudokuRules.CELLS).count { isCorrect(it) && valueAt(it) == digit }

    val isNewRecord: Boolean get() = phase == SudokuPhase.WON && bestAtStart >= 0 && finalScore > bestAtStart
}

@HiltViewModel
class SudokuViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val generator = SudokuGenerator()
    private val goldTracker = GoldTracker()

    private val _state = MutableStateFlow(SudokuUiState())
    val state: StateFlow<SudokuUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { restoreSavedGame() }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    // --- Oyun akisi ---

    fun startNewGame(difficulty: SudokuDifficulty) {
        _state.update { it.copy(phase = SudokuPhase.GENERATING, difficulty = difficulty) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { generator.generate(difficulty) }
            goldTracker.reset()
            _state.update {
                SudokuUiState(
                    phase = SudokuPhase.PLAYING,
                    difficulty = difficulty,
                    givens = puzzle.givens,
                    solution = puzzle.solution,
                    best = it.best,
                    bestAtStart = it.best,
                )
            }
            persist()
        }
    }

    /** Restart: zorluk secim ekranina doner, kayit silinir. */
    fun backToDifficultySelection() {
        _state.update { SudokuUiState(phase = SudokuPhase.CHOOSING, best = it.best, bestAtStart = it.best) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    /** Ekran on plandayken saniyede bir cagrilir. */
    fun tick() {
        if (_state.value.phase != SudokuPhase.PLAYING) return
        _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        if (_state.value.elapsedSeconds % 10 == 0) persist()
    }

    // --- Girdi ---

    fun select(cell: Int) {
        if (_state.value.phase != SudokuPhase.PLAYING) return
        _state.update { it.copy(selected = cell) }
    }

    fun toggleNotesMode() {
        _state.update { it.copy(notesMode = !it.notesMode) }
    }

    fun input(digit: Int) {
        val s = _state.value
        val cell = s.selected ?: return
        if (s.phase != SudokuPhase.PLAYING || s.isGiven(cell) || s.isCorrect(cell)) return

        if (s.notesMode) {
            if (s.entries[cell] != 0) return
            val current = s.notes[cell]
            val updated = if (digit in current) current - digit else current + digit
            _state.value = s.copy(notes = s.notes.replaceAt(cell, updated))
            feedback.play(Sfx.SWIPE, volume = 0.3f)
            persist()
            return
        }

        if (digit == s.solution[cell]) placeCorrect(s, cell, digit, fromHint = false) else placeWrong(s, cell, digit)
    }

    fun erase() {
        val s = _state.value
        val cell = s.selected ?: return
        if (s.phase != SudokuPhase.PLAYING || s.isGiven(cell) || s.isCorrect(cell)) return
        _state.value = s.copy(entries = s.entries.replaceAt(cell, 0), notes = s.notes.replaceAt(cell, emptySet()))
        persist()
    }

    /** Odullu reklam sonrasi: secili hucreyi (bos/yanlissa) ya da ilk bos hucreyi dogru rakamla doldurur. */
    fun useHint() {
        val s = _state.value
        if (s.phase != SudokuPhase.PLAYING) return
        val target = s.selected?.takeIf { !s.isCorrect(it) }
            ?: (0 until SudokuRules.CELLS).firstOrNull { !s.isCorrect(it) }
            ?: return
        placeCorrect(s.copy(selected = target, hintsUsed = s.hintsUsed + 1), target, s.solution[target], fromHint = true)
    }

    /** Odullu reklam sonrasi: 3 hatayla kaybedilen oyuna devam (hata sayisi 2'ye iner, yanlislar silinir). */
    fun continueAfterLoss() {
        val s = _state.value
        if (s.phase != SudokuPhase.LOST) return
        val cleaned = s.entries.mapIndexed { i, v -> if (s.isWrong(i)) 0 else v }
        _state.value = s.copy(phase = SudokuPhase.PLAYING, mistakes = MAX_MISTAKES - 1, entries = cleaned)
        persist()
    }

    private fun placeCorrect(s: SudokuUiState, cell: Int, digit: Int, fromHint: Boolean) {
        val peers = SudokuRules.peers(cell).toSet()
        val notes = s.notes.mapIndexed { i, n -> if (i == cell) emptySet() else if (i in peers) n - digit else n }
        var next = s.copy(entries = s.entries.replaceAt(cell, digit), notes = notes, selected = cell)

        val solved = (0 until SudokuRules.CELLS).all { next.isCorrect(it) }
        if (solved) {
            val score = sudokuScore(next.difficulty, next.mistakes, next.hintsUsed, next.elapsedSeconds)
            val delta = goldTracker.onScore(score)
            next = next.copy(phase = SudokuPhase.WON, finalScore = score, goldEarned = goldTracker.awarded, selected = null)
            _state.value = next
            viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
            feedback.play(Sfx.COMBO)
            feedback.vibrate(Haptic.HEAVY)
            persist()
            return
        }

        _state.value = next
        val completedUnit = listOf(
            SudokuRules.rowCells(SudokuRules.row(cell)),
            SudokuRules.colCells(SudokuRules.col(cell)),
            SudokuRules.boxCells(SudokuRules.box(cell)),
        ).any { unit -> unit.all { next.isCorrect(it) } }

        when {
            completedUnit -> {
                feedback.play(Sfx.CLEAR, volume = 0.7f)
                feedback.vibrate(Haptic.LIGHT)
            }
            fromHint -> feedback.play(Sfx.DROP)
            else -> feedback.play(Sfx.BLOCK, volume = 0.7f)
        }
        persist()
    }

    private fun placeWrong(s: SudokuUiState, cell: Int, digit: Int) {
        val mistakes = s.mistakes + 1
        val lost = mistakes >= MAX_MISTAKES
        _state.value = s.copy(
            entries = s.entries.replaceAt(cell, digit),
            mistakes = mistakes,
            phase = if (lost) SudokuPhase.LOST else s.phase,
        )
        if (lost) {
            feedback.play(Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
        } else {
            feedback.vibrate(Haptic.HEAVY)
        }
        persist()
    }

    // --- Kaydetme ---

    private suspend fun restoreSavedGame() {
        val save = prefs.savedGame(GAME_ID).first()?.let(SudokuSave::fromJson)
        if (save == null) {
            _state.update { it.copy(phase = SudokuPhase.CHOOSING) }
            return
        }
        goldTracker.reset()
        _state.update {
            it.copy(
                phase = SudokuPhase.PLAYING,
                difficulty = SudokuDifficulty.valueOf(save.difficulty),
                givens = save.givens,
                solution = save.solution,
                entries = save.entries,
                notes = save.notes.map { n -> n.toSet() },
                mistakes = save.mistakes,
                hintsUsed = save.hintsUsed,
                elapsedSeconds = save.elapsedSeconds,
            )
        }
    }

    /** Oyun suruyorsa kaydeder; kazanildi/kaybedildi ise kaydi siler. */
    private fun persist() {
        val s = _state.value
        val json = when (s.phase) {
            SudokuPhase.PLAYING -> SudokuSave(
                difficulty = s.difficulty.name,
                givens = s.givens,
                solution = s.solution,
                entries = s.entries,
                notes = s.notes.map { it.sorted() },
                mistakes = s.mistakes,
                hintsUsed = s.hintsUsed,
                elapsedSeconds = s.elapsedSeconds,
            ).toJson()
            SudokuPhase.WON, SudokuPhase.LOST -> null
            else -> return
        }
        viewModelScope.launch {
            if (json == null) prefs.clearSavedGame(GAME_ID) else prefs.saveGame(GAME_ID, json)
        }
    }

    private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> = mapIndexed { i, v -> if (i == index) value else v }

    companion object {
        const val GAME_ID = "sudoku"
        const val MAX_MISTAKES = 3
    }
}
