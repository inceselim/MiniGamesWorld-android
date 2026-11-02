package com.eomer91.minigamesworld.ui.games.connect4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Connect4EngineTest {

    private val engine = Connect4Engine(Random(2))

    @Test
    fun piecesFallToBottom() {
        var b = engine.empty()
        b = engine.play(b, 3, 1)
        b = engine.play(b, 3, 2)
        assertEquals(1, b[engine.index(5, 3)])
        assertEquals(2, b[engine.index(4, 3)])
    }

    @Test
    fun detectsAllDirections() {
        var h = engine.empty()
        (0..3).forEach { h = engine.play(h, it, 1) }
        assertEquals(1, engine.winner(h))

        var v = engine.empty()
        repeat(4) { v = engine.play(v, 6, 2) }
        assertEquals(2, engine.winner(v))
        assertNotNull(engine.winningLine(v))

        // Capraz: 1 oyuncusu (0,5)-(1,4)-(2,3)-(3,2)
        var d = engine.empty()
        listOf(0 to 1, 1 to 2, 1 to 1, 2 to 2, 2 to 2, 2 to 1, 3 to 2, 3 to 2, 3 to 2, 3 to 1).forEach { (c, p) -> d = engine.play(d, c, p) }
        assertEquals(1, engine.winner(d))
    }

    @Test
    fun columnFull() {
        var b = engine.empty()
        repeat(6) { b = engine.play(b, 0, if (it % 2 == 0) 1 else 2) }
        assertFalse(engine.canPlay(b, 0))
    }

    @Test
    fun botTakesWinAndBlocks() {
        BotLevel.entries.forEach { level ->
            // Bot 3'lu yapmis: kazanmali
            var b = engine.empty()
            listOf(0 to 2, 0 to 1, 1 to 2, 1 to 1, 2 to 2, 6 to 1).forEach { (c, p) -> b = engine.play(b, c, p) }
            assertEquals(3, engine.botMove(b, level))
        }
        // Normal/Zor rakibin 3'lusunu engeller
        listOf(BotLevel.NORMAL, BotLevel.HARD).forEach { level ->
            var b = engine.empty()
            listOf(0 to 1, 6 to 2, 1 to 1, 6 to 2, 2 to 1, 5 to 2).forEach { (c, p) -> b = engine.play(b, c, p) }
            assertEquals(3, engine.botMove(b, level))
        }
    }

    @Test
    fun hardBotIsFastEnough() {
        val start = System.currentTimeMillis()
        engine.botMove(engine.empty(), BotLevel.HARD)
        assertTrue(System.currentTimeMillis() - start < 3_000)
    }
}
