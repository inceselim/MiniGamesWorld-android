package com.eomer91.minigamesworld.ui.games.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MemoryEngineTest {

    private val engine = MemoryEngine(Random(42))

    @Test
    fun newDeck_hasEverySymbolExactlyTwice() {
        val deck = engine.newDeck()
        assertEquals(16, deck.size)
        assertEquals(16, deck.map { it.id }.toSet().size)
        deck.groupingBy { it.symbol }.eachCount().values.forEach { assertEquals(2, it) }
    }

    @Test
    fun isPair_and_matching() {
        val deck = engine.newDeck()
        val first = deck[0]
        val twin = deck.first { it.symbol == first.symbol && it.id != first.id }
        val other = deck.first { it.symbol != first.symbol }
        assertTrue(engine.isPair(deck, first.id, twin.id))
        assertFalse(engine.isPair(deck, first.id, other.id))
        assertFalse(engine.isPair(deck, first.id, first.id))

        val allDone = engine.markMatched(deck, deck.map { it.id })
        assertTrue(engine.allMatched(allDone))
    }

    @Test
    fun scoreFor_perfectAndPenalty() {
        assertEquals(1000, engine.scoreFor(8))
        assertEquals(1000 - 40 * 4, engine.scoreFor(12))
        assertEquals(MemoryEngine.MIN_SCORE, engine.scoreFor(500))
    }
}
