package com.eomer91.minigamesworld.ui.games.island.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsBotsTest {

    private fun world(bots: Int = 3, size: MapSize = MapSize.MEDIUM, type: MapType = MapType.ISLAND, diff: Difficulty = Difficulty.NORMAL, seed: Long = 5) =
        IsGen.newGame(IsSettings(companyName = "Player", bots = bots, mapType = type, mapSize = size, difficulty = diff, seed = seed))

    private fun report(w: IsWorld, label: String) {
        val sb = StringBuilder("$label (${w.year}): turist ${w.present.map { it.toInt() }} kayip ${w.lost.map { it.toInt() }}\n")
        for (co in w.companies) {
            val n = w.buildings.count { it != null && it.owner == co.id }
            sb.append("   ${co.name.padEnd(12)} ${co.personality.name.padEnd(10)} bina $n  net ${IsSim.netWorth(w, co.id) / 1000}K  kasa ${co.balance / 1000}K  itibar ${co.reputation.toInt()}  kredi ${co.loanLeft / 1000}K ${if (co.bankrupt) "IFLAS" else ""}\n")
        }
        print(sb)
    }

    /** Oyuncu hic oynamazken botlar adayi gelistirmeli; hepsi batmamali, hepsi de sonsuz zengin olmamali */
    @Test
    fun botsDevelopTheIsland() {
        for (seed in listOf(5L, 9L)) {
            val w = world(bots = 4, seed = seed)
            w.settings.goal = Goal.ENDLESS
            val demand0 = w.demand.sum()
            for (y in 1..10) {
                repeat(360) { IsSim.step(w) }
                if (y % 2 == 0) report(w, "Seed $seed yil $y")
            }
            val bots = w.companies.filter { it.bot }
            val alive = bots.count { it.alive }
            val built = bots.sumOf { b -> w.buildings.count { it != null && it.owner == b.id } }
            val served = w.present.sum() / (w.demand.sum() * IsSim.SEASON[w.month])
            println("hayatta $alive/${bots.size}, bina $built, hizmet orani ${(served * 100).toInt()}%, haber ${w.news.size}")
            assertTrue("en az iki bot ayakta", alive >= 2)
            assertTrue("botlar insa etmeli", built >= 30)
            assertTrue("turistlerin cogu yer bulmali", served > 0.4)
            for (b in bots) assertTrue("asiri zenginlik", IsSim.netWorth(w, b.id) < 3_000_000_000L)
            assertTrue(w.demand.sum() > demand0)
        }
    }

    @Test
    fun largeMapFiveBotsSpeed() {
        val w = world(bots = 5, size = MapSize.LARGE, type = MapType.ARCHIPELAGO, diff = Difficulty.HARD)
        w.settings.goal = Goal.ENDLESS
        val t0 = System.nanoTime()
        repeat(360 * 6) { IsSim.step(w) }
        val ms = (System.nanoTime() - t0) / 1_000_000
        report(w, "Buyuk harita 6 yil")
        val nb = w.buildings.count { it != null }
        println("$nb bina, 6 yil $ms ms (gun basina ${ms / (360.0 * 6)} ms)")
        assertTrue(ms / (360.0 * 6) < 12.0)
    }

    @Test
    fun dirtyTricksOdds() {
        var ok = 0; var caught = 0; val trials = 400
        for (s in 0 until trials) {
            val w = world(bots = 1, seed = 100L + s)
            w.botsPaused = true
            val target = w.companies[1]
            val b = IsSim.build(w, 1, IsCatalog.types.first { it.key == "kiosk" },
                (0 until w.n).first { x -> (0 until w.n).any { y -> IsSim.canBuild(w, 1, IsCatalog.types.first { it.key == "kiosk" }, x, y) == BuildError.OK } }.let { x ->
                    x }, (0 until w.n).first { y -> IsSim.canBuild(w, 1, IsCatalog.types.first { it.key == "kiosk" }, (0 until w.n).first { x -> (0 until w.n).any { yy -> IsSim.canBuild(w, 1, IsCatalog.types.first { it.key == "kiosk" }, x, yy) == BuildError.OK } }, y) == BuildError.OK })!!
            b.buildLeft = 0
            val r = IsRivals.doDirty(w, 0, Dirty.SABOTAGE, b.id)
            if (r == 1 || (r == 2 && b.closedDays > 0)) ok++
            if (r == 2) caught++
            // Bekleme suresi
            assertTrue(!IsRivals.canDirty(w, 0, Dirty.PROPAGANDA))
            assertTrue(target.alive)
        }
        println("sabotaj basari %${ok * 100 / trials}, yakalanma %${caught * 100 / trials}")
        assertTrue(ok in (trials * 0.65).toInt()..(trials * 0.85).toInt())
        assertTrue(caught in (trials * 0.15).toInt()..(trials * 0.38).toInt())
    }

    @Test
    fun bankruptcyAndAuction() {
        val w = world(bots = 2)
        w.botsPaused = true
        val victim = w.companies[1]
        victim.balance = 50_000_000
        val t = IsCatalog.types.first { it.key == "hotel3" }
        var built: Building? = null
        loop@ for (y in 0 until w.n) for (x in 0 until w.n) if (IsSim.canBuild(w, 1, t, x, y) == BuildError.OK) { built = IsSim.build(w, 1, t, x, y); break@loop }
        val hotel = built!!
        hotel.buildLeft = 0
        victim.balance = -20_000_000
        repeat(95) { IsSim.step(w) }
        assertTrue(victim.bankrupt)
        val a = w.auctions.first { it.building == hotel.id }
        println("acik artirma min ${a.minBid}, bina degeri ${IsSim.buildingValue(hotel)}")
        w.companies[0].balance = 10_000_000
        assertTrue(IsRivals.bid(w, 0, a, IsRivals.nextBid(a)))
        repeat(IsRivals.AUCTION_DAYS + 1) { IsSim.step(w) }
        assertTrue(a.done)
        assertEquals(0, hotel.owner)
        assertEquals(0, w.owner[w.idx(hotel.x, hotel.y)])
        assertTrue(hotel.open)
    }
}
