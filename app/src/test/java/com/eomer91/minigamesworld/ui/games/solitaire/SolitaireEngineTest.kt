package com.eomer91.minigamesworld.ui.games.solitaire

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SolitaireEngineTest {

    private val engine = SolitaireEngine(Random(9))

    @Test
    fun deal_layout() {
        val g = engine.deal(1)
        assertEquals(24, g.stock.size)
        g.tableau.forEachIndexed { i, col ->
            assertEquals(i + 1, col.size)
            assertTrue(col.last().faceUp)
            assertTrue(col.dropLast(1).none { it.faceUp })
        }
        val all = g.stock + g.tableau.flatten()
        assertEquals(52, all.map { it.id }.toSet().size)
    }

    @Test
    fun drawAndRecycle() {
        var g = engine.deal(3)
        g = engine.draw(g)
        assertEquals(21, g.stock.size)
        assertEquals(3, g.waste.size)
        repeat(7) { g = engine.draw(g) }
        assertTrue(g.stock.isEmpty())
        g = engine.draw(g)
        assertEquals(24, g.stock.size)
        assertTrue(g.waste.isEmpty())
    }

    @Test
    fun placementRules() {
        val redSeven = Card(0, 1, 7, true)
        val blackEight = Card(1, 0, 8, true)
        val redEight = Card(2, 2, 8, true)
        assertTrue(engine.canPlaceOnTableau(redSeven, listOf(blackEight)))
        assertFalse(engine.canPlaceOnTableau(redSeven, listOf(redEight)))
        assertTrue(engine.canPlaceOnTableau(Card(3, 3, 13, true), emptyList()))
        assertFalse(engine.canPlaceOnTableau(redSeven, emptyList()))
        assertTrue(engine.canPlaceOnFoundation(Card(4, 1, 1, true), emptyList()))
        assertTrue(engine.canPlaceOnFoundation(Card(5, 1, 2, true), listOf(Card(4, 1, 1, true))))
        assertFalse(engine.canPlaceOnFoundation(Card(6, 2, 2, true), listOf(Card(4, 1, 1, true))))
    }

    @Test
    fun movingFromTableauFlipsCardBelow() {
        // 2. sutunun ustundeki karti 1. sutuna tasinabilir yapmak icin el ile kurulum
        val g = SolitaireGame(
            stock = emptyList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = listOf(
                listOf(Card(0, 0, 8, true)),
                listOf(Card(1, 3, 2, false), Card(2, 1, 7, true)),
            ) + List(5) { emptyList() },
            drawCount = 1,
        )
        val m = engine.bestMove(g, Pile.Tableau(1), 1)
        assertNotNull(m)
        val next = engine.apply(g, m!!)
        assertEquals(2, next.tableau[0].size)
        assertTrue(next.tableau[1].single().faceUp)
        assertEquals(5, next.score)
    }

    @Test
    fun autoCompleteWins() {
        // Her renkten 13 kart, sutunlarda ters sirali ve acik: otomatik bitirme kazandirir
        val cols = (0 until 4).map { suit -> (13 downTo 1).map { r -> Card(suit * 13 + r - 1, suit, r, true) } }
        var g = SolitaireGame(emptyList(), emptyList(), List(4) { emptyList() }, cols + List(3) { emptyList() }, drawCount = 1)
        assertTrue(engine.canAutoComplete(g))
        var guard = 0
        while (!g.isWon && guard++ < 100) g = engine.apply(g, engine.nextFoundationMove(g)!!)
        assertTrue(g.isWon)
    }
}
