package com.eomer91.minigamesworld.ui.games.mahjong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MahjongEngineTest {

    @Test
    fun freeRules() {
        val a = Slot(0, 0, 0); val b = Slot(2, 0, 0); val c = Slot(4, 0, 0); val top = Slot(3, 1, 1)
        val all = listOf(a, b, c)
        assertTrue(MahjongRules.isFree(a, all))
        assertFalse(MahjongRules.isFree(b, all))
        assertTrue(MahjongRules.isFree(c, all))
        // Yarim kaydirmali satir da yan komsu sayilir
        assertFalse(MahjongRules.isFree(b, listOf(Slot(0, 1, 0), b, Slot(4, -1, 0))))
        // Ustte tas varsa kapali
        assertFalse(MahjongRules.isFree(c, listOf(c, top)))
        assertTrue(MahjongRules.isFree(top, listOf(a, b, c, top)))
    }

    @Test
    fun levelsAreDeterministicEvenAndSolvable() {
        val families = HashMap<String, Int>()
        var maxW = 0; var maxH = 0
        for (level in 1..1500) {
            val g = MahjongDealer.generate(level)
            val tiles = g.tiles
            assertEquals("cift", 0, tiles.size % 2)
            assertTrue("en az 16: $level ${tiles.size}", tiles.size >= 16)
            assertTrue("en fazla 150: $level ${tiles.size}", tiles.size <= 150)
            // Her yuz cift sayida
            tiles.groupingBy { it.face }.eachCount().values.forEach { assertEquals(0, it % 2) }
            // Ayni konumda iki tas yok
            assertEquals(tiles.size, tiles.map { Triple(it.x, it.y, it.z) }.toSet().size)
            // Cakisan tas yok (ayni katta)
            for (i in tiles.indices) for (j in i + 1 until tiles.size) {
                val p = tiles[i]; val q = tiles[j]
                if (p.z == q.z) assertFalse("cakisma $level", kotlin.math.abs(p.x - q.x) < 2 && kotlin.math.abs(p.y - q.y) < 2)
            }
            // Bilinen cozum gercekten oynanabilir
            val alive = BooleanArray(tiles.size) { true }
            val slots = tiles.map { Slot(it.x, it.y, it.z) }
            for (k in 0 until tiles.size / 2) {
                val a = g.solution[2 * k]; val b = g.solution[2 * k + 1]
                val present = slots.filterIndexed { i, _ -> alive[i] }
                assertTrue("acik degil $level", MahjongRules.isFree(slots[a], present) && MahjongRules.isFree(slots[b], present))
                assertEquals(tiles[a].face, tiles[b].face)
                alive[a] = false; alive[b] = false
            }
            families[g.info.layout] = (families[g.info.layout] ?: 0) + 1
            maxW = maxOf(maxW, tiles.maxOf { it.x } + 2 - tiles.minOf { it.x })
            maxH = maxOf(maxH, tiles.maxOf { it.y } + 2 - tiles.minOf { it.y })
        }
        // Ayni seviye ayni tahta
        assertEquals(MahjongDealer.deal(77), MahjongGame.forLevel(77).tiles)
        assertTrue("genislik $maxW", maxW <= MahjongLayouts.MAX_COLS * 2 + 2)
        assertTrue("yukseklik $maxH", maxH <= MahjongLayouts.MAX_ROWS * 2 + 2)
        assertTrue("cesitlilik ${families.size}", families.size >= 15)
        println("aileler: $families  maxW=$maxW maxH=$maxH")
    }

    @Test
    fun tapMatchUndo() {
        val g = MahjongGame.forLevel(1)
        val (a, b) = g.findPair()!!
        assertEquals(MahjongGame.TapResult.Selected, g.tap(a))
        assertEquals(MahjongGame.TapResult.Matched(a, b), g.tap(b))
        assertTrue(g.isRemoved(a) && g.isRemoved(b))
        assertNotNull(g.undo())
        assertFalse(g.isRemoved(a))
        assertEquals(g.tiles.size, g.remaining)
    }

    /** Rastgele oynayan oyuncu, takilinca karistirir: her seviye bitmeli. */
    @Test
    fun randomPlayerAlwaysFinishesWithShuffles() {
        var totalShuffles = 0
        var stuckLevels = 0
        val rnd = SeededRandom(42)
        for (level in 1..400 step 3) {
            var g = MahjongGame.forLevel(level)
            var shuffles = 0
            while (!g.isCleared) {
                val free = g.freeIds()
                val pairs = ArrayList<Pair<Int, Int>>()
                for (i in free.indices) for (j in i + 1 until free.size) if (g.tiles[free[i]].face == g.tiles[free[j]].face) pairs.add(free[i] to free[j])
                if (pairs.isEmpty()) {
                    g = g.shuffle(rnd); shuffles++
                    assertTrue("seviye $level cok karistirma", shuffles < 40)
                    assertTrue("karistirma sonrasi hamle yok $level", g.hasMoves())
                    continue
                }
                val (a, b) = pairs[rnd.nextInt(pairs.size)]
                g.tap(a); g.tap(b)
            }
            if (shuffles > 0) stuckLevels++
            totalShuffles += shuffles
        }
        println("rastgele oyuncu: takilan seviye $stuckLevels / 134, toplam karistirma $totalShuffles")
    }

    @Test
    fun saveRestore() {
        val g = MahjongGame.forLevel(30)
        val (a, b) = g.findPair()!!
        g.tap(a); g.tap(b)
        val r = MahjongGame.restore(g.snapshot())!!
        assertEquals(g.remaining, r.remaining)
        assertTrue(r.isRemoved(a))
    }

    @Test
    fun difficultyGrows() {
        val early = (1..20).map { MahjongLevels.params(it).first }.average()
        val late = (200..220).map { MahjongLevels.params(it).first }.average()
        assertTrue(late > early * 2)
        for (l in 1..300) {
            val info = MahjongDealer.levelInfo(l)
            assertEquals((l - 1) / 20 % 6, info.theme)
            assertTrue(info.faceTypes <= if (info.theme == 0) 36 else 18)
        }
    }
}
