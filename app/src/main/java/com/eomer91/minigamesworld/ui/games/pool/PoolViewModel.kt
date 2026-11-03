package com.eomer91.minigamesworld.ui.games.pool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
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
import kotlin.math.atan2
import kotlin.math.hypot

enum class PoolLevel(val noiseDeg: Float, val winScore: Int) {
    EASY(6f, 200),
    NORMAL(2.5f, 500),
    HARD(0.8f, 1000),
}

enum class PoolPhase { CHOOSING, PLAYING, FINISHED }

data class PoolUiState(
    val phase: PoolPhase = PoolPhase.CHOOSING,
    /** null = iki oyuncu */
    val level: PoolLevel? = PoolLevel.NORMAL,
    val turn: Int = 0,
    val groups: List<PoolGroup?> = listOf(null, null),
    val ballInHand: Boolean = false,
    val shooting: Boolean = false,
    val botThinking: Boolean = false,
    val event: PoolEvent = PoolEvent.NONE,
    val winner: Int = -1,
    val wins: Int = 0,
    val losses: Int = 0,
    val finalScore: Int = 0,
    val goldEarned: Int = 0,
) {
    val vsBot: Boolean get() = level != null
    val humanTurn: Boolean get() = phase == PoolPhase.PLAYING && !shooting && !botThinking && (!vsBot || turn == 0)
}

@HiltViewModel
class PoolViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(PoolUiState())
    val state: StateFlow<PoolUiState> = _state.asStateFlow()

    var game = PoolGame()
        private set

    var frame by mutableLongStateOf(0L)
        private set

    /** Nisan acisi (masa koordinati) ve guc (0..1, cekilirken) */
    var aim = Math.PI.toFloat()
        private set
    var power = 0f
        private set

    private var botJob: Job? = null
    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var lastPocketed = 0

    fun start(level: PoolLevel?) {
        botJob?.cancel()
        game = PoolGame()
        aim = 0f
        power = 0f
        lastPocketed = 0
        _state.update { PoolUiState(phase = PoolPhase.PLAYING, level = level, wins = it.wins, losses = it.losses, goldEarned = it.goldEarned) }
        frame++
    }

    fun rematch() = start(_state.value.level)

    fun backToChoosing() {
        botJob?.cancel()
        _state.update { it.copy(phase = PoolPhase.CHOOSING) }
    }

    // ------------------------------------------------------------ Girdi (masa koordinati)

    fun aimAt(x: Float, y: Float) {
        if (!_state.value.humanTurn) return
        val c = game.table.cue
        if (hypot(x - c.x, y - c.y) < 0.01f) return
        aim = atan2(y - c.y, x - c.x)
        frame++
    }

    /** Ince ayar (radyan) */
    fun nudge(d: Float) {
        if (!_state.value.humanTurn) return
        aim += d
        frame++
    }

    fun canMoveCue(x: Float, y: Float): Boolean {
        val c = game.table.cue
        return _state.value.humanTurn && game.ballInHand && hypot(x - c.x, y - c.y) < PoolTable.R * 3f
    }

    fun moveCue(x: Float, y: Float) {
        if (game.placeCue(x, y)) frame++
    }

    fun setPower(p: Float) {
        if (!_state.value.humanTurn) return
        power = p.coerceIn(0f, 1f)
        frame++
    }

    fun release() {
        val p = power
        power = 0f
        if (!_state.value.humanTurn || p < 0.03f) {
            frame++
            return
        }
        shoot(aim, p)
    }

    private fun shoot(angle: Float, p: Float) {
        game.shoot(angle, p)
        lastPocketed = game.table.balls.count { it.pocketed }
        _state.update { it.copy(shooting = true, ballInHand = false, event = PoolEvent.NONE) }
        feedback.play(Sfx.BLOCK, volume = 0.3f + 0.7f * p)
        feedback.vibrate(Haptic.LIGHT)
    }

    // ------------------------------------------------------------ Dongu

    fun onFrame(dt: Float) {
        if (!_state.value.shooting) return
        val done = game.update(dt.coerceAtMost(0.033f))
        frame++
        val pocketed = game.table.balls.count { it.pocketed }
        if (pocketed > lastPocketed) {
            feedback.play(Sfx.DROP, volume = 0.8f)
            lastPocketed = pocketed
        }
        if (!done) return
        val g = game
        _state.update {
            it.copy(shooting = false, turn = g.turn, groups = g.groups.toList(), ballInHand = g.ballInHand, event = g.lastEvent, winner = g.winner)
        }
        when (g.lastEvent) {
            PoolEvent.FOUL -> feedback.vibrate(Haptic.HEAVY)
            PoolEvent.CONTINUE, PoolEvent.GROUPS_SET -> feedback.play(Sfx.CLEAR, volume = 0.5f)
            else -> Unit
        }
        if (g.over) finish() else {
            // Siradaki oyuncu icin nisan: en yakin hedefe
            val target = g.targetsFor(g.turn).map { g.table.balls[it] }.minByOrNull { hypot(it.x - g.table.cue.x, it.y - g.table.cue.y) }
            if (target != null) aim = atan2(target.y - g.table.cue.y, target.x - g.table.cue.x)
            maybeBot()
        }
    }

    private fun maybeBot() {
        val s = _state.value
        val level = s.level ?: return
        if (s.phase != PoolPhase.PLAYING || game.turn != 1 || game.over) return
        _state.update { it.copy(botThinking = true) }
        botJob = viewModelScope.launch {
            delay(600)
            val plan = withContext(Dispatchers.Default) { PoolAi.choose(game, 1, level.noiseDeg) }
            // Nisani yavasca cevir
            val from = aim
            var to = plan.angle
            while (to - from > Math.PI) to -= (2 * Math.PI).toFloat()
            while (to - from < -Math.PI) to += (2 * Math.PI).toFloat()
            for (k in 1..20) {
                aim = from + (to - from) * k / 20f
                frame++
                delay(25)
            }
            _state.update { it.copy(ballInHand = false) }
            for (k in 1..12) {
                power = plan.power * k / 12f
                frame++
                delay(30)
            }
            delay(150)
            power = 0f
            _state.update { it.copy(botThinking = false) }
            shoot(plan.angle, plan.power)
        }
    }

    private fun finish() {
        val s = _state.value
        var score = 0
        var wins = s.wins
        var losses = s.losses
        if (s.level != null) {
            if (game.winner == 0) {
                wins++
                score = s.level.winScore
            } else {
                losses++
            }
        }
        if (score > 0) {
            sessionScore += score
            val delta = goldTracker.onScore(sessionScore)
            viewModelScope.launch {
                if (delta > 0) prefs.addGold(delta)
                prefs.submitScore(GAME_ID, score)
            }
        }
        _state.update { it.copy(phase = PoolPhase.FINISHED, wins = wins, losses = losses, finalScore = score, goldEarned = goldTracker.awarded) }
        feedback.play(if (game.winner == 0 || s.level == null) Sfx.COMBO else Sfx.GAME_OVER)
        feedback.vibrate(Haptic.HEAVY)
    }

    companion object {
        const val GAME_ID = "pool"
    }
}
