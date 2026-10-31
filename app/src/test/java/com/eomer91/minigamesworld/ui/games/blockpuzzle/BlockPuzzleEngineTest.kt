package com.eomer91.minigamesworld.ui.games.blockpuzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPuzzleEngineTest {

    private val engine = BlockPuzzleEngine()
    private val single = BlockPiece(Shape(listOf(0 to 0)), color = 1)
    private val square = BlockPiece(Shape(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)), color = 2)

    @Test
    fun shapes_haveCorrectSize() {
        assertEquals(2, square.shape.rows)
        assertEquals(2, square.shape.cols)
        BlockPuzzleEngine.SHAPES.forEach { assertTrue(it.cells.isNotEmpty()) }
    }

    @Test
    fun canPlace_respectsBoundsAndOccupiedCells() {
        val board = engine.emptyBoard()
        assertTrue(engine.canPlace(board, square.shape, 6, 6))
        assertFalse(engine.canPlace(board, square.shape, 7, 7))
        assertFalse(engine.canPlace(board, square.shape, -1, 0))

        val placed = engine.place(board, single, 3, 3)
        assertFalse(engine.canPlace(placed, square.shape, 2, 2))
        assertEquals(1, placed[3][3])
    }

    @Test
    fun clearLines_clearsRowAndColumnTogether() {
        var board = engine.emptyBoard()
        // 3. satiri ve 5. sutunu doldur (kesisim hucresi ortak)
        for (c in 0 until 8) board = engine.place(board, single, 3, c)
        for (r in 0 until 8) if (r != 3) board = engine.place(board, single, r, 5)

        val result = engine.clearLines(board)
        assertEquals(2, result.linesCleared)
        assertTrue(result.board.flatten().all { it == 0 })
    }

    @Test
    fun gameOver_whenNoPieceFits() {
        // Tahtayi dama deseniyle doldur: hicbir 2'li parca sigmaz, tekli sigar
        val checker = List(8) { r -> List(8) { c -> if ((r + c) % 2 == 0) 1 else 0 } }
        assertTrue(engine.isGameOver(checker, listOf(square, null, null)))
        assertFalse(engine.isGameOver(checker, listOf(square, single, null)))
    }

    @Test
    fun score_placementPlusLines() {
        assertEquals(10, engine.scoreFor(0))
        assertEquals(210, engine.scoreFor(2))
    }
}
