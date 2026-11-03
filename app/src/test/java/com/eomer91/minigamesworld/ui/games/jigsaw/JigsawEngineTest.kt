package com.eomer91.minigamesworld.ui.games.jigsaw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JigsawEngineTest {

    @Test
    fun neighbourEdgesInterlock() {
        val e = JigsawEngine(6, 4, seed = 5)
        for (r in 0 until 4) for (c in 0 until 6) {
            val ed = e.edges(r, c)
            if (r == 0) assertEquals(0, ed[0])
            if (c == 5) assertEquals(0, ed[1])
            if (c < 5) assertEquals(-ed[1], e.edges(r, c + 1)[3])
            if (r < 3) assertEquals(-ed[2], e.edges(r + 1, c)[0])
        }
    }

    @Test
    fun scatteredPiecesStartInTray() {
        val e = JigsawEngine(4, 3, seed = 1)
        e.scatter(3.25f, 7f)
        assertTrue(e.pieces.all { it.y >= 3.25f && !it.placed })
        assertEquals(12, e.pieces.size)
    }

    @Test
    fun dropSnapsOnlyWhenClose() {
        val e = JigsawEngine(4, 3, seed = 2)
        e.scatter(3.25f, 7f)
        val p = e.pieces.first()
        val picked = e.pick(p.x + 0.5f, p.y + 0.5f)!!
        picked.x = picked.c + 0.5f
        picked.y = picked.r.toFloat()
        assertFalse(e.drop(picked))
        picked.x = picked.c + 0.2f
        picked.y = picked.r - 0.1f
        assertTrue(e.drop(picked))
        assertEquals(picked.c.toFloat(), picked.x)
        assertEquals(1, e.placedCount)
        // Yerlesen parca tekrar secilemez
        assertTrue(e.pick(picked.c + 0.5f, picked.r + 0.5f) == null || e.pick(picked.c + 0.5f, picked.r + 0.5f) != picked)
    }

    @Test
    fun solvingAllPiecesFinishes() {
        val e = JigsawEngine(8, 5, seed = 3)
        e.scatter(5.25f, 9f)
        e.pieces.toList().forEach { it.x = it.c.toFloat(); it.y = it.r.toFloat(); e.drop(it) }
        assertTrue(e.done)
    }
}
