package com.eomer91.minigamesworld.ui.games.fox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FoxEngineTest {

    private val dt = 1f / 60f

    /** Ust bos satirlar + verilen satirlar + iki zemin satiri. */
    private fun level(vararg rows: String, width: Int = 30, ground: String? = null): List<String> {
        val g = ground ?: "#".repeat(width)
        val pad = List(8 - rows.size) { ".".repeat(width) }
        return pad + rows.map { it.padEnd(width, '.') } + listOf(g, g)
    }

    private fun engine(rows: List<String>) = FoxEngine(0, 0, rows, Random(1))

    private fun run(e: FoxEngine, seconds: Float, dir: Int = 0) {
        var t = 0f
        while (t < seconds) {
            e.update(dt, dir)
            t += dt
        }
    }

    @Test
    fun parsesStartStarsAndFlag() {
        val e = engine(level("..@....*....*...*.......F."))
        assertEquals(3, e.totalStars)
        assertEquals(24, e.flagCol)
        assertEquals(2f, e.px, 0.5f)
    }

    @Test
    fun landsOnGround() {
        val e = engine(level("..@"))
        run(e, 1f)
        assertTrue(e.onGround)
        assertEquals(8f - FoxEngine.PH, e.py, 0.01f)
    }

    @Test
    fun jumpReachesAboutFourTiles() {
        val e = engine(level("..@"))
        run(e, 0.5f)
        val startY = e.py
        e.pressJump()
        var minY = startY
        repeat(120) {
            e.update(dt, 0)
            minY = minOf(minY, e.py)
        }
        val rise = startY - minY
        assertTrue("rise=$rise", rise in 3.8f..4.5f)
        // Kisa basis daha alcak ziplar
        run(e, 1f)
        e.pressJump()
        e.update(dt, 0)
        e.releaseJump()
        var minY2 = e.py
        repeat(120) {
            e.update(dt, 0)
            minY2 = minOf(minY2, e.py)
        }
        assertTrue(startY - minY2 < rise * 0.6f)
    }

    @Test
    fun collectsCoinsWhileRunning() {
        val e = engine(level("..@..ccc"))
        run(e, 1.5f, dir = 1)
        assertEquals(3, e.coins)
    }

    @Test
    fun questionBlockGivesCoinAndBecomesUsed() {
        // ? blogu oyuncunun tam ustunde, iki satir yukarida
        val e = engine(level("..?", "", "..@"))
        val r = e.tiles.indexOfFirst { it.contains('?') }
        run(e, 0.5f)
        e.pressJump()
        run(e, 1f)
        assertEquals('U', e.tiles[r][2])
        assertEquals(1, e.coins)
    }

    @Test
    fun stompKillsSnailButHedgehogHurts() {
        // Tilki yukaridan salyangozun ustune duser
        val s = engine(level("..@", "", "", "", "", "..e"))
        run(s, 1f)
        assertEquals(1, s.stompCount)
        assertFalse(s.enemies.first().alive)
        assertEquals(FoxEngine.MAX_HEARTS, s.hearts)

        // Baslangictaki kisa koruma bitince kirpiye carpar
        val h = engine(level("..@" + ".".repeat(17) + "h"))
        run(h, 3f, dir = 1)
        assertTrue(h.hearts < FoxEngine.MAX_HEARTS)
    }

    @Test
    fun fallingInPitKills() {
        val g = "#####" + ".".repeat(5) + "#".repeat(20)
        val e = engine(level("..@", ground = g))
        run(e, 2f, dir = 1)
        assertTrue(e.dead)
        assertEquals(1, e.deaths)
        e.respawnAtCheckpoint()
        assertFalse(e.dead)
        assertEquals(FoxEngine.MAX_HEARTS, e.hearts)
    }

    @Test
    fun springLaunchesHigherThanJump() {
        val g = "#####S" + "#".repeat(24)
        val e = engine(level("..@", ground = g))
        var minY = 99f
        repeat(150) {
            e.update(dt, if (e.px < 5.1f) 1 else 0)
            minY = minOf(minY, e.py)
        }
        assertTrue("minY=$minY", 8f - FoxEngine.PH - minY > 5.5f)
    }

    @Test
    fun reachingFlagWins() {
        val e = engine(level("..@..*....F"))
        run(e, 3f, dir = 1)
        assertTrue(e.won)
        val r = e.result()
        assertEquals(1, r.stars)
        assertTrue(r.score > 1000)
    }

    @Test
    fun checkpointMovesRespawn() {
        val g = "#".repeat(12) + ".".repeat(4) + "#".repeat(14)
        val e = engine(level("..@...K", ground = g))
        run(e, 3f, dir = 1)
        assertTrue(e.dead)
        assertEquals(6, e.checkpointCol)
        e.respawnAtCheckpoint()
        assertEquals(6f, e.px, 0.5f)
    }

    @Test
    fun profileKeepsBestAndUnlocksNext() {
        var p = FoxProfile()
        assertTrue(p.isUnlocked(0, 0))
        assertFalse(p.isUnlocked(0, 1))
        p = p.withResult(0, 0, FoxResult(stars = 2, coins = 10, time = 60f, deaths = 0))
        assertTrue(p.isUnlocked(0, 1))
        p = p.withResult(0, 0, FoxResult(stars = 1, coins = 5, time = 40f, deaths = 1))
        assertEquals(2, p.starsOf(0, 0))
        assertEquals(40, p.timeOf(0, 0))
        // Dunya sonu bir sonraki dunyayi acar
        p = p.withResult(0, 4, FoxResult(3, 0, 50f, 0))
        assertTrue(p.isUnlocked(1, 0))
        assertEquals(p, FoxProfile.fromJson(p.toJson()))
    }

    @Test
    fun allLevelsAreWellFormed() {
        assertEquals(FoxEngine.WORLDS, FoxLevels.worlds.size)
        FoxLevels.worlds.forEachIndexed { w, levels ->
            assertEquals(FoxEngine.LEVELS_PER_WORLD, levels.size)
            levels.forEachIndexed { l, rows ->
                val width = rows.first().length
                assertTrue(rows.all { it.length == width })
                val e = FoxEngine(w, l, rows)
                assertEquals("$w-$l", 3, e.totalStars)
                assertTrue(e.flagCol in 10 until width)
                // Baslangicta yere duser, olmez
                repeat(60) { e.update(dt, 0) }
                assertTrue("$w-$l", e.onGround && !e.dead)
            }
        }
    }
}
