package com.eomer91.minigamesworld.ui.games.checkers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CheckersRulesTest {

    private fun sq(r: Int, c: Int) = r * 8 + c

    private fun board(vararg pieces: Pair<Int, Int>): IntArray {
        val b = IntArray(64)
        pieces.forEach { (i, v) -> b[i] = v }
        return b
    }

    @Test
    fun initialPositionHas16Each() {
        val b = CheckersRules.initialBoard()
        assertEquals(16, CheckersRules.count(b, CheckersRules.WHITE))
        assertEquals(16, CheckersRules.count(b, CheckersRules.BLACK))
        // Beyaz: on siradaki 8 tas ileri gidebilir, yana gidemez (yanlar dolu)
        val moves = CheckersRules.legalMoves(b, CheckersRules.WHITE)
        assertEquals(8, moves.size)
        assertTrue(moves.all { it.to / 8 == 4 })
    }

    @Test
    fun manMovesForwardAndSidewaysNotBack() {
        val b = board(sq(4, 4) to 1)
        val tos = CheckersRules.legalMoves(b, CheckersRules.WHITE).map { it.to }.toSet()
        assertEquals(setOf(sq(3, 4), sq(4, 3), sq(4, 5)), tos)
    }

    @Test
    fun captureIsMandatory() {
        val b = board(sq(4, 4) to 1, sq(3, 4) to -1, sq(6, 0) to 1)
        val moves = CheckersRules.legalMoves(b, CheckersRules.WHITE)
        assertEquals(1, moves.size)
        assertEquals(listOf(sq(4, 4), sq(2, 4)), moves[0].path)
        assertEquals(listOf(sq(3, 4)), moves[0].captured)
    }

    @Test
    fun mustTakeMaximumPieces() {
        // Sol: tek alma. Ileri: iki alma zinciri.
        val b = board(
            sq(6, 4) to 1,
            sq(6, 3) to -1, // sola tek
            sq(5, 4) to -1, sq(3, 4) to -1, // ileri iki
        )
        val moves = CheckersRules.legalMoves(b, CheckersRules.WHITE)
        assertTrue(moves.isNotEmpty())
        assertTrue(moves.all { it.captured.size == 2 })
        assertEquals(sq(2, 4), moves.first().to)
    }

    @Test
    fun manPromotesOnLastRow() {
        val b = board(sq(1, 2) to 1)
        val m = CheckersRules.legalMoves(b, CheckersRules.WHITE).first { it.to == sq(0, 2) }
        val after = CheckersRules.apply(b, m)
        assertEquals(2, after[sq(0, 2)])
    }

    @Test
    fun kingSlidesAndFlyingCaptures() {
        val b = board(sq(7, 0) to 2, sq(3, 0) to -1)
        val moves = CheckersRules.legalMoves(b, CheckersRules.WHITE)
        // Uzaktan alma zorunlu; arkasindaki 3 bos kareye inebilir
        assertTrue(moves.all { it.captured == listOf(sq(3, 0)) })
        assertEquals(setOf(sq(2, 0), sq(1, 0), sq(0, 0)), moves.map { it.to }.toSet())

        val free = board(sq(4, 4) to 2)
        assertEquals(14, CheckersRules.legalMoves(free, CheckersRules.WHITE).size)
    }

    @Test
    fun kingCannotTurn180DuringCapture() {
        // Dama asagi alip geri yukari donmemeli
        val b = board(sq(2, 3) to 2, sq(4, 3) to -1, sq(1, 3) to -1)
        val moves = CheckersRules.legalMoves(b, CheckersRules.WHITE)
        // Ya asagi (4,3) ya yukari (1,3) alabilir ama ikisini birden 180 derece donerek alamaz
        assertTrue(moves.all { it.captured.size == 1 })
    }

    @Test
    fun capturedPiecesRemovedImmediately() {
        // Ileri al, sonra sola al
        val b = board(sq(6, 4) to 1, sq(5, 4) to -1, sq(4, 3) to -1)
        val m = CheckersRules.legalMoves(b, CheckersRules.WHITE).single()
        assertEquals(2, m.captured.size)
        val after = CheckersRules.apply(b, m)
        assertEquals(0, CheckersRules.count(after, CheckersRules.BLACK))
        assertEquals(1, after[sq(4, 2)])
    }

    @Test
    fun noMovesMeansLoss() {
        val b = board(sq(0, 0) to -1, sq(1, 0) to 1, sq(0, 1) to 1, sq(2, 0) to 1)
        // Siyah tas (0,0): asagi (1,0) dolu ve arkasi dolu, saga (0,1) dolu ve arkasi bos -> alabilir
        assertTrue(CheckersRules.legalMoves(b, CheckersRules.BLACK).isNotEmpty())
        val blocked = board(sq(7, 0) to -1)
        // Son sirada tas (normalde dama olurdu) ileri gidemez ama yana gidebilir
        assertTrue(CheckersRules.legalMoves(blocked, CheckersRules.BLACK).isNotEmpty())
        val none = board(sq(4, 4) to 1)
        assertTrue(CheckersRules.legalMoves(none, CheckersRules.BLACK).isEmpty())
    }

    @Test
    fun botTakesFreePieceAndHardBeatsEasy() {
        val b = board(sq(4, 4) to -1, sq(5, 4) to 1, sq(7, 7) to 1)
        val m = CheckersRules.bestMove(b, CheckersRules.BLACK, 4)
        assertNotNull(m)
        assertEquals(listOf(sq(5, 4)), m!!.captured)

        // Zor (derinlik 4) ile kolay (derinlik 1 + gurultu) karsilasir; zor kaybetmemeli
        val rnd = Random(11)
        var hardWins = 0
        var easyWins = 0
        repeat(4) { game ->
            var board = CheckersRules.initialBoard()
            var turn = CheckersRules.WHITE
            val hardSide = if (game % 2 == 0) CheckersRules.WHITE else CheckersRules.BLACK
            var plies = 0
            while (plies < 200) {
                val mv = if (turn == hardSide) CheckersRules.bestMove(board, turn, 4, rnd)
                else CheckersRules.bestMove(board, turn, 1, rnd, noise = 150)
                if (mv == null) {
                    if (turn == hardSide) easyWins++ else hardWins++
                    break
                }
                board = CheckersRules.apply(board, mv)
                turn = -turn
                plies++
            }
        }
        assertTrue("hard=$hardWins easy=$easyWins", hardWins >= 3 && easyWins == 0)
    }

    @Test
    fun allLegalMovesAreConsistent() {
        // Rastgele oyunlarda hamle uretimi tutarli: alinan taslar rakip, hedef bos
        val rnd = Random(3)
        repeat(20) {
            var b = CheckersRules.initialBoard()
            var turn = CheckersRules.WHITE
            repeat(120) {
                val moves = CheckersRules.legalMoves(b, turn)
                if (moves.isEmpty()) return@repeat
                moves.forEach { m ->
                    assertTrue(b[m.from] * turn > 0)
                    assertTrue(m.captured.all { b[it] * turn < 0 })
                    // Alinan taslar hemen kalktigi icin dama onlarin karesine de inebilir
                    assertTrue(b[m.to] == 0 || m.to == m.from || m.to in m.captured)
                }
                b = CheckersRules.apply(b, moves[rnd.nextInt(moves.size)])
                assertFalse(b.any { kotlin.math.abs(it) > 2 })
                turn = -turn
            }
        }
    }
}
