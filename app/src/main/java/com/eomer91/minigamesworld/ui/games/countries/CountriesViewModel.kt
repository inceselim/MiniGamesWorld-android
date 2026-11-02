package com.eomer91.minigamesworld.ui.games.countries

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

enum class CwPhase { LOADING, SETUP, PLAYING }
enum class CwTab { OVERVIEW, BUILD, ARMY, WAR, MARKET }
enum class CwNotice { NOT_ENOUGH_RESOURCES, NO_BARRACKS_SPACE, NOT_ENOUGH_POPULATION, MAX_LEVEL, PEACE_TIME, COOLDOWN, NO_UNITS, GEMS_ADDED }

data class CwUiState(
    val phase: CwPhase = CwPhase.LOADING,
    val game: CwGame? = null,
    val tab: CwTab = CwTab.OVERVIEW,
    val paused: Boolean = false,
    val turnProgress: Float = 0f,
    // Kurulum
    val setupDifficulty: CwDifficulty = CwDifficulty.NORMAL,
    val setupName: String = "",
    val setupPolity: Polity = Polity.DEMOCRACY,
    // Savas hazirligi
    val targetId: Int? = null,
    val sent: Map<String, Int> = emptyMap(),
    /** Gosterilen savas raporu (diyalog). */
    val report: BattleReport? = null,
    val notice: CwNotice? = null,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = game != null && game.over && bestAtStart >= 0 && game.score > bestAtStart
}

