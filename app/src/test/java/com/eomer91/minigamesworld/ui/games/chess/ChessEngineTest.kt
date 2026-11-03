package com.eomer91.minigamesworld.ui.games.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChessEngineTest {

    private fun perft(p: ChessPosition, depth: Int): Long {
        if (depth == 0) return 1
        var n = 0L
        for (m in p.legalMoves()) {
            val u = p.make(m)
            n += perft(p, depth - 1)
            p.unmake(m, u)
        }
        return n
    }

    @Test
    fun perftStartPosition() {
        val p = ChessPosition()
        assertEquals(20L, perft(p, 1))
        assertEquals(400L, perft(p, 2))
        assertEquals(8902L, perft(p, 3))
        assertEquals(197281L, perft(p, 4))
    }

    @Test
    fun perftKiwipete() {
        // Rok, gecerken alma, terfi ve acmaz iceren zor pozisyon
        val p = ChessPosition.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -")
        assertEquals(48L, perft(p, 1))
        assertEquals(2039L, perft(p, 2))
        assertEquals(97862L, perft(p, 3))
    }

    @Test
    fun perftEndgamePosition() {
        val p = ChessPosition.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -")
        assertEquals(14L, perft(p, 1))
        assertEquals(191L, perft(p, 2))
        assertEquals(2812L, perft(p, 3))
        assertEquals(43238L, perft(p, 4))
    }

    @Test
    fun perftPromotions() {
        val p = ChessPosition.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -")
        assertEquals(6L, perft(p, 1))
        assertEquals(264L, perft(p, 2))
        assertEquals(9467L, perft(p, 3))
    }

    @Test
    fun makeUnmakeRestoresPosition() {
        val p = ChessPosition.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -")
        val key = p.key()
        for (m in p.legalMoves()) {
            val u = p.make(m)
            p.unmake(m, u)
            assertEquals(key, p.key())
        }
    }

    @Test
    fun checkmateAndStalemate() {
        val mate = ChessPosition.fromFen("R5k1/5ppp/8/8/8/8/5PPP/6K1 b - -")
        assertTrue(mate.legalMoves().isEmpty() && mate.inCheck())
        val stale = ChessPosition.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - -")
        assertTrue(stale.legalMoves().isEmpty() && !stale.inCheck())
        assertTrue(ChessPosition.fromFen("8/8/4k3/8/8/3NK3/8/8 w - -").insufficientMaterial())
        assertFalse(ChessPosition.fromFen("8/8/4k3/8/8/3RK3/8/8 w - -").insufficientMaterial())
    }

    @Test
    fun aiFindsMateInOne() {
        val p = ChessPosition.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - -")
        val m = ChessAi.bestMove(p, 3)
        assertNotNull(m)
        assertEquals("d8", Chess.name(Chess.to(m!!)))
    }

    @Test
    fun aiTakesHangingQueen() {
        val p = ChessPosition.fromFen("4k3/8/8/3q4/8/8/3R4/4K3 w - -")
        val m = ChessAi.bestMove(p, 2)!!
        assertEquals("d5", Chess.name(Chess.to(m)))
    }

    @Test
    fun strongBeatsRandom() {
        val rnd = Random(4)
        var wins = 0
        repeat(2) { g ->
            val p = ChessPosition()
            val strong = if (g == 0) Chess.WHITE else Chess.BLACK
            var plies = 0
            while (plies < 160) {
                val moves = p.legalMoves()
                if (moves.isEmpty()) {
                    if (p.inCheck() && p.side != strong) wins++
                    break
                }
                val m = if (p.side == strong) ChessAi.bestMove(p, 2, rnd, maxNodes = 30_000)!! else moves[rnd.nextInt(moves.size)]
                p.make(m)
                plies++
            }
        }
        assertEquals(2, wins)
    }
}
