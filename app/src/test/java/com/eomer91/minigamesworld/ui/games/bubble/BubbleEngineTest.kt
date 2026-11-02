package com.eomer91.minigamesworld.ui.games.bubble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.random.Random

class BubbleEngineTest {

    private val engine = BubbleEngine(Random(3))

    private fun board(vararg cells: Pair<Slot, Int>, shift: Int = 0): BubbleBoard {
        var id = 100L
        return BubbleBoard(cells.associate { (s, c) -> s to Bubble(id++, c) }, shift)
    }

    @Test
    fun rowsAlternateWidth() {
        val b = engine.newBoard(4)
        assertEquals(8, engine.colsIn(b, 0))
        assertEquals(7, engine.colsIn(b, 1))
        assertEquals(8 + 7 + 8 + 7, b.cells.size)
    }

    @Test
    fun neighborsTouchEachOther() {
        val b = engine.newBoard(6)
        // Her komsu merkez uzakligi ~1 (balon capi)
        val s = Slot(2, 3)
        engine.neighbors(b, s).forEach { n ->
            assertEquals(1f, engine.center(b, s).dist(engine.center(b, n)), 0.01f)
        }
        assertEquals(6, engine.neighbors(b, s).size)
    }

    @Test
    fun straightShot_landsUnderTargetAndPopsGroup() {
        // 0. satirda ortada iki kirmizi; ustune duz atilan kirmizi ucluyu tamamlar
        val b = board(Slot(0, 3) to 0, Slot(0, 4) to 0, Slot(0, 0) to 1)
        val r = engine.shoot(b, angle = (PI / 2).toFloat(), color = 0)
        assertEquals(1, r.landed.row)
        assertEquals(3, r.popped.size)
        assertFalse(r.board.cells.values.any { it.color == 0 })
        assertEquals(3 * BubbleEngine.POINTS_POP, r.points)
    }

    @Test
    fun floatingBubblesDrop() {
        // 0. satirda kirmizi ikili; altlarinda (1. satir) asili bir mavi
        val b = board(Slot(0, 3) to 0, Slot(0, 4) to 0, Slot(1, 3) to 2)
        val group = engine.sameColorGroup(b.copy(cells = b.cells + (Slot(0, 2) to Bubble(999, 0))), Slot(0, 2))
        assertEquals(3, group.size)
        val afterPop = b.copy(cells = b.cells - setOf(Slot(0, 3), Slot(0, 4)))
        assertTrue(Slot(1, 3) !in engine.attachedToTop(afterPop))
    }

    @Test
    fun addRow_keepsPositionsConsistent() {
        val b = engine.newBoard(3)
        val s = Slot(1, 2)
        val before = engine.center(b, s)
        val after = engine.addRow(b)
        val moved = engine.center(after, Slot(2, 2))
        assertEquals(before.x, moved.x, 0.001f)
        assertEquals(after.cells[Slot(2, 2)]!!.id, b.cells[s]!!.id)
    }

    @Test
    fun gameOverAtDangerRow() {
        val b = board(Slot(BubbleEngine.DANGER_ROW, 0) to 1)
        assertTrue(engine.isGameOver(b))
        assertFalse(engine.isGameOver(engine.newBoard(5)))
    }
}
