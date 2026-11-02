package com.eomer91.minigamesworld.ui.games.match3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Match3EngineTest {

    private val engine = Match3Engine(Random(1))

    private fun boardOf(vararg rows: String): GemBoard {
        var id = 1000L
        return rows.flatMap { line -> line.map { ch -> Gem(id++, ch.digitToInt()) } }
    }

    @Test
    fun newBoard_hasNoMatchesButHasMoves() {
        repeat(20) {
            val b = engine.newBoard()
            assertEquals(64, b.size)
            assertTrue(engine.findMatches(b).cells.isEmpty())
            assertTrue(engine.hasPossibleMove(b))
        }
    }

    @Test
    fun findMatches_rowAndColumn() {
        val b = boardOf(
            "00012345",
            "12345012",
            "23450123",
            "34501234",
            "45012345",
            "50123450",
            "01234501",
            "12345012",
        )
        val m = engine.findMatches(b)
        assertEquals(setOf(0, 1, 2), m.cells)
        assertEquals(listOf(3), m.runs)
        assertEquals(15, engine.scoreFor(m, chain = 1))
        assertEquals(30, engine.scoreFor(m, chain = 2))
    }

    @Test
    fun collapse_dropsGemsAndRefillsFromAbove() {
        val b = engine.newBoard()
        val col = 3
        val cleared = setOf(engine.index(7, col), engine.index(6, col), engine.index(5, col))
        val above = b[engine.index(4, col)]!!
        val out = engine.collapse(b, cleared)

        assertTrue(out.all { it != null })
        assertEquals(above.id, out[engine.index(7, col)]!!.id)
        // Ustteki 3 yeni tas tahtanin ustunden gelir
        (0..2).forEach { r -> assertTrue(out[engine.index(r, col)]!!.spawnRow < 0) }
        // Diger sutunlar degismez
        assertEquals(b[engine.index(7, 0)]!!.id, out[engine.index(7, 0)]!!.id)
    }

    @Test
    fun adjacency() {
        assertTrue(engine.areAdjacent(0, 1))
        assertTrue(engine.areAdjacent(0, 8))
        assertFalse(engine.areAdjacent(7, 8))
        assertFalse(engine.areAdjacent(0, 9))
    }

    @Test
    fun shuffle_keepsGemsAndHasMove() {
        val b = engine.newBoard()
        val s = engine.shuffle(b)
        assertEquals(b.map { it!!.id }.toSet(), s.map { it!!.id }.toSet())
        assertNotNull(engine.findPossibleMove(s))
    }
}
