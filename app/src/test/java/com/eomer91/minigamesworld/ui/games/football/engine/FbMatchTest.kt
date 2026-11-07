package com.eomer91.minigamesworld.ui.games.football.engine

import org.junit.Assert.assertTrue
import org.junit.Test

class FbMatchTest {
    private val world by lazy { FbGen(2026L).generate() }

    private class Agg {
        var n = 0; var hg = 0; var ag = 0; var hw = 0; var d = 0; var aw = 0
        var shots = 0; var sot = 0; var corners = 0; var fouls = 0; var yel = 0; var red = 0; var off = 0
        var possH = 0.0; var passPct = 0.0; var xg = 0.0
        fun add(m: MatchSim) {
            n++; hg += m.homeGoals; ag += m.awayGoals
            if (m.homeGoals > m.awayGoals) hw++ else if (m.homeGoals == m.awayGoals) d++ else aw++
            for (s in listOf(m.home, m.away)) {
                shots += s.stats.shots; sot += s.stats.onTarget; corners += s.stats.corners; fouls += s.stats.fouls
                yel += s.stats.yellows; red += s.stats.reds; off += s.stats.offsides; xg += s.stats.xg
                passPct += s.stats.passesOk.toDouble() / s.stats.passes.coerceAtLeast(1)
            }
            possH += m.home.stats.possessionSec.toDouble() / (m.home.stats.possessionSec + m.away.stats.possessionSec)
        }
        fun goals() = (hg + ag).toDouble() / n
        override fun toString() = "maç=%d gol=%.2f (ev %.2f dep %.2f) G/B/M=%.0f%%/%.0f%%/%.0f%% şut=%.1f isabet=%.1f xG=%.2f korner=%.1f faul=%.1f sarı=%.2f kırmızı=%.2f ofsayt=%.1f pas%%=%.0f evTopla=%.0f%%".format(
            n, goals(), hg.toDouble() / n, ag.toDouble() / n, hw * 100.0 / n, d * 100.0 / n, aw * 100.0 / n,
            shots.toDouble() / n, sot.toDouble() / n, xg / n, corners.toDouble() / n, fouls.toDouble() / n, yel.toDouble() / n, red.toDouble() / n, off.toDouble() / n, passPct * 50 / n, possH * 100 / n)
    }

    @Test
    fun calibration() {
        val agg = Agg()
        val t0 = System.currentTimeMillis()
        var seed = 1L
        for (lg in world.leagues.filter { it.tier == 1 }) {
            val clubs = lg.clubs
            repeat(30) {
                val h = clubs[(seed * 7 % clubs.size).toInt()]
                var a = clubs[(seed * 13 % clubs.size).toInt()]
                if (a == h) a = clubs[(clubs.indexOf(h) + 1) % clubs.size]
                agg.add(FbLineup.aiMatch(world, h, a, seed++).playToEnd())
            }
        }
        val ms = (System.currentTimeMillis() - t0).toDouble() / agg.n
        println("KALIBRASYON: $agg")
        println("mac basina %.2f ms".format(ms))
        assertTrue("gol ort ${agg.goals()}", agg.goals() in 2.3..3.2)
        assertTrue("ev galibiyet", agg.hw * 100.0 / agg.n in 38.0..52.0)
        assertTrue("beraberlik", agg.d * 100.0 / agg.n in 18.0..32.0)
    }

    /** Lig sezonu: guclu takimlar ustte bitmeli. */
    @Test
    fun leagueSeasonOrder() {
        val eng = world.leaguesOf(FbCountries.all.indexOfFirst { it.code == "ENG" })[0]
        val clubs = eng.clubs
        val pts = HashMap<Int, Int>()
        val gf = HashMap<Int, Int>(); val ga = HashMap<Int, Int>()
        var seed = 100L
        val agg = Agg()
        for (h in clubs) for (a in clubs) if (h != a) {
            val m = FbLineup.aiMatch(world, h, a, seed++).playToEnd()
            agg.add(m)
            val (ph, pa) = when { m.homeGoals > m.awayGoals -> 3 to 0; m.homeGoals == m.awayGoals -> 1 to 1; else -> 0 to 3 }
            pts[h] = (pts[h] ?: 0) + ph; pts[a] = (pts[a] ?: 0) + pa
            gf[h] = (gf[h] ?: 0) + m.homeGoals; ga[h] = (ga[h] ?: 0) + m.awayGoals
            gf[a] = (gf[a] ?: 0) + m.awayGoals; ga[a] = (ga[a] ?: 0) + m.homeGoals
        }
        val strength = clubs.associateWith { c -> FbLineup.teamStrength(world.clubPlayers(c), FbLineup.bestFormation(world.clubPlayers(c))) }
        val table = clubs.sortedByDescending { pts[it]!! * 1000 + (gf[it]!! - ga[it]!!) }
        println("ENG1 sezon: $agg")
        for ((i, c) in table.withIndex()) println("%2d. %-24s P=%3d A=%3d Y=%3d guc=%.2f".format(i + 1, world.clubs[c].name, pts[c], gf[c], ga[c], strength[c]))
        // Spearman siralama korelasyonu
        val byStr = clubs.sortedByDescending { strength[it]!! }
        val n = clubs.size
        val d2 = clubs.sumOf { c -> val dd = table.indexOf(c) - byStr.indexOf(c); dd * dd }
        val rho = 1 - 6.0 * d2 / (n * (n * n - 1))
        println("guc-siralama korelasyonu: %.2f".format(rho))
        assertTrue(rho > 0.6)
        val champPts = pts[table[0]]!!; val lastPts = pts[table.last()]!!
        assertTrue("sampiyon puani $champPts", champPts in 72..102)
        assertTrue("son puan $lastPts", lastPts in 12..40)
    }

