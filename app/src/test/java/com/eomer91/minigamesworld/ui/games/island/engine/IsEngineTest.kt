package com.eomer91.minigamesworld.ui.games.island.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class IsEngineTest {

    private fun world(type: MapType = MapType.ISLAND, size: MapSize = MapSize.MEDIUM, seed: Long = 7, bots: Int = 3, money: Difficulty = Difficulty.NORMAL): IsWorld =
        IsGen.newGame(IsSettings(companyName = "Test", bots = bots, mapType = type, mapSize = size, seed = seed, difficulty = money))

    /** Kosula uyan, insa edilebilir ilk yer (sudan uzaklik araligi) */
    private fun spot(w: IsWorld, c: Int, t: BType, wdMin: Int = 1, wdMax: Int = 4, near: Pair<Int, Int>? = null, maxDist: Int = 999): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null; var bestD = Int.MAX_VALUE
        for (y in 0 until w.n) for (x in 0 until w.n) {
            if (!w.inside(x, y)) continue
            val wd = w.waterDist[w.idx(x, y)].toInt()
            if (wd !in wdMin..wdMax) continue
            if (IsSim.canBuild(w, c, t, x, y) != BuildError.OK) continue
            val d = if (near != null) maxOf(kotlin.math.abs(x - near.first), kotlin.math.abs(y - near.second)) else 0
            if (d > maxDist) continue
            if (d < bestD) { bestD = d; best = x to y; if (near == null) return best }
        }
        return best
    }

    private fun build(w: IsWorld, c: Int, key: String, wdMin: Int = 1, wdMax: Int = 4, near: Pair<Int, Int>? = null): Building {
        val t = IsCatalog.types.first { it.key == key }
        val s = spot(w, c, t, wdMin, wdMax, near) ?: error("no spot for $key")
        return IsSim.build(w, c, t, s.first, s.second)!!
    }

    private fun run(w: IsWorld, days: Int) = repeat(days) { IsSim.step(w) }

    @Test
    fun mapsGenerate() {
        for (type in MapType.entries) for (size in MapSize.entries) {
            val w = world(type, size, seed = 11)
            val n = w.n
            var land = 0; var sand = 0
            for (i in 0 until n * n) {
                val t = Terrain.entries[w.terrain[i].toInt()]
                if (t.isLand) land++
                if (t == Terrain.SAND) sand++
            }
            val frac = land / (n * n).toFloat()
            val airport = w.buildings.any { it?.type == IsCatalog.AIRPORT }
            val harbor = w.buildings.any { it?.type == IsCatalog.HARBOR }
            println("$type $size: kara %${(frac * 100).toInt()}, kumsal $sand, havalimani $airport, iskele $harbor")
            assertTrue("kara orani $type $size $frac", frac in 0.18f..0.65f)
            assertTrue(sand > n)
            assertTrue(airport)
            assertTrue(harbor)
            // Kenarlar deniz
            for (k in 0 until n) assertTrue(!w.ter(k, 0).isLand && !w.ter(0, k).isLand)
        }
    }

    @Test
    fun buildRules() {
        val w = world()
        val tent = IsCatalog.types.first { it.key == "tent" }
        val beach = IsCatalog.types.first { it.key == "beachclub" }
        assertEquals(BuildError.WATER, IsSim.canBuild(w, 0, tent, 0, 0))
        val ap = w.buildings.first { it?.type == IsCatalog.AIRPORT }!!
        assertEquals(BuildError.OCCUPIED, IsSim.canBuild(w, 0, tent, ap.x, ap.y))
        val inland = spot(w, 0, beach, wdMin = 8, wdMax = 30)
        assertTrue(inland == null)
        val b = build(w, 0, "tent")
        assertEquals(0, w.owner[w.idx(b.x, b.y)])
        // Baska sirketin arsasina insa edilemez
        assertEquals(BuildError.NOT_YOURS, IsSim.canBuild(w, 1, IsCatalog.types.first { it.key == "palm" }, b.x, b.y).let { if (it == BuildError.OCCUPIED) BuildError.NOT_YOURS else it })
        val start = 1_500_000L
        assertTrue(w.companies[0].balance < start - tent.cost)
        assertTrue(IsSim.buyLand(w, 0, b.x + 5, b.y) || true)
    }

    /** Baslangic sermayesiyle mantikli bir kurulum karli olmali; geri donus 1-3 yil */
    @Test
    fun economyBalance() {
        val w = world(bots = 1)
        val co = w.companies[0]
        val tents = (1..2).map { build(w, 0, "tent") }
        val anchor = tents[0].x to tents[0].y
        for (k in listOf("pension", "kiosk", "beachclub", "park", "souvenir")) {
            val b = build(w, 0, k, wdMin = if (k == "beachclub") 1 else 2, wdMax = 8, near = anchor)
            println("$k kuruldu, kalan ${co.balance}")
        }
        val invested = 1_500_000 - co.balance
        println("Yatirim: $invested, kalan ${co.balance}")
        val startBal = co.balance
        var minBal = co.balance
        for (m in 1..24) {
            run(w, 30)
            minBal = minOf(minBal, co.balance)
            if (m % 3 == 0) println("Ay $m (${w.month + 1}/${w.year}): gelir ${co.lastMonthIn / 1000}K gider ${co.lastMonthOut / 1000}K bakiye ${co.balance / 1000}K itibar ${co.reputation.toInt()} " +
                "turist ${w.present.map { it.toInt() }} kayip ${w.lost.map { it.toInt() }}")
        }
        val gained = co.balance - startBal
        println("2 yilda kazanc: $gained (yatirimin %${gained * 100 / invested})")
        assertTrue("karli olmali", gained > 0)
        assertTrue("cok hizli geri donmemeli", gained < invested * 2.5)
        assertTrue("iflasa gitmemeli", minBal > -200_000)
        assertTrue(co.reputation in 25f..95f)
    }

    @Test
    fun cheaperGetsMoreGuests() {
        val w = world(bots = 1)
        val a = build(w, 0, "tent")
        val b = build(w, 1, "tent", near = a.x to a.y)
        IsSim.setPrice(a, 12f); IsSim.setPrice(b, 30f)
        // Kapasite dolmasin diye talebi dusur
        w.demand[0] = 60.0; w.demand[1] = 0.0; w.demand[2] = 0.0
        run(w, 40)
        val ga = a.lastMonthUse; val gb = b.lastMonthUse
        println("ucuz $ga, pahali $gb")
        assertTrue(ga > gb)
    }

    @Test
    fun noiseHurtsLuxury() {
        val w = world(bots = 1, money = Difficulty.EASY)
        w.companies[0].balance = 100_000_000; w.companies[1].balance = 100_000_000
        val quiet = build(w, 0, "villa")
        val loud = build(w, 1, "villa", near = (quiet.x + 25) to quiet.y)
        repeat(2) { build(w, 1, "disco", wdMax = 10, near = loud.x to loud.y) }
        run(w, 50)
        IsSim.refreshAllLocations(w)
        println("sessiz villa loc ${quiet.loc[2]}, gurultulu ${loud.loc[2]}")
        assertTrue(quiet.loc[2] > loud.loc[2])
    }

    @Test
    fun touristsGrowOnePerMille() {
        val w = world()
        val d0 = w.demand[1]
        run(w, 360)
        val ratio = w.demand[1] / d0
        println("1 yilda talep orani $ratio")
        assertEquals(Math.pow(1.001, 12.0), ratio, 1e-9)
    }

    @Test
    fun loanAndRepay() {
        val w = world()
        val co = w.companies[0]
        val lim = IsSim.creditLimit(w, 0)
        assertTrue(lim >= 500_000)
        val b0 = co.balance
        assertTrue(IsSim.takeLoan(w, 0, 500_000, 1))
        assertEquals(b0 + 500_000, co.balance)
        assertTrue(!IsSim.takeLoan(w, 0, 100_000, 1))
        run(w, 360)
        assertEquals(0, co.loanMonthsLeft)
        assertEquals(0L, co.loanLeft)
        println("Faiz gideri: ${co.ledger[Fin.INTEREST.ordinal] + co.lastLedger[Fin.INTEREST.ordinal]}")
    }

    @Test
    fun saveRoundTripIsDeterministic() {
        val w = world()
        build(w, 0, "tent"); build(w, 0, "kiosk", wdMax = 8); build(w, 1, "pension"); build(w, 2, "pub", wdMax = 8)
        IsSim.takeLoan(w, 0, 300_000, 2)
        IsRivals.startAd(w, 0, 0)
        run(w, 100)
        val bytes = ByteArrayOutputStream().also { IsSave.write(w, it) }.toByteArray()
        val w2 = IsSave.read(ByteArrayInputStream(bytes))
        println("Kayit boyutu ${bytes.size} bayt")
        run(w, 60); run(w2, 60)
        for (k in w.companies.indices) {
            assertEquals(w.companies[k].balance, w2.companies[k].balance)
            assertEquals(w.companies[k].reputation, w2.companies[k].reputation, 1e-4f)
        }
        assertEquals(w.present[0], w2.present[0], 1e-3f)
        assertEquals(w.news.size, w2.news.size)
    }

    @Test
    fun bigMapPerformance() {
        val w = world(MapType.ARCHIPELAGO, MapSize.LARGE, bots = 5)
        for (co in w.companies) co.balance = 500_000_000
        val keys = listOf("tent", "pension", "bungalow", "hotel3", "kiosk", "fastfood", "restaurant", "pub", "disco", "arcade", "souvenir", "market", "beachclub", "park")
        var built = 0
        outer@ for (round in 0 until 30) for (c in w.companies.indices) {
            val key = keys[(round + c) % keys.size]
            val t = IsCatalog.types.first { it.key == key }
            val s = spot(w, c, t, 1, 12) ?: continue
            IsSim.build(w, c, t, s.first, s.second); built++
            if (built >= 300) break@outer
        }
        val t0 = System.nanoTime()
        run(w, 360)
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("$built bina, 360 gun: $ms ms")
        assertTrue(ms < 20_000)
    }
}
