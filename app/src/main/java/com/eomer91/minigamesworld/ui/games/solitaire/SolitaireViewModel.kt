package com.eomer91.minigamesworld.ui.games.solitaire

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class SolitairePhase { LOADING, CHOOSING, PLAYING, WON }

data class SolitaireUiState(
    val phase: SolitairePhase = SolitairePhase.LOADING,
    val game: SolitaireGame? = null,
    val elapsedSeconds: Int = 0,
    val canUndo: Boolean = false,
    /** Ipucu ile parlatilan hamle (kisa sure). */
    val hint: Move? = null,
    /** Ipucu "desteden cek" ise true. */
    val hintStock: Boolean = false,
    val autoCompleting: Boolean = false,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
) {
    val isNewRecord: Boolean get() = phase == SolitairePhase.WON && bestAtStart >= 0 && finalScore > bestAtStart
}

@Serializable
private data class SolitaireSave(val version: Int = 1, val game: SolitaireGame, val elapsedSeconds: Int)

@HiltViewModel
class SolitaireViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    val engine = SolitaireEngine()
    private val goldTracker = GoldTracker()
    private val history = ArrayDeque<SolitaireGame>()
    private var hintJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(SolitaireUiState())
    val state: StateFlow<SolitaireUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { restore() }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun start(drawCount: Int) {
        history.clear()
        goldTracker.reset()
        _state.update {
            SolitaireUiState(phase = SolitairePhase.PLAYING, game = engine.deal(drawCount), best = it.best, bestAtStart = it.best)
        }
        persist()
    }

    fun backToChoosing() {
        history.clear()
        _state.update { SolitaireUiState(phase = SolitairePhase.CHOOSING, best = it.best, bestAtStart = it.best) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    fun tick() {
        if (_state.value.phase != SolitairePhase.PLAYING) return
        _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        if (_state.value.elapsedSeconds % 10 == 0) persist()
    }

    fun onStockTap() {
        val g = playableGame() ?: return
        if (g.stock.isEmpty() && g.waste.isEmpty()) return
        commit(engine.draw(g))
        feedback.play(Sfx.SWIPE, volume = 0.4f)
    }

    /** Dokunarak oynama: en iyi yere tasi. */
    fun onCardTap(pile: Pile, index: Int) {
        val g = playableGame() ?: return
        if (pile == Pile.Stock) return onStockTap()
        val move = engine.bestMove(g, pile, index)
        if (move == null) {
            feedback.vibrate(Haptic.LIGHT)
            return
        }
        doMove(g, move)
    }

    /** Surukle-birak: hedef gecersizse false (kartlar yerine doner). */
    fun onDrop(from: Pile, index: Int, to: Pile): Boolean {
        val g = playableGame() ?: return false
        val move = Move(from, index, to)
        if (!engine.isValid(g, move)) return false
        doMove(g, move)
        return true
    }

    fun undo() {
        if (_state.value.phase != SolitairePhase.PLAYING || _state.value.autoCompleting) return
        val previous = history.removeLastOrNull() ?: return
        _state.update { it.copy(game = previous, canUndo = history.isNotEmpty(), hint = null, hintStock = false) }
        feedback.play(Sfx.SWIPE, volume = 0.4f)
        persist()
    }

    /** Odullu reklam sonrasi: bir hamleyi 2.5 sn parlat. */
    fun showHint() {
        val g = playableGame() ?: return
        val move = engine.hint(g)
        hintJob?.cancel()
        _state.update { it.copy(hint = move, hintStock = move == null) }
        hintJob = viewModelScope.launch {
            delay(2_500)
            _state.update { it.copy(hint = null, hintStock = false) }
        }
    }

    fun autoComplete() {
        val start = playableGame() ?: return
        if (!engine.canAutoComplete(start)) return
        _state.update { it.copy(autoCompleting = true) }
        viewModelScope.launch {
            var g = start
            while (!g.isWon) {
                val m = engine.nextFoundationMove(g) ?: break
                g = engine.apply(g, m)
                _state.update { it.copy(game = g) }
                feedback.play(Sfx.DROP, volume = 0.3f)
                delay(90)
            }
            _state.update { it.copy(autoCompleting = false) }
            if (g.isWon) win(g)
        }
    }

    private fun playableGame(): SolitaireGame? {
        val s = _state.value
        if (s.phase != SolitairePhase.PLAYING || s.autoCompleting) return null
        return s.game
    }

    private fun doMove(g: SolitaireGame, move: Move) {
        val next = engine.apply(g, move)
        commit(next)
        when {
            move.to is Pile.Foundation -> feedback.play(Sfx.BLOCK, volume = 0.8f)
            else -> feedback.play(Sfx.DROP, volume = 0.6f)
        }
        if (next.isWon) win(next)
    }

    private fun commit(next: SolitaireGame) {
        val current = _state.value.game ?: return
        history.addLast(current)
        if (history.size > MAX_UNDO) history.removeFirst()
        _state.update { it.copy(game = next, canUndo = true, hint = null, hintStock = false) }
        persist()
    }

    private fun win(g: SolitaireGame) {
        val s = _state.value
        val score = g.score + engine.winBonus(s.elapsedSeconds)
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        _state.update { it.copy(phase = SolitairePhase.WON, game = g, finalScore = score, goldEarned = goldTracker.awarded, canUndo = false) }
        viewModelScope.launch {
            prefs.submitScore(GAME_ID, score)
            prefs.clearSavedGame(GAME_ID)
        }
        feedback.play(Sfx.COMBO)
        feedback.vibrate(Haptic.HEAVY)
    }

    private suspend fun restore() {
        val save = prefs.savedGame(GAME_ID).first()?.let { runCatching { json.decodeFromString(SolitaireSave.serializer(), it) }.getOrNull() }
        val valid = save != null && save.version == 1 &&
            (save.game.stock + save.game.waste + save.game.foundations.flatten() + save.game.tableau.flatten()).map { it.id }.toSet().size == 52
        if (!valid) {
            _state.update { it.copy(phase = SolitairePhase.CHOOSING) }
            return
        }
        _state.update { it.copy(phase = SolitairePhase.PLAYING, game = save!!.game, elapsedSeconds = save.elapsedSeconds) }
    }

    private fun persist() {
        val s = _state.value
        val g = s.game ?: return
        if (s.phase != SolitairePhase.PLAYING) return
        val text = json.encodeToString(SolitaireSave.serializer(), SolitaireSave(game = g, elapsedSeconds = s.elapsedSeconds))
        viewModelScope.launch { prefs.saveGame(GAME_ID, text) }
    }

    companion object {
        const val GAME_ID = "solitaire"
        private const val MAX_UNDO = 200
    }
}