    /** Ayni iki takim, taktik degisiminin etkisi (A/B). */
    @Test
    fun tacticEffects() {
        val lg = world.leaguesOf(FbCountries.all.indexOfFirst { it.code == "ESP" })[0]
        val a = lg.clubs[3]; val b = lg.clubs[8]
        val ap = world.clubPlayers(a); val bp = world.clubPlayers(b)
        val af = FbLineup.bestFormation(ap); val bf = FbLineup.bestFormation(bp)
        fun run(name: String, ta: Tactics, tb: Tactics = Tactics(formation = bf), n: Int = 1500): Agg {
            val agg = Agg(); var gd = 0; var cond = 0.0
            for (k in 0 until n) {
                val m = MatchSim(FbLineup.side(world, a, ta, true), FbLineup.side(world, b, tb, false), 5000L + k).playToEnd()
                agg.add(m); gd += m.homeGoals - m.awayGoals
                cond += m.home.active.map { it.condition }.average()
            }
            println("%-34s ortGF=%+.2f gol=%.2f atılan=%.2f yenen=%.2f şut=%.1f evTopla=%.0f%% kondisyon=%.0f".format(name, gd.toDouble() / n, agg.goals(), agg.hg.toDouble() / n, agg.ag.toDouble() / n, agg.shots.toDouble() / n, agg.possH * 100 / n, cond / n))
            return agg
        }
        val base = Tactics(formation = af)
        val b0 = run("Dengeli", base)
        val att = run("Tam hücum", base.copy(mentality = Mentality.VERY_ATTACKING))
        val def = run("Çok defansif", base.copy(mentality = Mentality.VERY_DEFENSIVE))
        run("Kısa pas + yavaş tempo", base.copy(passing = PassingStyle.SHORT, tempo = Tempo.SLOW))
        run("Uzun top + hızlı tempo", base.copy(passing = PassingStyle.DIRECT, tempo = Tempo.FAST))
        run("Yüksek pres", base.copy(pressing = Pressing.HIGH))
        run("Yüksek savunma hattı", base.copy(defLine = DefLine.HIGH))
        run("Derin savunma + kontra", base.copy(defLine = DefLine.DEEP, counter = true, mentality = Mentality.DEFENSIVE))
        run("Kanat + geniş + orta", base.copy(focus = AttackFocus.FLANKS, width = Width.WIDE))
        run("Ofsayt taktiği", base.copy(offsideTrap = true, defLine = DefLine.HIGH))
        run("Sert müdahale", base.copy(hardTackling = true))
        run("Kontra vs rakip tam hücum", base.copy(counter = true, mentality = Mentality.DEFENSIVE), Tactics(formation = bf, mentality = Mentality.VERY_ATTACKING))
        run("Dengeli vs rakip tam hücum", base, Tactics(formation = bf, mentality = Mentality.VERY_ATTACKING))
        // Beklenen yonler
        assertTrue("hucum daha cok gol", att.goals() > b0.goals())
        assertTrue("defans daha az gol", def.goals() < b0.goals())
    }

    @Test
    fun knockoutHasWinner() {
        val lg = world.leagues[0]
        var shootouts = 0
        for (k in 0 until 200) {
            val m = FbLineup.aiMatch(world, lg.clubs[k % 10], lg.clubs[10 + k % 10], 900L + k, needWinner = true).playToEnd()
            val so = m.shootout
            if (so != null) { shootouts++; assertTrue(so.first != so.second) } else assertTrue(m.homeGoals != m.awayGoals)
        }
        println("200 eleme macinda penalti: $shootouts")
    }
}