@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    val engine = CountriesEngine()
    private val goldTracker = GoldTracker()
    private var timer = 0f

    private val _state = MutableStateFlow(CwUiState())
    val state: StateFlow<CwUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = prefs.savedGame(GAME_ID).first()?.let(CwGame::fromJson)?.takeIf { !it.over }
            _state.update {
                if (saved != null) it.copy(phase = CwPhase.PLAYING, game = saved, paused = true)
                else it.copy(phase = CwPhase.SETUP)
            }
        }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    // ---------------------------------------------------------- Kurulum

    fun setDifficulty(d: CwDifficulty) = _state.update { it.copy(setupDifficulty = d) }
    fun setName(n: String) = _state.update { it.copy(setupName = n.take(20)) }
    fun setSetupPolity(p: Polity) = _state.update { it.copy(setupPolity = p) }

    fun startGame(defaultName: String) {
        val s = _state.value
        goldTracker.reset()
        timer = 0f
        val g = engine.newGame(s.setupDifficulty, s.setupName.ifBlank { defaultName }, s.setupPolity)
        _state.update {
            it.copy(phase = CwPhase.PLAYING, game = g, tab = CwTab.OVERVIEW, paused = false, turnProgress = 0f,
                targetId = null, sent = emptyMap(), report = null, goldEarned = 0, bestAtStart = it.best)
        }
        persist(g)
        feedback.play(Sfx.CLEAR, volume = 0.6f)
    }

    /** Yeni oyun: kurulum ekranina don. */
    fun restart() {
        _state.update { it.copy(phase = CwPhase.SETUP, game = null, report = null, goldEarned = 0) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    // ---------------------------------------------------------- Zaman

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }
    fun setTab(t: CwTab) = _state.update { it.copy(tab = t) }
    fun consumeNotice() = _state.update { it.copy(notice = null) }
    fun dismissReport() = _state.update { it.copy(report = null) }

    fun onFrame(dt: Float) {
        val s = _state.value
        val g = s.game ?: return
        if (s.phase != CwPhase.PLAYING || s.paused || g.over || s.report != null) return
        timer += dt
        if (timer >= CountriesEngine.TURN_SECONDS) {
            timer = 0f
            val next = engine.tick(g)
            val botBattle = next.lastBattle?.takeIf { it !== g.lastBattle && it.defender == g.player.name }
            _state.value = s.copy(game = next, turnProgress = 0f, report = botBattle ?: s.report)
            if (botBattle != null) {
                feedback.play(if (botBattle.won) Sfx.GAME_OVER else Sfx.COMBO)
                feedback.vibrate(Haptic.HEAVY)
            }
            afterChange(next)
        } else {
            _state.value = s.copy(turnProgress = timer / CountriesEngine.TURN_SECONDS)
        }
    }

    // ---------------------------------------------------------- Eylemler

    private fun apply(result: CwResult, sfx: Sfx = Sfx.BLOCK): Boolean {
        val notice = when (result) {
            is CwResult.Ok -> {
                _state.update { it.copy(game = result.game) }
                feedback.play(sfx, volume = 0.7f)
                feedback.vibrate(Haptic.LIGHT)
                afterChange(result.game)
                return true
            }
            CwResult.NotEnoughResources -> CwNotice.NOT_ENOUGH_RESOURCES
            CwResult.NoBarracksSpace -> CwNotice.NO_BARRACKS_SPACE
            CwResult.NotEnoughPopulation -> CwNotice.NOT_ENOUGH_POPULATION
            CwResult.MaxLevel -> CwNotice.MAX_LEVEL
            CwResult.PeaceTime -> CwNotice.PEACE_TIME
            CwResult.Cooldown -> CwNotice.COOLDOWN
            CwResult.Invalid -> CwNotice.NO_UNITS
        }
        _state.update { it.copy(notice = notice) }
        feedback.play(Sfx.SWIPE, volume = 0.4f)
        return false
    }

    fun build(b: Building) {
        val g = _state.value.game ?: return
        apply(engine.build(g, b))
    }

    fun recruit(u: UnitType, n: Int) {
        val g = _state.value.game ?: return
        apply(engine.recruit(g, u, n), Sfx.DROP)
    }

    fun recruitMax(u: UnitType) {
        val g = _state.value.game ?: return
        val n = engine.maxRecruit(g.player, u)
        if (n <= 0) {
            // Nedenini gostermek icin 1 tane dene
            apply(engine.recruit(g, u, 1))
            return
        }
        apply(engine.recruit(g, u, n), Sfx.DROP)
    }

    fun setTax(rate: Int) {
        val g = _state.value.game ?: return
        val next = engine.setTax(g, rate)
        _state.update { it.copy(game = next) }
        persist(next)
    }

    fun setFocus(f: Focus) {
        val g = _state.value.game ?: return
        val next = engine.setFocus(g, f)
        _state.update { it.copy(game = next) }
        persist(next)
        feedback.play(Sfx.SWIPE, volume = 0.4f)
    }

    fun setPolity(p: Polity) {
        val g = _state.value.game ?: return
        apply(engine.setPolity(g, p))
    }

    fun activatePremium() {
        val g = _state.value.game ?: return
        apply(engine.activatePremium(g), Sfx.COMBO)
    }

    /** Odullu reklam sonrasi elmas. */
    fun adGems() {
        val g = _state.value.game ?: return
        val next = engine.addGems(g, CountriesEngine.AD_GEMS)
        _state.update { it.copy(game = next, notice = CwNotice.GEMS_ADDED) }
        persist(next)
        feedback.play(Sfx.COMBO)
    }

    fun trade(r: Resource, lots: Int, selling: Boolean) {
        val g = _state.value.game ?: return
        apply(engine.trade(g, r, lots, selling), Sfx.DROP)
    }

    // ---------------------------------------------------------- Savas

    fun selectTarget(id: Int) = _state.update { it.copy(targetId = id) }

    fun changeSent(u: UnitType, delta: Int) = _state.update {
        val have = it.game?.player?.count(u) ?: 0
        val now = (it.sent[u.name] ?: 0) + delta
        it.copy(sent = it.sent + (u.name to now.coerceIn(0, have)))
    }

    fun sendAll() = _state.update { s ->
        val p = s.game?.player ?: return@update s
        s.copy(sent = UnitType.entries.associate { it.name to p.count(it) })
    }

    fun clearSent() = _state.update { it.copy(sent = emptyMap()) }

    fun attack() {
        val s = _state.value
        val g = s.game ?: return
        val target = s.targetId ?: return
        val result = engine.attack(g, target, s.sent)
        if (result is CwResult.Ok) {
            val report = result.game.lastBattle
            _state.update { it.copy(game = result.game, report = report, sent = emptyMap(), targetId = null) }
            feedback.play(if (report?.won == true) Sfx.COMBO else Sfx.GAME_OVER)
            feedback.vibrate(Haptic.HEAVY)
            afterChange(result.game)
        } else {
            apply(result)
        }
    }

    // ---------------------------------------------------------- Kayit / bitis

    private fun afterChange(g: CwGame) {
        if (g.over) finish(g) else persist(g)
    }

    private fun finish(g: CwGame) {
        val score = g.score
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        _state.update { it.copy(goldEarned = goldTracker.awarded) }
        viewModelScope.launch {
            prefs.submitScore(GAME_ID, score)
            prefs.clearSavedGame(GAME_ID)
        }
        if (g.victory == true) feedback.play(Sfx.CLEAR) else feedback.play(Sfx.GAME_OVER)
    }

    private fun persist(g: CwGame) {
        viewModelScope.launch { prefs.saveGame(GAME_ID, g.toJson()) }
    }

    companion object {
        const val GAME_ID = "countries-war"
    }
}
