package com.eomer91.minigamesworld.ui.games.dressup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DressUpTest {

    @Test
    fun artIsComplete() {
        DressPalettes.hairStyles.forEach { h ->
            assertTrue(h, DressUpArt.hairBack.containsKey(h) && DressUpArt.hairFront.containsKey(h))
        }
        listOf(DressUpArt.top, DressUpArt.bottom, DressUpArt.dress, DressUpArt.shoes, DressUpArt.head, DressUpArt.extra).forEach { items ->
            assertTrue(items.isNotEmpty())
            assertEquals(items.size, items.map { it.id }.toSet().size)
        }
        // Varsayilan kombin gercek parcalara isaret eder
        val o = Outfit()
        assertEquals(3, DressUpViewModel.wornItems(o).size)
    }

    @Test
    fun matchingOutfitScoresHigher() {
        val beachTags = listOf(setOf("summer") to 1L, setOf("summer") to 1L, setOf("summer") to 2L, setOf("summer") to 2L)
        val winterTags = listOf(setOf("winter") to 1L, setOf("winter") to 2L, setOf("winter") to 3L, setOf("winter") to 4L, setOf("winter") to 5L)
        val good = DressJudge.rawScore(DressTheme.BEACH, beachTags, DressBackground.BEACH)
        val bad = DressJudge.rawScore(DressTheme.BEACH, winterTags, DressBackground.SNOW)
        assertTrue(good > 10)
        assertTrue(bad < 0)
        assertEquals(3, DressJudge.stars(DressJudge.judges(good + 4, Random(1))))
        assertEquals(0, DressJudge.stars(DressJudge.judges(bad, Random(1))))
    }

    @Test
    fun dressReplacesTopAndBottom() {
        val o = Outfit(top = null, bottom = null, dress = "party")
        val worn = DressUpViewModel.wornItems(o).map { it.first }
        assertTrue(DressCategory.DRESS in worn && DressCategory.TOP !in worn)
    }

    @Test
    fun profileRoundTrip() {
        val p = DressProfile(outfit = Outfit(hair = "bob", colors = mapOf("top:tee" to 0xFF123456)), totalStars = 7)
        assertEquals(p, DressProfile.fromJson(p.toJson()))
    }
}
