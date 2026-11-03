package com.eomer91.minigamesworld.ui.games.factory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FactoryEngineTest {

    private fun run(e: FactoryEngine, seconds: Int, step: Double = 0.5) {
        var t = 0.0
        while (t < seconds) { e.update(step); t += step }
    }

    @Test
    fun mineProducesOrePerSecond() {
        val e = FactoryEngine(random = Random(1))
        run(e, 10)
        assertEquals(10.0, e.stock[Product.ORE]!!, 1.0)
        assertEquals(BuildingStatus.RUNNING, e.status[Building.MINE])
    }

    @Test
    fun smelterNeedsOre() {
        val e = FactoryEngine(FactorySave(cash = 0.0, levels = mapOf(Building.SMELTER to 1)), Random(1))
        run(e, 5)
        assertEquals(BuildingStatus.NO_INPUT, e.status[Building.SMELTER])
        e.stock[Product.ORE] = 10.0
        run(e, 4)
        // Beklerken biriken bir dongu + 4 sn x 0.5
        assertEquals(3.0, e.stock[Product.IRON]!!, 0.01)
        assertEquals(4.0, e.stock[Product.ORE]!!, 0.01)
    }

    @Test
    fun warehouseCapsStock() {
        val e = FactoryEngine(FactorySave(levels = mapOf(Building.MINE to 5)), Random(1))
        run(e, 60)
        assertEquals(e.capacity, e.stock[Product.ORE]!!, 0.01)
        assertEquals(BuildingStatus.FULL, e.status[Building.MINE])
    }

    @Test
    fun sellingLowersPriceAndRecovers() {
        val e = FactoryEngine(FactorySave(stock = mapOf(Product.ORE to 100.0)), Random(1))
        val p0 = e.price(Product.ORE)
        val got = e.sell(Product.ORE)
        assertTrue(got > 0 && got < 100 * p0)
        assertTrue(e.price(Product.ORE) < p0)
        run(e, 300)
        assertEquals(1.0, e.demand[Product.ORE]!!, 0.02)
    }

    @Test
    fun upgradesCostMoreEachLevel() {
        val e = FactoryEngine(FactorySave(cash = 1e9), Random(1))
        var last = 0.0
        repeat(5) {
            val c = Building.SMELTER.upgradeCost(e.level(Building.SMELTER))
            assertTrue(c >= last)
            last = c
            assertTrue(e.upgrade(Building.SMELTER))
        }
        assertFalse(FactoryEngine(FactorySave(cash = 10.0)).upgrade(Building.SAWMILL))
    }

    @Test
    fun saveRoundTrip() {
        val e = FactoryEngine(random = Random(2))
        run(e, 30)
        e.toggleAutoSell(Product.ORE)
        val s = e.save(123L)
        val e2 = FactoryEngine(s)
        assertEquals(e.cash, e2.cash, 1e-6)
        assertEquals(e.stock[Product.ORE]!!, e2.stock[Product.ORE]!!, 1e-6)
        assertTrue(Product.ORE in e2.autoSell)
    }

    @Test
    fun offlineEarnsWithAutoSell() {
        val e = FactoryEngine(FactorySave(levels = mapOf(Building.MINE to 3), autoSell = setOf(Product.ORE)), Random(3))
        val got = e.offline(600.0)
        assertTrue(got > 1000)
        assertEquals(0.0, e.offline(10.0), 0.0)
    }

    /** Basit acgozlu oyuncu: urunleri satar, en ucuz yukseltmeyi alir. Zincir makul surede ilerlemeli. */
    @Test
    fun progressionReachesCarPlant() {
        val e = FactoryEngine(random = Random(5))
        var t = 0
        var carAt = -1
        while (t < 3 * 3600 && carAt < 0) {
            e.update(1.0)
            // Girdi olarak gerekenler disindakileri sat
            val needed = Building.entries.filter { e.level(it) > 0 }.flatMap { it.inputs.keys }.toSet()
            Product.entries.forEach { p -> if (p !in needed || (e.stock[p] ?: 0.0) > e.capacity * 0.8) e.sell(p) }
            for (i in e.orders.indices.reversed()) e.deliver(i)
            val next = Building.entries.minByOrNull { it.upgradeCost(e.level(it)) * (if (e.level(it) == 0) 0.6 else 1.0) }!!
            e.upgrade(next)
            if (e.capacity < 400 && e.cash > e.warehouseCost * 3) e.upgradeWarehouse()
            if (e.level(Building.CAR_PLANT) > 0) carAt = t
            t++
        }
        println("car plant at ${carAt / 60} min, earned=${e.totalEarned.toInt()}")
        assertTrue("carAt=$carAt", carAt in 600..(150 * 60))
    }
}
