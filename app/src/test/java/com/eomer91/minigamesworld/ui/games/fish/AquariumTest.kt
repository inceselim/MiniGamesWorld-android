package com.eomer91.minigamesworld.ui.games.fish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AquariumTest {
    private val h = Aquarium.HOUR
    private val t0 = 1_000_000_000L

    private fun profileWith(vararg fish: OwnedFish) = FishProfile(aquarium = fish.toList(), foodUpdatedAt = t0)

    @Test
    fun hungerStates() {
        val f = OwnedFish(1, "CLOWNFISH", t0)
        assertEquals(Hunger.FULL, Aquarium.hunger(f, t0 + 10 * Aquarium.MINUTE))
        assertEquals(Hunger.HUNGRY, Aquarium.hunger(f, t0 + 2 * h))
        assertEquals(Hunger.STARVING, Aquarium.hunger(f, t0 + 13 * h))
    }

    @Test
    fun feedingGrowsOnlyWhenHungry() {
        var p = profileWith(OwnedFish(1, "CLOWNFISH", t0))
        p = Aquarium.feed(p, 1, t0 + 5 * Aquarium.MINUTE) // tok: yemez
        assertEquals(0f, p.aquarium[0].growth, 0.001f)
        p = Aquarium.feed(p, 1, t0 + 2 * h)
        assertEquals(0.1f, p.aquarium[0].growth, 0.001f)
    }

    @Test
    fun pearlsAccrueAndAreCappedAndStopWhenStarving() {
        val f = OwnedFish(1, "GOLDFISH", t0)
        val rate = Aquarium.pearlRate(f, 0f)
        assertEquals(rate * 2f, Aquarium.pending(f, t0 + 2 * h, 0f), 0.01f)
        assertEquals(rate * Aquarium.MAX_ACCRUAL_HOURS, Aquarium.pending(f, t0 + 30 * h, 0f), 0.01f)

        val (collected, gained) = Aquarium.collect(profileWith(f), t0 + 3 * h)
        assertEquals((rate * 3).toInt(), gained)
        assertEquals(gained, collected.pearls)
        // 12 saatten fazla ac: aclik basladiktan sonra toplanirsa yeni uretim yok
        val (late, _) = Aquarium.collect(profileWith(f), t0 + 13 * h)
        assertEquals(0f, Aquarium.pending(late.aquarium[0], t0 + 20 * h, 0f), 0.001f)
    }

    @Test
    fun foodRefillsOverTime() {
        var p = FishProfile(food = 0, foodUpdatedAt = t0)
        p = Aquarium.refillFood(p, t0 + 3 * Aquarium.FOOD_REFILL_MS + 1000)
        assertEquals(3, p.food)
        p = Aquarium.refillFood(p, t0 + 100 * Aquarium.FOOD_REFILL_MS)
        assertEquals(Aquarium.MAX_FOOD, p.food)
        assertNull(Aquarium.useFood(FishProfile(food = 0, foodUpdatedAt = t0), t0))
    }

    @Test
    fun sellingFreesSpace() {
        val p = profileWith(OwnedFish(1, "SARDINE", t0), OwnedFish(2, "ANGELFISH", t0, growth = 1f))
        val (after, price) = Aquarium.sell(p, 2)
        assertEquals(1, after.aquarium.size)
        assertEquals(12 * 6, price)
    }

    @Test
    fun hungryFishEatsDroppedPellet() {
        val sim = TankSim(Random(1))
        val p = profileWith(OwnedFish(1, "CLOWNFISH", t0))
        var s = sim.build(p, t0 + 2 * h)
        s = sim.drop(s, 5f)
        var ate = false
        repeat(60 * 20) {
            val (n, eaten) = sim.update(s, 1f / 60f)
            s = n
            if (eaten.contains(1L)) ate = true
        }
        assertTrue(ate)
    }
}
