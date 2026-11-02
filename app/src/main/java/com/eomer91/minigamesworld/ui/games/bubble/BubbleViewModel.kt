package com.eomer91.minigamesworld.ui.games.bubble

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
import kotlin.math.max

/** Ucan balon: UI yol boyunca [durationMs] icinde ilerletir. */
data class FlyingBubble(val id: Long, val path: List<Vec>, val color: Int, val durationMs: Int)

/** Patlayan/dusen balonlarin gorsel efekti (konumlar birim cinsinden). */
data class PopEffect(val id: Long, val popped: List<Pair<Vec, Int>>, val dropped: List<Pair<Vec, Int>>)

data class BubbleUiState(
    val board: BubbleBoard,
    val current: Int,
    val next: Int,
    /** Nisan acisi (radyan); null iken nisan cizgisi gosterilmez. */
    val aimAngle: Float? = null,
    val flying: FlyingBubble? = null,
    val effects: List<PopEffect> = emptyList(),
    /** Yeni sira inmeden once kalan "bos" atis sayisi. */
    val shotsUntilNewRow: Int = BubbleEngine.MISSES_PER_NEW_ROW,
    val level: Int = 1,
    val score: Int = 0,
    val gameOver: Boolean = false,
    val continueUsed: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = score > 0 && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class BubbleViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    val engine = BubbleEngine()
    private val goldTracker = GoldTracker()
    private var shotJob: Job? = null
    private var effectId = 1L

    private val _state = MutableStateFlow(newState())
    val state: StateFlow<BubbleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    private fun newState(rows: Int = BubbleEngine.START_ROWS): BubbleUiState {
        val board = engine.newBoard(rows)
        return BubbleUiState(board = board, current = engine.nextColor(board), next = engine.nextColor(board))
    }

    fun aim(angle: Float?) {
        val s = _state.value
        if (s.gameOver || s.flying != null) return
        _state.value = s.copy(aimAngle = angle?.let(engine::clampAngle))
    }

    /** Atici balonu ile siradakini degistirir. */
    fun swap() {
        val s = _state.value
        if (s.gameOver || s.flying != null) return
        _state.value = s.copy(current = s.next, next = s.current)
        feedback.play(Sfx.SWIPE, volume = 0.4f)
    }

    fun shoot(angle: Float) {
        val s = _state.value
        if (s.gameOver || s.flying != null) return
        val result = engine.shoot(s.board, angle, s.current)

        shotJob = viewModelScope.launch {
            val length = result.path.zipWithNext { a, b -> a.dist(b) }.sum()
            val duration = max(120, (length / SPEED_UNITS_PER_SEC * 1000).toInt())
            _state.update {
                it.copy(aimAngle = null, flying = FlyingBubble(effectId++, result.path, s.current, duration))
            }
            delay(duration.toLong())
            land(result)
        }
    }

    private fun land(result: ShotResult) {
        val s = _state.value
        val popped = result.popped.size
        var board = result.board
        var score = s.score + result.points
        var shotsLeft = if (popped > 0) s.shotsUntilNewRow else s.shotsUntilNewRow - 1
        var level = s.level

        val effect = if (popped > 0) {
            PopEffect(
                effectId++,
                result.popped.map { (slot, b) -> engine.center(s.board, slot) to b.color },
                result.dropped.map { (slot, b) -> engine.center(s.board, slot) to b.color },
            )
        } else null

        when {
            popped == 0 -> feedback.play(Sfx.DROP, volume = 0.6f)
            result.dropped.isNotEmpty() || popped >= 5 -> {
                feedback.play(Sfx.COMBO)
                feedback.vibrate(Haptic.LIGHT)
            }
            else -> feedback.play(Sfx.BLOCK)
        }

        // Tahta temizlendi: bonus ve yeni (bir sira fazla) tahta.
        if (board.isEmpty) {
            score += BubbleEngine.CLEAR_BONUS
            level += 1
            board = engine.newBoard(BubbleEngine.START_ROWS + level - 1)
            shotsLeft = BubbleEngine.MISSES_PER_NEW_ROW
            feedback.play(Sfx.CLEAR)
            feedback.vibrate(Haptic.HEAVY)
        } else if (shotsLeft <= 0) {
            board = engine.addRow(board)
            shotsLeft = BubbleEngine.MISSES_PER_NEW_ROW
        }

        val gameOver = engine.isGameOver(board)
        _state.update {
            it.copy(
                board = board,
                flying = null,
                current = it.next.takeIf { c -> c in board.colors || board.isEmpty } ?: engine.nextColor(board),
                next = engine.nextColor(board),
                score = score,
                shotsUntilNewRow = shotsLeft,
                level = level,
                gameOver = gameOver,
                effects = it.effects + listOfNotNull(effect),
            )
        }
        awardProgress()
        if (gameOver) {
            feedback.play(Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
        }

        if (effect != null) {
            viewModelScope.launch {
                delay(EFFECT_MS)
                _state.update { st -> st.copy(effects = st.effects.filterNot { it.id == effect.id }) }
            }
        }
    }

    /** Odullu reklam sonrasi: alttaki 3 sirayi temizleyip devam (oyun basina bir kez). */
    fun continueAfterLoss() {
        _state.update {
            if (!it.gameOver || it.continueUsed) return@update it
            val board = engine.clearBottomRows(it.board)
            it.copy(
                board = board,
                gameOver = engine.isGameOver(board),
                continueUsed = true,
                shotsUntilNewRow = BubbleEngine.MISSES_PER_NEW_ROW,
                current = engine.nextColor(board),
                next = engine.nextColor(board),
            )
        }
    }

    fun restart() {
        shotJob?.cancel()
        goldTracker.reset()
        _state.update { newState().copy(best = it.best, bestAtStart = it.best) }
    }

    private fun awardProgress() {
        val score = _state.value.score
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        _state.update { it.copy(goldEarned = goldTracker.awarded) }
        if (score > _state.value.best) viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
    }

    companion object {
        const val GAME_ID = "bubble-shooter"
        private const val SPEED_UNITS_PER_SEC = 28f
        const val EFFECT_MS = 650L
    }
}
