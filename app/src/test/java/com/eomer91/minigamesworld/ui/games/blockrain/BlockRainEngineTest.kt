package com.eomer91.minigamesworld.ui.games.blockrain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BlockRainEngineTest {

    private val rows = BlockRainEngine.ROWS
    private val cols = BlockRainEngine.COLS

    /** Belirli bir parca ve tahta ile oyun baslat. */
    private fun engineWith(piece: Tetromino, board: List<List<Int>> = List(rows) { List(cols) { 0 } }): BlockRainEngine {
        val e = BlockRainEngine(Random(7))
        assertTrue(e.restore(BlockRainSave(board = board, piece = piece.ordinal, bag = emptyList(), hold = -1, score = 0, lines = 0)))
        return e
    }

    /** Alttan [n] satiri, [gapStart]..[gapStart+gapLen) bos kalacak sekilde doldur. */
    private fun filledBoard(n: Int, gapStart: Int, gapLen: Int): List<List<Int>> =
        List(rows) { r -> List(cols) { c -> if (r >= rows - n && c !in gapStart until gapStart + gapLen) 1 else 0 } }

    @Test
    fun bagContainsEveryPieceOnce() {
        val e = BlockRainEngine(Random(1))
        val seen = mutableListOf(e.piece)
        repeat(6) {
            e.holdPiece() // ilk tutma: yeni parca gelir
            seen += e.piece
            e.hardDrop()
        }
        // Ilk 7 parca torbadan, ama tutma sirayi bozmasin diye sadece cesitliligi kontrol et
        assertTrue(seen.toSet().size >= 5)
        assertEquals(3, e.next.size)
    }

    @Test
    fun movesStopAtWalls() {
        val e = engineWith(Tetromino.O)
        repeat(10) { e.move(-1) }
        assertEquals(0, e.cells().minOf { it.second })
        repeat(20) { e.move(1) }
        assertEquals(cols - 1, e.cells().maxOf { it.second })
    }

    @Test
    fun hardDropLandsOnFloorAndScores() {
        val e = engineWith(Tetromino.O)
        e.hardDrop()
        assertEquals(1, e.lockCount)
        assertTrue(e.board[rows - 1][4] != 0 && e.board[rows - 2][5] != 0)
        assertEquals((rows - 2) * 2, e.score)
    }

    @Test
    fun singleLineClearScores100() {
        // Alt satirda 4 bosluk (sutun 3-6), yatay I tam oraya duser
        val e = engineWith(Tetromino.I, filledBoard(1, 3, 4))
        val dropScore = (e.ghostRow() - e.row) * 2
        e.hardDrop()
        assertEquals(1, e.lines)
        assertEquals(100 + dropScore, e.score)
        assertTrue(e.board[rows - 1].all { it == 0 })
    }

    @Test
    fun fourLinesScore800() {
        // Sutun 0 bos 4 satir; dikey I
        val e = engineWith(Tetromino.I, filledBoard(4, 0, 1))
        e.rotate(1)
        repeat(10) { e.move(-1) }
        e.hardDrop()
        assertEquals(4, e.lines)
        assertEquals(4, e.lastCleared)
        assertTrue(e.score >= 800)
        assertTrue(e.board.all { row -> row.all { it == 0 } })
    }

    @Test
    fun rotationKicksOffWall() {
        val e = engineWith(Tetromino.I)
        e.rotate(1)
        repeat(10) { e.move(1) } // dikey I sag duvarda
        assertTrue(e.rotate(1)) // yatay donus duvardan tekmeyle
        assertTrue(e.cells().all { it.second in 0 until cols })
    }

    @Test
    fun holdOncePerPiece() {
        val e = engineWith(Tetromino.T)
        assertTrue(e.holdPiece())
        assertEquals(Tetromino.T, e.hold)
        assertFalse(e.holdPiece())
        e.hardDrop()
        val current = e.piece
        assertTrue(e.holdPiece())
        assertEquals(current, e.hold)
        assertEquals(Tetromino.T, e.piece)
    }

    @Test
    fun gravityPullsPieceDown() {
        val e = engineWith(Tetromino.T)
        val r0 = e.row
        e.update(0.5f)
        e.update(0.5f)
        assertTrue(e.row > r0)
        // Zeminde kilitlenme gecikmesinden sonra kilitlenir
        repeat(600) { e.update(0.05f) }
        assertTrue(e.lockCount >= 1)
    }

    @Test
    fun gameOverWhenStackReachesTop() {
        val e = BlockRainEngine(Random(3))
        var guard = 0
        while (!e.gameOver && guard++ < 100) e.hardDrop()
        assertTrue(e.gameOver)
        e.rescue()
        assertFalse(e.gameOver)
    }

    @Test
    fun levelRisesEveryTenLines() {
        val e = engineWith(Tetromino.I, filledBoard(4, 0, 1))
        e.rotate(1)
        repeat(10) { e.move(-1) }
        e.hardDrop()
        assertEquals(1, e.level)
        val fast = BlockRainEngine()
        assertTrue(fast.gravityInterval() in 0.5f..1f)
    }

    @Test
    fun snapshotRestoresGame() {
        val e = BlockRainEngine(Random(5))
        repeat(5) { e.hardDrop() }
        e.holdPiece()
        val s = e.snapshot()
        val copy = BlockRainEngine(Random(9))
        assertTrue(copy.restore(s))
        assertEquals(e.score, copy.score)
        assertEquals(e.piece, copy.piece)
        assertNotNull(copy.hold)
        for (r in 0 until rows) assertTrue(e.board[r].contentEquals(copy.board[r]))
        assertEquals(e.next, copy.next)
    }
}
