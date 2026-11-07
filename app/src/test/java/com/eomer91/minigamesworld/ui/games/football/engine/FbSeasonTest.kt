package com.eomer91.minigamesworld.ui.games.football.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FbSeasonTest {

    @Test
    fun calendarDates() {
        val cal = FbCalendar(2026)
        assertEquals(FbCalendar.WED, cal.weekday(0)) // 1 Temmuz 2026 Carsamba
        assertEquals(Triple(2027, 6, 30), cal.date(364))
        assertEquals(Triple(2026, 12, 25), cal.date(cal.dayOf(12, 25)))
        val days = cal.leagueRoundDays(38, true)
        assertEquals(38, days.size)
        assertEquals(days.size, days.distinct().size)
        assertTrue(days.all { it !in cal.intlSaturdays })
        assertTrue(days.count { cal.weekday(it) == FbCalendar.SAT } >= 30)
        val lower = cal.leagueRoundDays(46, false)
        assertEquals(46, lower.size)
        assertEquals(46, lower.distinct().size)
        println("2026/27 lig: ${cal.date(days.first())} .. ${cal.date(days.last())}, CL finali ${cal.date(cal.clFinal)}, kupa finali ${cal.date(cal.cupDays.last())}")
    }

    @Test
    fun funnel() {
        assertEquals(listOf(21, 11, 6), FbSeason.funnel(42, 6))
        assertEquals(listOf(21, 13), FbSeason.funnel(42, 13))
        assertEquals(emptyList<Int>(), FbSeason.funnel(5, 6))
    }

    private fun playSeason(w: FbWorld, check: ((FbWorld) -> Unit)? = null) {
        var r: DayReport
        do { check?.invoke(w); r = FbSeason.advanceDay(w, autoUser = true) } while (!r.seasonEnded)
    }

    @Test
    fun fullSeason() {
        val w = FbGen(21L).generate()
        FbSeason.start(w)
        val st = w.state!!
        val sizes = w.leagues.map { it.clubs.size }
        var clashes = 0
        var leagueGoals = 0; var leagueGames = 0; var homeWins = 0; var draws = 0
        var day363checked = false
        playSeason(w) { world ->
            val today = FbSeason.todayFixtures(world)
            val seen = HashSet<Long>()
            for (f in today) {
                val nat = if (st.comps[f.comp].national) 1L shl 40 else 0L
                if (!seen.add(nat or f.home.toLong())) clashes++
                if (!seen.add(nat or f.away.toLong())) clashes++
            }
            if (world.day == 363) {
                day363checked = true
                assertEquals("oynanmamis mac", 0, st.fixtures.count { !it.played && it.day < 363 })
                for (c in st.comps) if (c.kind.official) assertTrue("bitmedi ${c.kind} ${c.country}", c.done)
                for (k in listOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE)) {
                    val c = st.comps.first { it.kind == k }
                    assertEquals(36, c.stages.first { it.type == StageType.SWISS }.groups[0].size)
                    assertTrue(c.winner >= 0)
                }
                for (f in st.fixtures) if (st.comps[f.comp].kind == CompKind.LEAGUE) {
                    leagueGames++; leagueGoals += f.hg + f.ag
                    if (f.hg > f.ag) homeWins++ else if (f.hg == f.ag) draws++
                }
                // Lig sampiyonu puani makul
                val eng = st.comps.first { it.kind == CompKind.LEAGUE && FbCountries.all[it.country].code == "ENG" && w.leagues[it.league].tier == 1 }
                val t = st.table(eng.id, 0, 0)
                println("ENG: sampiyon ${t.first().pts} puan, son ${t.last().pts} puan")
                assertTrue(t.first().pts in 70..100)
                assertTrue(t.last().pts in 10..45)
            }
        }
        assertTrue(day363checked)
        assertEquals("ayni gun iki mac", 0, clashes)
        val g = leagueGoals.toDouble() / leagueGames
        println("lig maci=$leagueGames gol/mac=%.2f ev=%.0f%% beraberlik=%.0f%%".format(g, homeWins * 100.0 / leagueGames, draws * 100.0 / leagueGames))
        assertTrue(g in 2.3..3.0)
        assertTrue(homeWins * 100.0 / leagueGames in 38.0..52.0)
        assertTrue(draws * 100.0 / leagueGames in 18.0..32.0)
        // Yeni sezon kuruldu, ligler ayni boyutta, dusme/cikma oldu
        assertEquals(FbWorld.START_SEASON + 1, w.season)
        assertEquals(sizes, w.leagues.map { it.clubs.size })
        val sum = FbSeason.lastSummary!!
        assertTrue(sum.promoted.size == sum.relegated.size && sum.promoted.isNotEmpty())
        for (c in w.clubs) if (c.league >= 0) assertTrue(c.id in w.leagues[c.league].clubs)
        assertTrue(st.honours.any { it.kind == CompKind.NATIONS_LEAGUE })
    }

    @Test
    fun saveMidSeason() {
        val w = FbGen(5L).generate()
        FbSeason.start(w)
        while (w.day < 140) FbSeason.advanceDay(w, autoUser = true)
        w.formationCache.clear()
        val bytes = ByteArrayOutputStream().also { FbSave.write(w, it) }.toByteArray()
        val w2 = FbSave.read(ByteArrayInputStream(bytes))
        println("kayit boyutu (gun 140): ${bytes.size / 1024} KB")
        assertEquals(w.day, w2.day)
        assertEquals(w.state!!.fixtures.size, w2.state!!.fixtures.size)
        assertEquals(w.state!!.stats.size, w2.state!!.stats.size)
        repeat(40) { FbSeason.advanceDay(w, autoUser = true); FbSeason.advanceDay(w2, autoUser = true) }
        val a = w.state!!.fixtures.map { "${it.hg}:${it.ag}" }
        val b = w2.state!!.fixtures.map { "${it.hg}:${it.ag}" }
        assertEquals("kayittan devam ayni sonuclari vermeli", a, b)
    }

    @Test
    fun userMatchWaits() {
        val w = FbGen(3L).generate()
        val lg = w.leagues.first { FbCountries.all[it.country].code == "TUR" && it.tier == 1 }
        w.userClub = lg.clubs[0]
        FbSeason.start(w)
        var r: DayReport
        do { r = FbSeason.advanceDay(w) } while (r.pending == null)
        val f = r.pending!!
        val dayBefore = w.day
        // Bekleyen mac oynanmadan gun ilerlemez
        assertEquals(dayBefore, FbSeason.advanceDay(w).let { w.day })
        // Arayuz: tam simulasyonla oyna ve gonder
        val st = w.state!!
        val comp = st.comps[f.comp]
        val hs = FbSeason.sheet(w, f.home, comp.national); val aws = FbSeason.sheet(w, f.away, comp.national)
        val sim = MatchSim(FbSeason.sideOf(hs, "h", Tactics(formation = hs.formation), true), FbSeason.sideOf(aws, "a", Tactics(formation = aws.formation), false), 1L,
            userSide = if (f.home == w.userClub) 0 else 1).playToEnd()
        FbSeason.submit(w, f, FbQuick.fromSim(sim))
        assertTrue(f.played)
        r = FbSeason.advanceDay(w)
        assertTrue(r.pending == null)
        assertEquals(dayBefore + 1, w.day)
    }

    @Test
    fun internationalCycle() {
        val w = FbGen(13L).generate()
        FbSeason.start(w)
        playSeason(w) // 2026/27 Uluslar Ligi
        val st = w.state!!
        assertTrue(st.comps.any { it.kind == CompKind.EURO })
        assertTrue(st.comps.count { it.kind == CompKind.QUALIFIERS } == 1)
        playSeason(w) // 2027/28 EURO 2028
        val euro = st.honours.firstOrNull { it.kind == CompKind.EURO }
        assertTrue(euro != null && FbCountries.all[euro.winner].confed == Confed.UEFA)
        println("EURO 2028: ${FbCountries.all[euro!!.winner].englishName} - ${FbCountries.all[euro.runnerUp].englishName}")
        playSeason(w); playSeason(w) // 2029/30 Dunya Kupasi
        val wc = st.honours.firstOrNull { it.kind == CompKind.WORLD_CUP }
        assertTrue(wc != null)
        println("Dunya Kupasi 2030: ${FbCountries.all[wc!!.winner].englishName} - ${FbCountries.all[wc.runnerUp].englishName}")
        val top = FbSeason.uefaRanking(w).take(5).map { FbCountries.all[it].code }
        println("UEFA ilk 5: $top")
    }
}
