package com.eomer91.minigamesworld.ui.games.fish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FishingEngineTest {

    private val engine = FishingEngine(Random(8))

    private fun run(c0: FishingCast, steer: (FishingCast) -> Float? = { null }): FishingCast {
        var c = engine.start(c0)
        var t = 0f
        while (c.phase != CastPhase.DONE && t < 60f) {
            steer(c)?.let { c = engine.steer(c, it) }
            c = engine.update(c, 1f / 60f).first
            t += 1f / 60f
        }
        return c
    }

    @Test
    fun castEndsAtSurface() {
        val c = run(engine.newCast(maxDepth = 30f, capacity = 2, speedFactor = 1f))
        assertEquals(CastPhase.DONE, c.phase)
        assertEquals(0f, c.depth, 0.001f)
        assertTrue(c.hooked.size <= 2)
    }

    @Test
    fun speciesSpawnInTheirDepthBands() {
        val c = engine.newCast(maxDepth = 135f, capacity = 7, speedFactor = 1f)
        c.creatures.filter { it.species != null }.forEach { cr ->
            val s = cr.species!!
            assertTrue("${s.name} at ${cr.depth}", cr.depth in s.minDepth..s.maxDepth)
        }
    }

    @Test
    fun steeringTowardFishCatchesSomething() {
        // Kanca, inerken en yakin baliga dogru yonlendirilirse bir sey tutulmali
        var caughtAny = false
        repeat(10) {
            val c = run(engine.newCast(maxDepth = 60f, capacity = 3, speedFactor = 1f)) { cast ->
                cast.creatures.filter { !it.caught && !it.isJelly && it.depth > cast.depth }.minByOrNull { it.depth }?.x
            }
            if (c.hooked.isNotEmpty()) caughtAny = true
        }
        assertTrue(caughtAny)
    }

    @Test
    fun upgradesImproveStats() {
        assertTrue(RodStats.maxDepth(3) > RodStats.maxDepth(0))
        assertTrue(RodStats.capacity(2) > RodStats.capacity(0))
        assertTrue(RodUpgrade.DEPTH.cost(2) > RodUpgrade.DEPTH.cost(1))
    }

    @Test
    fun profileJsonRoundTrip() {
        val p = FishProfile(pearls = 120, rodDepthLevel = 2, aquarium = listOf(OwnedFish(1, "CLOWNFISH", 1000L)))
        assertEquals(p, FishProfile.fromJson(p.toJson()))
        assertEquals(FishProfile(), FishProfile.fromJson("bozuk"))
    }
}
