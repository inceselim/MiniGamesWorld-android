package com.eomer91.minigamesworld.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldTrackerTest {

    @Test
    fun awardsOneGoldPerTwoHundredPoints() {
        assertEquals(200, GameEconomy.POINTS_PER_GOLD)
        val t = GoldTracker()
        assertEquals(0, t.onScore(190))
        assertEquals(1, t.onScore(210))
        assertEquals(0, t.onScore(390))
        assertEquals(3, t.onScore(820))
        assertEquals(4, t.awarded)
    }

    @Test
    fun undoDoesNotPayTwice() {
        val t = GoldTracker()
        assertEquals(2, t.onScore(450))
        // Geri alma: skor dustu, sonra ayni seviyeye tekrar cikti
        assertEquals(0, t.onScore(350))
        assertEquals(0, t.onScore(460))
        assertEquals(1, t.onScore(600))
    }

    @Test
    fun resetStartsNewGame() {
        val t = GoldTracker()
        t.onScore(1000)
        t.reset()
        assertEquals(1, t.onScore(200))
    }
}
