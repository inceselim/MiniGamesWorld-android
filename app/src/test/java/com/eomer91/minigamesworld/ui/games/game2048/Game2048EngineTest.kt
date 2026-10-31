package com.eomer91.minigamesworld.ui.games.game2048

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Game2048EngineTest {

    private val engine = Game2048Engine()

    @Test
    fun mergeLine_mergesPairsOnce() {
        assertEquals(listOf(4, 4, 0, 0) to 8, Game2048Engine.mergeLine(listOf(2, 2, 2, 2)))
        assertEquals(listOf(4, 4, 0, 0) to 4, Game2048Engine.mergeLine(listOf(2, 2, 4, 0)))
        assertEquals(listOf(8, 0, 0, 0) to 8, Game2048Engine.mergeLine(listOf(0, 4, 0, 4)))
        assertEquals(listOf(2, 4, 2, 4) to 0, Game2048Engine.mergeLine(listOf(2, 4, 2, 4)))
    }

    @Test
    fun slide_rightAndUp() {
        val board = listOf(
            listOf(2, 2, 0, 0),
            listOf(0, 0, 0, 0),
            listOf(2, 0, 0, 0),
            listOf(0, 0, 0, 0),
        )
        val right = engine.slide(board, Direction.RIGHT)
        assertEquals(listOf(0, 0, 0, 4), right.board[0])
        assertEquals(listOf(0, 0, 0, 2), right.board[2])
        assertEquals(4, right.gainedScore)

        val up = engine.slide(board, Direction.UP)
        assertEquals(listOf(4, 2, 0, 0), up.board[0])
        assertTrue(up.moved)
    }

    @Test
    fun canMove_falseWhenFullAndNoPairs() {
        val stuck = listOf(
            listOf(2, 4, 2, 4),
            listOf(4, 2, 4, 2),
            listOf(2, 4, 2, 4),
            listOf(4, 2, 4, 2),
        )
        assertFalse(engine.canMove(stuck))
        assertFalse(engine.slide(stuck, Direction.LEFT).moved)
    }

    @Test
    fun move_addsExactlyOneTile() {
        val board = listOf(
            listOf(2, 0, 0, 0),
            listOf(0, 0, 0, 0),
            listOf(0, 0, 0, 0),
            listOf(0, 0, 0, 0),
        )
        val result = engine.move(board, Direction.RIGHT)
        assertEquals(2, result.board.flatten().count { it != 0 })
    }
}
