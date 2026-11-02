package com.eomer91.minigamesworld.ui.games.rocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RocketEngineTest {

    private val engine = RocketEngine(Random(1))

    @Test
    fun doesNothingBeforeStart() {
        var g = engine.newGame()
        repeat(120) { g = engine.update(g, 1f / 60f).first }
        assertFalse(g.gameOver)
        assertTrue(g.gates.isEmpty())
    }

    @Test
    fun fallingWithoutFlapCrashesIntoGround() {
        var g = engine.flap(engine.newGame())
        var t = 0f
        while (!g.gameOver && t < 5f) {
            g = engine.update(g, 1f / 60f).first
            t += 1f / 60f
        }
        assertTrue(g.gameOver)
        assertEquals(RocketEngine.GROUND_Y - RocketEngine.ROCKET_RADIUS, g.y, 0.01f)
    }

    /** Basit otomatik pilot: siradaki boslugun altina dusunce zipla. Oyun gecilebilir olmali. */
    @Test
    fun gatesArePassableByAutopilot() {
        var g = engine.flap(engine.newGame())
        var t = 0f
        while (!g.gameOver && t < 60f) {
            val target = g.gates.firstOrNull { !it.passed }?.gapY ?: (RocketEngine.HEIGHT / 2f)
            if (g.y > target + 0.6f && g.vy >= 0f) g = engine.flap(g)
            g = engine.update(g, 1f / 60f).first
            t += 1f / 60f
        }
        assertTrue("passed=${g.gatesPassed}", g.gatesPassed >= 15)
    }

    @Test
    fun collisionGeometry() {
        val gate = Gate(1, RocketEngine.ROCKET_X - 0.5f, gapY = 8f, gapH = 4f)
        assertFalse(engine.hits(gate, 8f))
        assertTrue(engine.hits(gate, 5.5f))
        assertTrue(engine.hits(gate, 10.5f))
    }

    @Test
    fun reviveGivesShield() {
        val crashed = engine.newGame().copy(started = true, gameOver = true, y = 14f)
        val r = engine.revive(crashed)
        assertFalse(r.gameOver)
        assertTrue(r.shield > 0f)
    }
}
