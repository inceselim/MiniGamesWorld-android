package com.eomer91.minigamesworld.ui.games.stocks

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

/** Al/sat miktar secenekleri. */
enum class Qty { ONE, TEN, HALF, MAX }

enum class StockNotice { NOT_ENOUGH_CASH, NOT_ENOUGH_SHARES }

data class StockUiState(
    val game: MarketGame? = null,
    val selected: StockDef = StockDef.AA,
    val playing: Boolean = false,
    val fast: Boolean = false,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
    val notice: StockNotice? = null,
) {
    val isNewRecord: Boolean get() = game != null && game.over && bestAtStart >= 0 && game.score > bestAtStart
}

@HiltViewModel
class StockMarketViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = StockEngine()
    private val goldTracker = GoldTracker()
    private var dayTimer = 0f

    private val _state = MutableStateFlow(StockUiState())
    val state: StateFlow<StockUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = prefs.savedGame(GAME_ID).first()?.let(MarketGame::fromJson)
            _state.update { it.copy(game = saved?.takeIf { g -> !g.over } ?: engine.newGame()) }
        }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    fun restart() {
        goldTracker.reset()
        dayTimer = 0f
        _state.update { it.copy(game = engine.newGame(), playing = false, goldEarned = 0, bestAtStart = it.best, notice = null) }
        viewModelScope.launch { prefs.clearSavedGame(GAME_ID) }
    }

    fun select(d: StockDef) = _state.update { it.copy(selected = d) }
    fun togglePlay() = _state.update { it.copy(playing = !it.playing && it.game?.over == false) }
    fun toggleSpeed() = _state.update { it.copy(fast = !it.fast) }
    fun consumeNotice() = _state.update { it.copy(notice = null) }

    fun onFrame(dt: Float) {
        val s = _state.value
        if (!s.playing || s.game == null || s.game.over) return
        dayTimer += dt
        val interval = if (s.fast) FAST_DAY_SECONDS else DAY_SECONDS
        if (dayTimer >= interval) {
            dayTimer = 0f
            advance()
        }
    }

    /** Duraklatilmisken elle bir sonraki gune gec. */
    fun nextDay() {
        dayTimer = 0f
        advance()
    }

    private fun advance() {
        val s = _state.value
        val g = s.game ?: return
        if (g.over) return
        val next = engine.nextDay(g)
        _state.value = s.copy(game = next)
        val fresh = next.news.takeWhile { it.day == next.day }
        if (fresh.isNotEmpty()) feedback.play(Sfx.SWIPE, volume = 0.35f)
        if (next.over) finish(next) else persist(next)
    }

    private fun finish(g: MarketGame) {
        val score = g.score
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        _state.update { it.copy(playing = false, goldEarned = goldTracker.awarded) }
        viewModelScope.launch {
            prefs.submitScore(GAME_ID, score)
            prefs.clearSavedGame(GAME_ID)
        }
        val profit = g.netWorth > StockEngine.START_CASH
        feedback.play(if (profit) Sfx.COMBO else Sfx.GAME_OVER)
        feedback.vibrate(Haptic.HEAVY)
    }

    fun quantity(qty: Qty, buying: Boolean): Int {
        val s = _state.value
        val g = s.game ?: return 0
        val limit = if (buying) engine.maxBuy(g, s.selected) else g.stock(s.selected).owned
        return when (qty) {
            Qty.ONE -> 1
            Qty.TEN -> 10
            Qty.HALF -> limit / 2
            Qty.MAX -> limit
        }.coerceAtMost(limit)
    }

    fun buy(qty: Qty) = trade(qty, buying = true)
    fun sell(qty: Qty) = trade(qty, buying = false)

    private fun trade(qty: Qty, buying: Boolean) {
        val s = _state.value
        val g = s.game ?: return
        val n = quantity(qty, buying)
        val result = if (buying) engine.buy(g, s.selected, n) else engine.sell(g, s.selected, n)
        when (result) {
            is TradeResult.Ok -> {
                _state.value = s.copy(game = result.game)
                feedback.play(if (buying) Sfx.DROP else Sfx.BLOCK, volume = 0.7f)
                feedback.vibrate(Haptic.LIGHT)
                persist(result.game)
            }
            TradeResult.NotEnoughCash -> _state.value = s.copy(notice = StockNotice.NOT_ENOUGH_CASH)
            TradeResult.NotEnoughShares -> _state.value = s.copy(notice = StockNotice.NOT_ENOUGH_SHARES)
        }
    }

    /** Odullu reklam: 5 gun trendleri goster (oyun basina bir kez). */
    fun useTip() {
        val g = _state.value.game ?: return
        if (g.tipDay >= 0) return
        val next = engine.useTip(g)
        _state.update { it.copy(game = next) }
        persist(next)
        feedback.play(Sfx.CLEAR, volume = 0.6f)
    }

    private fun persist(g: MarketGame) {
        viewModelScope.launch { prefs.saveGame(GAME_ID, g.toJson()) }
    }

    companion object {
        const val GAME_ID = "stock-market"
        const val DAY_SECONDS = 2.2f
        const val FAST_DAY_SECONDS = 1.0f
    }
}
