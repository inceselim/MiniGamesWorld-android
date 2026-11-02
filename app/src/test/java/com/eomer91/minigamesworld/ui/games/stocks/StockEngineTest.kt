package com.eomer91.minigamesworld.ui.games.stocks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StockEngineTest {
    private val engine = StockEngine(Random(5))

    @Test
    fun gameEndsAfterAllDays() {
        var g = engine.newGame()
        repeat(StockEngine.DAYS + 5) { g = engine.nextDay(g) }
        assertTrue(g.over)
        assertEquals(StockEngine.DAYS, g.day)
        g.stocks.forEach { assertTrue(it.price >= StockEngine.MIN_PRICE) }
    }

    @Test
    fun buyAndSellKeepMoneyConsistent() {
        val g = engine.newGame()
        val max = engine.maxBuy(g, StockDef.AA)
        val bought = (engine.buy(g, StockDef.AA, max) as TradeResult.Ok).game
        assertTrue(bought.cash < g.stock(StockDef.AA).price * 1.01f)
        assertEquals(max, bought.stock(StockDef.AA).owned)
        assertEquals(TradeResult.NotEnoughCash, engine.buy(bought, StockDef.CCA, 1000))
        val sold = (engine.sell(bought, StockDef.AA, max) as TradeResult.Ok).game
        // Ayni fiyattan al-sat: sadece komisyon kaybi
        assertEquals(StockEngine.START_CASH * (1 - StockEngine.FEE) / (1 + StockEngine.FEE), sold.cash, 6f)
        assertEquals(TradeResult.NotEnoughShares, engine.sell(sold, StockDef.AA, 1))
    }

    @Test
    fun jsonRoundTrip() {
        var g = engine.newGame()
        repeat(10) { g = engine.nextDay(g) }
        assertEquals(g, MarketGame.fromJson(g.toJson()))
        assertFalse(g.over)
    }
}
