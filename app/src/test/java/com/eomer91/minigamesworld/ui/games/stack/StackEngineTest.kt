package com.eomer91.minigamesworld.ui.games.stack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StackEngineTest {

    private val engine = StackEngine()

    @Test
    fun perfectDrop_keepsWidthAndCountsCombo() {
        val g = engine.newGame().let { it.copy(movingX = it.top.x + 1f) }
        val (next, result) = engine.drop(g)
        assertEquals(DropResult.PERFECT, result)
        assertEquals(StackEngine.START_WIDTH, next.top.width, 0.001f)
        assertEquals(1, next.combo)
        assertTrue(next.debris.isEmpty())
    }

    @Test
    fun partialDrop_cutsOverhang() {
        val g = engine.newGame().let { it.copy(movingX = it.top.x + 15f) }
        val (next, result) = engine.drop(g)
        assertEquals(DropResult.PLACED, result)
        assertEquals(StackEngine.START_WIDTH - 15f, next.top.width, 0.001f)
        assertEquals(1, next.debris.size)
        assertEquals(15f, next.debris.first().width, 0.001f)
        assertEquals(0, next.combo)
    }

    @Test
    fun miss_endsGame() {
        val g = engine.newGame().let { it.copy(movingX = it.top.right + 1f) }
        val (next, result) = engine.drop(g)
        assertEquals(DropResult.MISSED, result)
        assertTrue(next.gameOver)
    }

    @Test
    fun update_bouncesAndStaysInRange() {
        var g = engine.newGame()
        repeat(500) { g = engine.update(g, 0.05f) }
        assertTrue(g.movingX >= -g.movingWidth && g.movingX <= StackEngine.BOARD_WIDTH)
    }

    @Test
    fun comboGrowsBlock() {
        var g = engine.newGame().let { base ->
            // genisligi dusur ki buyume gorulebilsin
            base.copy(blocks = listOf(base.top.copy(width = 40f)), movingWidth = 40f)
        }
        repeat(3) { g = engine.drop(g.copy(movingX = g.top.x)).first }
        assertTrue(g.top.width > 40f)
    }
}
