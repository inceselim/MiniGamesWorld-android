package com.eomer91.minigamesworld.ui.games.memory

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

data class MemoryUiState(
    val cards: List<MemoryCard>,
    val moves: Int = 0,
    val selected: List<Int> = emptyList(),
    /** Eslesmeyen iki kart kapanana kadar yeni kart acilamaz. */
    val locked: Boolean = false,
    val completed: Boolean = false,
    val finalScore: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = completed && bestAtStart >= 0 && finalScore > bestAtStart
}

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = MemoryEngine()

    private val goldTracker = GoldTracker()

    /** Puan 200'un katlarini gectikce altin verir; bu oyunda toplam kazanilan altini dondurur. */
    private fun awardGold(score: Int): Int {
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        return goldTracker.awarded
    }
    private val _state = MutableStateFlow(MemoryUiState(cards = engine.newDeck()))
    val state: StateFlow<MemoryUiState> = _state.asStateFlow()

    /** Yanlis eslesmede kartlari kapatan zamanlayici; Restart'ta iptal edilir. */
    private var flipBackJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun onCardTap(id: Int) {
        val s = _state.value
        if (s.completed || s.locked) return
        val card = s.cards.firstOrNull { it.id == id } ?: return
        if (card.isFaceUp || card.isMatched) return

        val cards = engine.flipUp(s.cards, id)
        val selected = s.selected + id

        if (selected.size < 2) {
            _state.value = s.copy(cards = cards, selected = selected)
            feedback.play(Sfx.SWIPE, volume = 0.5f)
            return
        }

        val (a, b) = selected
        val moves = s.moves + 1

        if (engine.isPair(cards, a, b)) {
            val matched = engine.markMatched(cards, selected)
            val completed = engine.allMatched(matched)
            val score = if (completed) engine.scoreFor(moves) else 0
            _state.value = s.copy(
                cards = matched,
                moves = moves,
                selected = emptyList(),
                completed = completed,
                finalScore = score,
                goldEarned = if (completed) awardGold(score) else s.goldEarned,
            )
            if (completed) {
                feedback.play(Sfx.CLEAR)
                feedback.vibrate(Haptic.HEAVY)
                viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
            } else {
                feedback.play(Sfx.BLOCK)
                feedback.vibrate(Haptic.LIGHT)
            }
        } else {
            _state.value = s.copy(cards = cards, moves = moves, selected = selected, locked = true)
            feedback.play(Sfx.SWIPE, volume = 0.5f)
            flipBackJob = viewModelScope.launch {
                delay(FLIP_BACK_DELAY_MS)
                _state.update {
                    it.copy(cards = engine.flipDown(it.cards, selected), selected = emptyList(), locked = false)
                }
            }
        }
    }

    fun restart() {
        goldTracker.reset()
        flipBackJob?.cancel()
        _state.update { MemoryUiState(cards = engine.newDeck(), best = it.best, bestAtStart = it.best) }
    }

    companion object {
        const val GAME_ID = "memory-card"
        private const val FLIP_BACK_DELAY_MS = 800L
    }
}
