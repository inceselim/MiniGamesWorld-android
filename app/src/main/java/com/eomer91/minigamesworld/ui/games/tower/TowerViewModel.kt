package com.eomer91.minigamesworld.ui.games.tower

import android.os.SystemClock
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TowerPhase { WORKSHOP, PLAYING, GAME_OVER }

data class TowerUiState(
    val phase: TowerPhase = TowerPhase.WORKSHOP,
    val run: TowerRun? = null,
    val permanent: Map<TowerStat, Int> = emptyMap(),
    val perks: Map<EconomyPerk, Int> = emptyMap(),
    val gold: Int = 0,
    /** 1 ya da 2 (oyun hizi). */
    val speed: Int = 1,
    val reviveUsed: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = (run?.score ?: 0) > 0 && bestAtStart >= 0 && (run?.score ?: 0) > bestAtStart
}

@HiltViewModel
class TowerViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(TowerUiState())
    val state: StateFlow<TowerUiState> = _state.asStateFlow()

    /** Kosu basinda kalici seviyelerle kurulur. */
    var engine: TowerEngine = TowerEngine(emptyMap(), emptyMap())
        private set

    private val goldTracker = GoldTracker()
    private var lastKillSound = 0L
    private var lastHitBuzz = 0L

    init {
        viewModelScope.launch {
            combine(prefs.towerUpgradeLevels, prefs.gold, prefs.highScore(GAME_ID)) { levels, gold, best -> Triple(levels, gold, best) }
                .collect { (levels, gold, best) ->
                    _state.update {
                        it.copy(
                            permanent = TowerStat.entries.associateWith { s -> levels[s.name] ?: 0 },
                            perks = EconomyPerk.entries.associateWith { p -> levels[p.name] ?: 0 },
                            gold = gold,
                            best = best,
                            bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart,
                        )
                    }
                }
        }
    }

    // ---------- Atolye ----------

    fun permanentCost(level: Int) = TowerBalance.permanentCost(level)

    fun buyPermanent(name: String, currentLevel: Int) {
        if (currentLevel >= TowerBalance.MAX_PERMANENT_LEVEL) return
        viewModelScope.launch {
            val ok = prefs.buyTowerUpgrade(name, permanentCost(currentLevel), TowerBalance.MAX_PERMANENT_LEVEL)
            if (ok) feedback.play(Sfx.BLOCK) else feedback.play(Sfx.SWIPE, volume = 0.4f)
        }
    }

    /** Odullu reklam izlendi: altin harcamadan bir seviye yukselt. */
    fun upgradeWithAd(name: String, currentLevel: Int) {
        if (currentLevel >= TowerBalance.MAX_PERMANENT_LEVEL) return
        viewModelScope.launch {
            val ok = prefs.buyTowerUpgrade(name, cost = 0, maxLevel = TowerBalance.MAX_PERMANENT_LEVEL)
            if (ok) feedback.play(Sfx.COMBO)
        }
    }

    fun startRun() {
        val s = _state.value
        engine = TowerEngine(s.permanent, s.perks)
        goldTracker.reset()
        _state.update {
            it.copy(phase = TowerPhase.PLAYING, run = engine.newRun(), reviveUsed = false, goldEarned = 0, bestAtStart = it.best)
        }
    }

    fun backToWorkshop() {
        val run = _state.value.run
        if (run != null) submitScore(run.score)
        _state.update { it.copy(phase = TowerPhase.WORKSHOP, run = null) }
    }

    // ---------- Oyun ----------

    fun toggleSpeed() {
        _state.update { it.copy(speed = if (it.speed == 1) 2 else 1) }
    }

    fun buyRunUpgrade(stat: TowerStat) {
        val s = _state.value
        val run = s.run ?: return
        val next = engine.buyRunUpgrade(run, stat)
        if (next !== run) {
            _state.value = s.copy(run = next)
            feedback.play(Sfx.BLOCK, volume = 0.7f)
        }
    }

    /** Her karede cagrilir. 2x hizda iki kucuk adim atilir (fizik kararli kalsin). */
    fun onFrame(dt: Float) {
        val s = _state.value
        if (s.phase != TowerPhase.PLAYING) return
        var run = s.run ?: return
        val step = dt.coerceAtMost(0.05f)
        var kills = 0
        var hits = 0
        var waveCleared = false
        var bossKilled = false
        repeat(s.speed) {
            val (next, ev) = engine.update(run, step)
            run = next
            kills += ev.kills
            hits += ev.hits
            waveCleared = waveCleared || ev.waveCleared
            bossKilled = bossKilled || ev.bossKilled
        }

        val delta = goldTracker.onScore(run.score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }

        val gameOver = run.gameOver
        _state.value = s.copy(
            run = run,
            phase = if (gameOver) TowerPhase.GAME_OVER else TowerPhase.PLAYING,
            goldEarned = goldTracker.awarded,
        )
        playFeedback(kills, hits, waveCleared, bossKilled, gameOver)
        if (waveCleared || gameOver) submitScore(run.score)
    }

    /** Odullu reklam sonrasi: yari canla devam (kosu basina bir kez). */
    fun revive() {
        _state.update {
            val run = it.run
            if (it.phase != TowerPhase.GAME_OVER || it.reviveUsed || run == null) it
            else it.copy(phase = TowerPhase.PLAYING, run = engine.revive(run), reviveUsed = true)
        }
    }

    private fun submitScore(score: Int) {
        if (score > _state.value.best) viewModelScope.launch { prefs.submitScore(GAME_ID, score) }
    }

    private fun playFeedback(kills: Int, hits: Int, waveCleared: Boolean, bossKilled: Boolean, gameOver: Boolean) {
        val now = SystemClock.elapsedRealtime()
        when {
            gameOver -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
            }
            bossKilled -> {
                feedback.play(Sfx.COMBO)
                feedback.vibrate(Haptic.HEAVY)
            }
            waveCleared -> feedback.play(Sfx.CLEAR, volume = 0.6f)
            kills > 0 && now - lastKillSound > KILL_SOUND_GAP_MS -> {
                lastKillSound = now
                feedback.play(Sfx.DROP, volume = 0.35f)
            }
        }
        if (hits > 0 && !gameOver && now - lastHitBuzz > HIT_BUZZ_GAP_MS) {
            lastHitBuzz = now
            feedback.vibrate(Haptic.LIGHT)
        }
    }

    companion object {
        const val GAME_ID = "tower-defense"
        private const val KILL_SOUND_GAP_MS = 120L
        private const val HIT_BUZZ_GAP_MS = 250L
    }
}
