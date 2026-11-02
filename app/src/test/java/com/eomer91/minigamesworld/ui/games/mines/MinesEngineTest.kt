package com.eomer91.minigamesworld.ui.games.mines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MinesEngineTest {

    private val engine = MinesEngine(Random(11))

    @Test
    fun firstTapIsAlwaysSafeAndOpensArea() {
        MinesDifficulty.entries.forEach { d ->
            repeat(20) {
                val b = engine.empty(d)
                val tap = (d.rows / 2) * d.cols + d.cols / 2
                val r = engine.reveal(b, tap)
                assertNull(r.hitMine)
                assertEquals(d.mines, r.board.cells.count { it.mine })
                // Ilk dokunusun komsulari mayinsiz: hucre 0'dir ve en az 9 hucre acilir
                assertEquals(0, r.board.cells[tap].adjacent)
                assertTrue(r.opened >= 9)
            }
        }
    }

    @Test
    fun adjacentCountsAreCorrect() {
        val b = engine.placeMines(engine.empty(MinesDifficulty.NORMAL), 0)
        b.cells.forEachIndexed { i, c ->
            assertEquals(engine.neighbors(b, i).count { b.cells[it].mine }, c.adjacent)
        }
    }

    @Test
    fun flagAndChord() {
        var b = engine.placeMines(engine.empty(MinesDifficulty.EASY), 40)
        // Bir mayinin komsusu olan, sayi tasiyan bir hucre bul
        val numbered = b.cells.indices.first { !b.cells[it].mine && b.cells[it].adjacent > 0 }
        b = engine.reveal(b, numbered).board
        engine.neighbors(b, numbered).filter { b.cells[it].mine }.forEach { b = engine.toggleFlag(b, it) }
        val r = engine.chord(b, numbered)
        assertNull(r.hitMine)
        assertTrue(engine.neighbors(r.board, numbered).all { r.board.cells[it].revealed || r.board.cells[it].flagged })
    }

    @Test
    fun hittingMineAndSecondChance() {
        val b = engine.placeMines(engine.empty(MinesDifficulty.EASY), 0)
        val mine = b.cells.indices.first { b.cells[it].mine }
        val r = engine.reveal(b, mine)
        assertEquals(mine, r.hitMine)
        val again = engine.secondChance(engine.revealMines(r.board), mine)
        assertTrue(again.cells[mine].flagged)
        assertFalse(again.cells.any { it.mine && it.revealed })
    }

    @Test
    fun winWhenAllSafeCellsOpen() {
        var b = engine.placeMines(engine.empty(MinesDifficulty.EASY), 0)
        b.cells.indices.filter { !b.cells[it].mine }.forEach { b = engine.reveal(b, it).board }
        assertTrue(engine.isWon(b))
    }

    @Test
    fun score() {
        assertEquals(800, minesScore(MinesDifficulty.NORMAL, 100, false))
        assertEquals(600, minesScore(MinesDifficulty.NORMAL, 100, true))
        assertEquals(160, minesScore(MinesDifficulty.NORMAL, 99999, false))
    }
}
