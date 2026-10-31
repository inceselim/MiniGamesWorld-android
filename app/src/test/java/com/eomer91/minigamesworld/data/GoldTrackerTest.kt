package com.eomer91.minigamesworld.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldTrackerTest {

    @Test
    fun awardsOneGoldPerHundredPoints() {
        val t = GoldTracker()
        assertEquals(0, t.onScore(90))
        assertEquals(1, t.onScore(110))
        assertEquals(0, t.onScore(150))
        assertEquals(3, t.onScore(420))
        assertEquals(4, t.awarded)
    }

    @Test
    fun undoDoesNotPayTwice() {
        val t = GoldTracker()
        assertEquals(2, t.onScore(250))
        // Geri alma: skor dustu, sonra ayni seviyeye tekrar cikti
        assertEquals(0, t.onScore(180))
        assertEquals(0, t.onScore(260))
        assertEquals(1, t.onScore(300))
    }

    @Test
    fun resetStartsNewGame() {
        val t = GoldTracker()
        t.onScore(500)
        t.reset()
        assertEquals(1, t.onScore(100))
    }
}
