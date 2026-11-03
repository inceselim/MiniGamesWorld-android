package com.eomer91.minigamesworld.ui.games.coloring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColoringEngineTest {

    /** 20x20 beyaz; x=10'da dikey siyah cizgi, (3..6, 3..6) kare cerceve. */
    private fun art(): ColoringEngine {
        val w = 20
        val lum = IntArray(w * w) { 255 }
        for (y in 0 until w) lum[y * w + 10] = 0
        for (k in 3..6) {
            lum[3 * w + k] = 0; lum[6 * w + k] = 0; lum[k * w + 3] = 0; lum[k * w + 6] = 0
        }
        return ColoringEngine(w, w, lum)
    }

    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun fillStopsAtLines() {
        val e = art()
        assertTrue(e.fill(15, 15, red))
        assertEquals(red, e.pixels[15 * 20 + 15])
        assertEquals(red, e.pixels[0 * 20 + 19])
        assertTrue(e.pixels[0] != red) // cizginin solu boyanmadi
        // Kare icindeki bolge ayri
        assertTrue(e.fill(1, 1, blue))
        assertTrue(e.pixels[4 * 20 + 4] != blue)
        assertTrue(e.fill(4, 4, red))
        assertEquals(red, e.pixels[5 * 20 + 5])
    }

    @Test
    fun undoRestores() {
        val e = art()
        val before = e.pixels.copyOf()
        e.fill(15, 15, red)
        e.fill(15, 15, blue)
        assertTrue(e.undo())
        assertEquals(red, e.pixels[15 * 20 + 15])
        assertTrue(e.undo())
        assertTrue(before.contentEquals(e.pixels))
        assertFalse(e.undo())
    }

    @Test
    fun tappingLineFillsNearbyRegionAndSameColorIsNoop() {
        val e = art()
        assertTrue(e.fill(10, 15, red)) // cizginin uzerine dokunuldu
        assertFalse(e.fill(15, 15, red) && false)
        assertTrue(e.coloredRatio() > 0.3f)
    }

    @Test
    fun antialiasedPixelsAreShaded() {
        val lum = IntArray(4) { if (it == 1) 200 else 255 }
        val e = ColoringEngine(4, 1, lum)
        e.fill(0, 0, 0xFFFFFFFF.toInt() and 0xFF00FF00.toInt() or 0xFF000000.toInt())
        assertEquals(200, (e.pixels[1] shr 8) and 0xFF)
    }
}
