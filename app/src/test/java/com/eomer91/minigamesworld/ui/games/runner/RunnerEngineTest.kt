package com.eomer91.minigamesworld.ui.games.runner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RunnerEngineTest {

    private val dt = 1f / 60f

    private fun run(e: RunnerEngine, seconds: Float, bot: Boolean = false) {
        var t = 0f
        while (t < seconds && !e.dead) {
            if (bot) think(e)
            e.update(dt)
            t += dt
        }
    }

    /** Onundeki engele gore tepki veren basit oyuncu. */
    private fun think(e: RunnerEngine) {
        val reach = e.speed * 0.45f
        val ahead = e.obstacles.filter { !it.taken && it.type != ObstacleType.COIN && it.z in 0f..reach }
        val mine = ahead.filter { it.lane == e.lane }.minByOrNull { it.z } ?: return
        when (mine.type) {
            ObstacleType.ROCK -> {
                val blocked = ahead.filter { it.type == ObstacleType.ROCK && kotlin.math.abs(it.z - mine.z) < 3f }.map { it.lane }.toSet()
                val free = (-1..1).filter { it !in blocked }.minByOrNull { kotlin.math.abs(it - e.lane) } ?: return
                if (free < e.lane) e.left() else if (free > e.lane) e.right()
            }
            ObstacleType.BEAM -> if (mine.z < e.speed * 0.25f) e.duck()
            ObstacleType.BARREL -> if (mine.z < e.speed * 0.3f) e.jump()
            else -> Unit
        }
    }

    @Test
    fun laneLimitsAndSmoothMove() {
        val e = RunnerEngine(Random(1))
        e.left(); e.left(); e.left()
        assertEquals(-1, e.lane)
        repeat(30) { e.update(dt) }
        assertEquals(-1f, e.laneX, 0.001f)
    }

    @Test
    fun rockCrashesBeamNeedsDuckBarrelNeedsJump() {
        val rock = RunnerEngine(Random(1)).apply { obstacles += Obstacle(ObstacleType.ROCK, 0, 5f) }
        run(rock, 1f)
        assertTrue(rock.dead)

        val beam = RunnerEngine(Random(1)).apply { obstacles += Obstacle(ObstacleType.BEAM, 0, 6f) }
        beam.update(dt)
        beam.duck()
        run(beam, 1f)
        assertFalse(beam.dead)

        val barrel = RunnerEngine(Random(1)).apply { obstacles += Obstacle(ObstacleType.BARREL, 0, 4f) }
        barrel.jump()
        run(barrel, 1f)
        assertFalse(barrel.dead)

        val dodge = RunnerEngine(Random(1)).apply { obstacles += Obstacle(ObstacleType.ROCK, 0, 8f) }
        dodge.right()
        run(dodge, 1.5f)
        assertFalse(dodge.dead)
    }

    @Test
    fun coinsAreCollected() {
        val e = RunnerEngine(Random(1))
        repeat(3) { e.obstacles += Obstacle(ObstacleType.COIN, 0, 3f + it * 2f) }
        run(e, 1f)
        assertEquals(3, e.coins)
        assertEquals(e.distance.toInt() + 3 * RunnerEngine.COIN_SCORE, e.score)
    }

    @Test
    fun patternsAreAlwaysSurvivable() {
        repeat(5) { seed ->
            val e = RunnerEngine(Random(seed))
            run(e, 150f, bot = true)
            assertFalse("seed=$seed t=${e.time} d=${e.distance}", e.dead)
            assertTrue(e.speed > 30f)
        }
    }

    @Test
    fun reviveClearsNearbyObstacles() {
        val e = RunnerEngine(Random(1)).apply { obstacles += Obstacle(ObstacleType.ROCK, 0, 3f); obstacles += Obstacle(ObstacleType.ROCK, 0, 10f) }
        run(e, 1f)
        assertTrue(e.dead)
        e.revive()
        assertFalse(e.dead)
        assertTrue(e.obstacles.none { it.z < 25f })
    }
}
