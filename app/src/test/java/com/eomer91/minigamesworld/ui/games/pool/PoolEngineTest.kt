package com.eomer91.minigamesworld.ui.games.pool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random

class PoolEngineTest {

    private fun settle(t: PoolTable) {
        var time = 0f
        while (t.moving && time < 20f) { t.step(1f / 60f); time += 1f / 60f }
    }

    private fun play(g: PoolGame) {
        var time = 0f
        while (!g.update(1f / 60f) && time < 30f) time += 1f / 60f
    }

    /** Oyunda sadece verilen toplar masada kalsin. */
    private fun only(g: PoolGame, vararg pos: Triple<Int, Float, Float>) {
        g.table.balls.forEach { if (it.id != 0) it.pocketed = true }
        pos.forEach { (id, x, y) -> g.table.balls[id].apply { this.x = x; this.y = y; pocketed = false } }
    }

    @Test
    fun headOnCollisionTransfersSpeed() {
        val t = PoolTable(listOf(Ball(0, 0.5f, 0.5f), Ball(1, 1.0f, 0.5f)))
        t.balls[0].vx = 2f
        repeat(30) { t.step(1f / 60f) }
        assertTrue(abs(t.balls[0].vx) < 0.15f)
        assertTrue(t.balls[1].vx > 1.5f)
        assertEquals(1, t.log.firstContact)
    }

    @Test
    fun frictionStopsBall() {
        val t = PoolTable(listOf(Ball(0, 0.3f, 0.5f)))
        t.balls[0].vx = 1f
        settle(t)
        assertFalse(t.moving)
        // v^2 / 2a yaklasik
        assertEquals(0.3f + 1f / (2 * PoolTable.FRICTION), t.balls[0].x, 0.05f)
    }

    @Test
    fun cushionBouncesAndCornerPockets() {
        val t = PoolTable(listOf(Ball(0, 0.7f, 0.5f)))
        t.balls[0].vy = 2f
        repeat(20) { t.step(1f / 60f) }
        assertTrue(t.balls[0].vy < 0f)
        assertTrue(t.log.cushionHits >= 1)

        val p = PoolTable(listOf(Ball(0, 0.4f, 0.4f)))
        val a = atan2(-0.4f, -0.4f)
        p.balls[0].vx = kotlin.math.cos(a) * 2f
        p.balls[0].vy = kotlin.math.sin(a) * 2f
        settle(p)
        assertTrue(p.balls[0].pocketed)
    }

    @Test
    fun scratchIsFoulWithBallInHand() {
        val g = PoolGame(Random(1))
        play(g.apply { shoot(0f, 0.9f) }) // kiris
        only(g, Triple(3, 1.5f, 0.5f))
        g.table.cue.apply { x = 0.4f; y = 0.4f; vx = 0f; vy = 0f; pocketed = false }
        val shooter = g.turn
        g.shoot(atan2(-0.4f, -0.4f), 0.6f) // beyazi cebe at
        play(g)
        assertEquals(PoolEvent.FOUL, g.lastEvent)
        assertTrue(g.ballInHand)
        assertEquals(1 - shooter, g.turn)
        assertFalse(g.table.cue.pocketed)
    }

    @Test
    fun legalPotAssignsGroupAndContinues() {
        val g = PoolGame(Random(2))
        play(g.apply { shoot(0f, 0.9f) })
        only(g, Triple(3, 0.15f, 0.15f), Triple(11, 1.5f, 0.6f))
        g.table.cue.apply { x = 0.5f; y = 0.5f; pocketed = false }
        g.groups[0] = null; g.groups[1] = null
        val shooter = g.turn
        // Beyazdan 3'e, 3 de (0,0) cebine dogru
        g.shoot(atan2(0.15f - 0.5f + 0.042f, 0.15f - 0.5f + 0.042f), 0.6f)
        play(g)
        assertTrue(g.table.balls[3].pocketed)
        assertEquals(PoolGroup.SOLID, g.groups[shooter])
        assertEquals(PoolGroup.STRIPE, g.groups[1 - shooter])
        assertEquals(shooter, g.turn)
    }

    @Test
    fun earlyEightLoses() {
        val g = PoolGame(Random(3))
        play(g.apply { shoot(0f, 0.9f) })
        only(g, Triple(8, 0.15f, 0.15f), Triple(4, 1.6f, 0.7f))
        g.table.cue.apply { x = 0.5f; y = 0.5f; pocketed = false }
        val shooter = g.turn
        g.groups[shooter] = PoolGroup.SOLID; g.groups[1 - shooter] = PoolGroup.STRIPE
        g.shoot(atan2(-0.3f, -0.3f), 0.6f)
        play(g)
        assertTrue(g.over)
        assertEquals(1 - shooter, g.winner)
    }

    @Test
    fun aiPotsAnEasyShot() {
        var potted = 0
        repeat(10) { seed ->
            val g = PoolGame(Random(seed))
            play(g.apply { shoot(0f, 0.9f) })
            only(g, Triple(5, 1.7f, 0.25f))
            g.table.cue.apply { x = 1.0f; y = 0.62f; pocketed = false }
            g.groups[g.turn] = PoolGroup.SOLID; g.groups[1 - g.turn] = PoolGroup.STRIPE
            val plan = PoolAi.choose(g, g.turn, 0.4f, Random(seed))
            g.shoot(plan.angle, plan.power)
            play(g)
            if (g.table.balls[5].pocketed) potted++
        }
        assertTrue("potted=$potted", potted >= 8)
    }

    @Test
    fun aiVsAiGameEnds() {
        repeat(3) { seed ->
            val g = PoolGame(Random(seed))
            var shots = 0
            while (!g.over && shots < 200) {
                val plan = PoolAi.choose(g, g.turn, 1.5f, Random(seed * 100 + shots))
                g.shoot(plan.angle, plan.power)
                play(g)
                shots++
            }
            assertTrue("seed=$seed shots=$shots", g.over)
        }
    }
}
