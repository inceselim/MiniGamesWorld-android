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
    fun trayMatchUndo() {
        val g = MahjongGame.forLevel(1)
        val free = g.freeIds()
        var a = -1; var b = -1
        loop@ for (i in free) for (j in free) if (i != j && g.tiles[i].face == g.tiles[j].face) { a = i; b = j; break@loop }
        check(a >= 0)
        assertEquals(MahjongGame.TapResult.ToTray(a, 0), g.tap(a))
        assertTrue(g.inTray(a))
        assertEquals(MahjongGame.TapResult.Paired(b, a, 0), g.tap(b))
        assertTrue(g.isRemoved(a) && g.isRemoved(b))
        assertEquals(0, g.tray.size)
        assertEquals(listOf(b, a), g.undo())
        assertEquals(listOf(a), g.tray)
        assertTrue(g.onBoard(b))
        assertEquals(listOf(a), g.undo())
        assertEquals(g.tiles.size, g.boardCount)
    }

    @Test
    fun trayFillsAndBlocks() {
        val g = MahjongGame.forLevel(40)
        // Dort farkli yuzlu acik tas bulup tepsiyi doldur
        val taken = HashSet<Int>()
        while (!g.isTrayFull) {
            val id = g.freeIds().first { f -> g.tray.none { g.tiles[it].face == g.tiles[f].face } && g.tiles[f].face !in taken }
            taken.add(g.tiles[id].face); g.tap(id)
        }
        val other = g.freeIds().first()
        assertEquals(MahjongGame.TapResult.TrayFull, g.tap(other))
        g.undo()
        assertFalse(g.isTrayFull)
    }

    /** Ipucunu izleyen oyuncu; ipucu yoksa karistirir. Her seviye bitmeli. */
    @Test
    fun hintPlayerAlwaysFinishes() {
        var shuffleTotal = 0
        val rnd = SeededRandom(7)
        for (level in 1..400 step 3) {
            var g = MahjongGame.forLevel(level)
            var shuffles = 0
            var steps = 0
            while (!g.isCleared) {
                check(steps++ < 2000) { "dongu $level" }
                val h = g.findHint()
                if (h.isEmpty()) {
                    val ng = g.shuffle(rnd)
                    if (ng === g || ng.findHint().isEmpty()) { check(g.undo() != null) { "kurtulus yok $level" }; continue }
                    g = ng; shuffles++
                    check(shuffles < 30) { "cok karistirma $level" }
                    continue
                }
                for (id in h) if (g.isFree(id)) g.tap(id)
            }
            shuffleTotal += shuffles
        }
        println("ipucu oyuncusu: toplam karistirma $shuffleTotal / 134 seviye")
    }

    /** Rastgele oyuncu (tepsiyi rastgele doldurur), takilinca geri alip karistirir. */
    @Test
    fun randomPlayerWithTray() {
        val rnd = SeededRandom(3)
        var shuffles = 0
        for (level in 1..200 step 5) {
            var g = MahjongGame.forLevel(level)
            var steps = 0
            while (!g.isCleared) {
                check(steps++ < 5000) { "dongu $level" }
                if (g.isTrayFull) { g.undo(); g.undo(); val ng = g.shuffle(rnd); if (ng !== g) { g = ng; shuffles++ }; continue }
                val free = g.freeIds()
                val match = free.firstOrNull { f -> g.tray.any { g.tiles[it].face == g.tiles[f].face } }
                g.tap(match ?: free[rnd.nextInt(free.size)])
            }
        }
        println("rastgele tepsi oyuncusu: $shuffles karistirma / 40 seviye")
    }

    @Test
    fun saveRestore() {
        val g = MahjongGame.forLevel(30)
        val a = g.freeIds().first()
        g.tap(a)
        val r = MahjongGame.restore(g.snapshot())!!
        assertEquals(listOf(a), r.tray)
        assertEquals(g.remaining, r.remaining)
    }

    @Test
    fun shuffleKeepsTrayPartners() {
        val rnd = SeededRandom(11)
        val g = MahjongGame.forLevel(90)
        val a = g.freeIds().first()
        g.tap(a)
        val s = g.shuffle(rnd)
        val board = s.tiles.indices.filter { s.onBoard(it) }
        val faceCount = board.groupingBy { s.tiles[it].face }.eachCount()
        assertEquals(1, (faceCount[s.tiles[a].face] ?: 0) % 2)
        assertEquals(listOf(a), s.tray)
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
