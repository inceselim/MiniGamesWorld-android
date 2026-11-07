package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Bir gunun sonucu. [pending] doluysa kullanicinin maci oynanmayi bekliyor ve gun ilerlemedi. */
class DayReport(val day: Int, val played: List<Int>, val pending: FbFixture?, val seasonEnded: Boolean)

/** Sezon sonu ozeti (haber/arayuz icin, kaydedilmez). */
class SeasonSummary(val season: Int) {
    val promoted = ArrayList<Pair<Int, Int>>() // (kulup, yeni lig)
    val relegated = ArrayList<Pair<Int, Int>>()
}

/**
 * Sezon motoru: takvim kurulumu, gun gun ilerleme, eleme turlarinin kurulmasi,
 * Avrupa kupalarina katilim, milli takim organizasyonlari, sezon sonu dusme/cikma.
 */
object FbSeason {

    // ================================================================== kurulum

    /** Yeni dunyada ilk sezonu kurar (katsayi ve Elo baslangic degerleri dahil). */
    fun start(world: FbWorld) {
        val st = FbSeasonState(FbCountries.all.size)
        world.state = st
        for ((ci, c) in FbCountries.all.withIndex()) {
            val base = 1f + 19f * ((c.level - 20f) / 80f).coerceIn(0f, 1f).pow(2f)
            for (k in 0 until 5) st.coef[ci][k] = base * (0.9f + 0.04f * k)
        }
        val byNation = nationPools(world)
        for (ci in FbCountries.all.indices) {
            val s = nationalSheet(world, ci, byNation)
            st.elo[ci] = 1500f + (s.strength - 10f) * 90f
        }
        FbClubLife.init(world)
        buildSeason(world, null)
    }

    /** Onceki sezondan devreden bilgiler (Avrupa katilimi, super kupalar). */
    class Carry(
        val leagueOrder: Map<Int, IntArray>, // ulke -> 1. lig son siralama
        val cupWinner: Map<Int, Int>,
        val clWinner: Int, val elWinner: Int,
    )

    private fun rng(world: FbWorld, salt: Int) = FbRandom(world.seed xor (world.season.toLong() * 7919L) xor (salt.toLong() * 104729L))

    fun calendar(world: FbWorld) = FbCalendar(world.season)

    private fun buildSeason(world: FbWorld, carry: Carry?) {
        val st = world.state!!
        val cal = calendar(world)
        val rnd = rng(world, 1)
        st.comps.clear(); st.fixtures.clear(); st.stats.clear(); st.yellowAcc.clear(); st.byDay = HashMap(); st.byComp = HashMap()
        st.coefPts.fill(0f); st.coefClubs.fill(0)

        // ---- ligler
        for (lg in world.leagues) {
            if (lg.clubs.size < 2) continue
            val comp = newComp(st, CompKind.LEAGUE, lg.country, lg.id)
            val n = lg.clubs.size
            val cycles = if (n <= 10) 4 else if (n <= 12) 3 else 2
            val rounds = (if (n % 2 == 0) n - 1 else n) * cycles
            val stage = FbStage(StageType.TABLE, StageLabel.LEAGUE, cycles, cal.leagueRoundDays(rounds, lg.tier == 1))
            stage.groups.add(lg.clubs.toIntArray())
            comp.stages.add(stage)
            buildTable(st, comp, 0, rnd)
            // Play-off (yukselme): otomatik = promoted-1, 4 takim play-off
            if (lg.playoffSpots >= 4 && lg.promoted >= 2 && n >= 8) {
                val po = newComp(st, CompKind.PLAYOFF, lg.country, lg.id)
                po.stages.add(FbStage(StageType.KO, StageLabel.SF, 2, intArrayOf(cal.playoffDays[0], cal.playoffDays[1])))
                po.stages.add(FbStage(StageType.KO, StageLabel.FINAL, 1, intArrayOf(cal.playoffDays[2])))
            }
        }

        // ---- ulusal kupalar ve super kupalar
        for ((ci, _) in FbCountries.all.withIndex()) {
            val tiers = world.leaguesOf(ci)
            if (tiers.isEmpty()) continue
            val entrants = tiers.take(3).flatMap { lg -> lg.clubs.sortedByDescending { world.clubs[it].reputation } }.take(128)
            if (entrants.size >= 4) {
                val comp = newComp(st, CompKind.CUP, ci)
                val k = ceil(ln(entrants.size.toDouble()) / ln(2.0)).toInt().coerceAtMost(cal.cupDays.size)
                val days = cal.cupDays.takeLast(k)
                for ((r, d) in days.withIndex()) {
                    val teamsBefore = 1 shl (k - r)
                    comp.stages.add(FbStage(StageType.KO, StageLabel.forTeams(teamsBefore), 1, intArrayOf(d))
                        .also { it.target = teamsBefore / 2 })
                }
                buildKo(world, comp, 0, entrants, rnd, seeded = true)
            }
            // Super kupa: sampiyon - kupa galibi
            val top = tiers[0].clubs
            if (top.size >= 2) {
                val order = carry?.leagueOrder?.get(ci)?.filter { it in top }?.toIntArray()?.takeIf { it.size >= 2 } ?: top.sortedByDescending { world.clubs[it].reputation }.toIntArray()
                val champ = order[0]
                var cupW = carry?.cupWinner?.get(ci) ?: order[min(order.size - 1, 1 + rnd.int(min(4, order.size - 1)))]
                if (cupW == champ || cupW !in top) cupW = order.first { it != champ }
                val sc = newComp(st, CompKind.SUPER_CUP, ci)
                sc.stages.add(FbStage(StageType.KO, StageLabel.FINAL, 1, intArrayOf(cal.domesticSuperCup)))
                buildKo(world, sc, 0, listOf(champ, cupW), rnd, seeded = true)
            }
        }

        // ---- Avrupa kupalari
        buildEurope(world, cal, carry, rnd)

        // ---- milli takimlar
        buildNational(world, cal, rnd)
    }

    private fun newComp(st: FbSeasonState, kind: CompKind, country: Int = -1, league: Int = -1, confed: Int = -1): FbComp {
        val c = FbComp(st.comps.size, kind, country, league, confed)
        st.comps.add(c)
        return c
    }

    // ================================================================== Avrupa

    /** UEFA ulke siralamasi (ligi olan ulkeler, katsayiya gore). */
    fun uefaRanking(world: FbWorld): List<Int> {
        val st = world.state!!
        return FbCountries.all.indices.filter { FbCountries.all[it].confed == Confed.UEFA && world.leaguesOf(it).isNotEmpty() && world.leaguesOf(it)[0].clubs.size >= 2 }
            .sortedByDescending { st.coef[it].sum() }
    }

    private fun buildEurope(world: FbWorld, cal: FbCalendar, carry: Carry?, rnd: FbRandom) {
        val st = world.state!!
        val rank = uefaRanking(world)
        // Ulke bazinda siralama ve kupa galibi
        val order = HashMap<Int, IntArray>()
        val cupW = HashMap<Int, Int>()
        for (ci in rank) {
            val top = world.leaguesOf(ci)[0].clubs
            val prev = carry?.leagueOrder?.get(ci)?.filter { it in top }
            val o = if (prev != null) (prev + top.filter { it !in prev }.sortedByDescending { world.clubs[it].reputation }).toIntArray()
                else top.sortedByDescending { world.clubs[it].reputation + rnd.int(800) }.toIntArray()
            order[ci] = o
            cupW[ci] = carry?.cupWinner?.get(ci)?.takeIf { it in top } ?: o[min(o.size - 1, rnd.int(min(6, o.size)))]
        }
        val taken = HashSet<Int>()
        fun nextLeague(ci: Int): Int? = order[ci]!!.firstOrNull { it !in taken }?.also { taken.add(it) }
        fun cup(ci: Int): Int? { val c = cupW[ci]!!; return if (c !in taken) c.also { taken.add(it) } else nextLeague(ci) }

        val clDirect = ArrayList<Int>(); val clQual = ArrayList<Int>()
        val elDirect = ArrayList<Int>(); val elQual = ArrayList<Int>()
        val eclDirect = ArrayList<Int>(); val eclQual = ArrayList<Int>()
        for ((r, ci) in rank.withIndex()) {
            val n = when { r < 4 -> 4; r < 6 -> 3; r < 8 -> 2; r < 12 -> 1; else -> 0 }
            repeat(n) { nextLeague(ci)?.let { clDirect.add(it) } }
            if (r >= 12) nextLeague(ci)?.let { clQual.add(it) }
        }
        for ((r, ci) in rank.withIndex()) {
            if (r < 12) { cup(ci)?.let { elDirect.add(it) }; if (r < 6) nextLeague(ci)?.let { elDirect.add(it) } }
            else cup(ci)?.let { elQual.add(it) }
        }
        for ((r, ci) in rank.withIndex()) {
            nextLeague(ci)?.let { if (r < 12) eclDirect.add(it) else eclQual.add(it) }
        }
        for (c in clDirect + clQual + elDirect + elQual + eclDirect + eclQual) {
            val ci = world.clubs[c].country; st.coefClubs[ci]++
        }

        val cl = euroComp(world, CompKind.CHAMPIONS, cal, 0, cal.clFinal, clDirect, clQual, 6, rnd)
        val el = euroComp(world, CompKind.EUROPA, cal, 2, cal.elFinal, elDirect, elQual, 13, rnd)
        val ecl = euroComp(world, CompKind.CONFERENCE, cal, 2, cal.eclFinal, eclDirect, eclQual, 16, rnd)
        // On eleme son turu kaybedenleri bir alt kupanin lig asamasina
        cl.stages.lastOrNull { it.label in QUAL_LABELS }?.losersTo = el.id
        el.stages.lastOrNull { it.label in QUAL_LABELS }?.losersTo = ecl.id

        // UEFA Super Kupa
        val us = newComp(st, CompKind.UEFA_SUPER)
        us.stages.add(FbStage(StageType.KO, StageLabel.FINAL, 1, intArrayOf(cal.uefaSuperCup)))
        val a = carry?.clWinner?.takeIf { it >= 0 } ?: order[rank[0]]!![0]
        val b = carry?.elWinner?.takeIf { it >= 0 && it != a } ?: order[rank[1]]!![0]
        buildKo(world, us, 0, listOf(a, b), rnd, seeded = true)
    }

    private val QUAL_LABELS = setOf(StageLabel.Q1, StageLabel.Q2, StageLabel.Q3, StageLabel.QPO)

    private fun euroComp(world: FbWorld, kind: CompKind, cal: FbCalendar, dayShift: Int, finalDay: Int, direct: List<Int>, qual: List<Int>, qualSlots: Int, rnd: FbRandom): FbComp {
        val st = world.state!!
        val comp = newComp(st, kind)
        // On eleme hunisi
        val targets = funnel(qual.size, qualSlots).take(4)
        val qLabels = listOf(StageLabel.Q1, StageLabel.Q2, StageLabel.Q3, StageLabel.QPO).takeLast(targets.size)
        val qDays = cal.euroQualTuesdays.toList().chunked(2).takeLast(targets.size)
        for ((i, t) in targets.withIndex()) {
            comp.stages.add(FbStage(StageType.KO, qLabels[i], 2, intArrayOf(qDays[i][0] + dayShift, qDays[i][1] + dayShift)).also { it.target = t })
        }
        val lp = FbStage(StageType.SWISS, StageLabel.LEAGUE_PHASE, 1, cal.euroLeagueTuesdays.map { it + dayShift }.toIntArray())
        lp.extra.addAll(direct.sortedByDescending { world.clubs[it].reputation })
        comp.stages.add(lp)
        val ko = cal.euroKoTuesdays.map { it + dayShift }
        comp.stages.add(FbStage(StageType.KO, StageLabel.KPO, 2, intArrayOf(ko[0], ko[1])).also { it.target = 8 })
        comp.stages.add(FbStage(StageType.KO, StageLabel.R16, 2, intArrayOf(ko[2], ko[3])).also { it.target = 8 })
        comp.stages.add(FbStage(StageType.KO, StageLabel.QF, 2, intArrayOf(ko[4], ko[5])).also { it.target = 4 })
        comp.stages.add(FbStage(StageType.KO, StageLabel.SF, 2, intArrayOf(ko[6], ko[7])).also { it.target = 2 })
        comp.stages.add(FbStage(StageType.KO, StageLabel.FINAL, 1, intArrayOf(finalDay)).also { it.target = 1 })
        if (targets.isNotEmpty()) buildKo(world, comp, 0, qual.sortedByDescending { world.clubs[it].reputation }, rnd, seeded = true)
        return comp
    }

    /** N takimdan K takima eleme turlari: her turda tur atlayan sayi. */
    fun funnel(n0: Int, k: Int): List<Int> {
        val out = ArrayList<Int>(); var n = n0
        while (n > k && n > 1) { val m = min(n - k, n / 2); n -= m; out.add(n) }
        return out
    }

    // ================================================================== milli takimlar

    private fun buildNational(world: FbWorld, cal: FbCalendar, rnd: FbRandom) {
        val st = world.state!!
        val y = world.season
        val all = FbCountries.all
        val uefa = all.indices.filter { all[it].confed == Confed.UEFA }.sortedByDescending { st.elo[it] }
        val busy = HashMap<Int, HashSet<Int>>() // gun -> oynayan milli takimlar
        fun markBusy(c: FbComp) {
            for (f in st.compFixtures(c.id)) { busy.getOrPut(f.day) { HashSet() }.apply { add(f.home); add(f.away) } }
        }
        val qDays = cal.intlDays.take(6).toIntArray()
        if (y % 2 == 0) {
            // Uluslar Ligi: A, B, C 16'sar (4x4), D kalan
            val nl = newComp(st, CompKind.NATIONS_LEAGUE)
            val stage = FbStage(StageType.TABLE, StageLabel.GROUPS, 2, qDays)
            var idx = 0
            for (tierN in 0 until 4) {
                val teams = if (tierN < 3) uefa.drop(idx).take(16) else uefa.drop(idx)
                idx += teams.size
                if (teams.isEmpty()) continue
                stage.groups.addAll(drawGroups(teams, groupCount(teams.size), rnd))
            }
            nl.stages.add(stage)
            nl.stages.add(FbStage(StageType.KO, StageLabel.SF, 1, intArrayOf(cal.juneDays[0])).also { it.target = 2 })
            nl.stages.add(FbStage(StageType.KO, StageLabel.FINAL, 1, intArrayOf(cal.juneDays[2])).also { it.target = 1 })
            buildTable(st, nl, 0, rnd)
            markBusy(nl)
            // Haziran: A ligi disindakiler hazirlik maci
            val aTeams = stage.groups.take(4).flatMap { it.toList() }.toSet()
            friendlies(world, intArrayOf(cal.juneDays[0], cal.juneDays[2]), all.indices.filter { it !in aTeams }, busy, rnd)
            friendlies(world, intArrayOf(cal.juneDays[1], cal.juneDays[3]), all.indices.toList(), busy, rnd)
        } else {
            val euro = (y + 1) % 4 == 0
            val tour = newComp(st, if (euro) CompKind.EURO else CompKind.WORLD_CUP)
            val td = cal.tournamentDays
            tour.stages.add(FbStage(StageType.TABLE, StageLabel.GROUPS, 1, intArrayOf(td[0], td[1], td[2])))
            if (!euro) tour.stages.add(FbStage(StageType.KO, StageLabel.R32, 1, intArrayOf(td[3])).also { it.target = 16 })
            tour.stages.add(FbStage(StageType.KO, StageLabel.R16, 1, intArrayOf(td[4])).also { it.target = 8 })
            tour.stages.add(FbStage(StageType.KO, StageLabel.QF, 1, intArrayOf(td[5])).also { it.target = 4 })
            tour.stages.add(FbStage(StageType.KO, StageLabel.SF, 1, intArrayOf(td[6])).also { it.target = 2 })
            tour.stages.add(FbStage(StageType.KO, StageLabel.FINAL, 1, intArrayOf(td[7])).also { it.target = 1 })
            val confeds = if (euro) listOf(Confed.UEFA) else Confed.entries
            for (cf in confeds) {
                val teams = all.indices.filter { all[it].confed == cf }.sortedByDescending { st.elo[it] }
                if (teams.size < 2) { tour.stages[0].extra.addAll(teams); continue }
                val q = newComp(st, CompKind.QUALIFIERS, confed = cf.ordinal)
                val stage = FbStage(StageType.TABLE, StageLabel.GROUPS, 2, qDays)
                stage.groups.addAll(drawGroups(teams, groupCount(teams.size), rnd))
                q.stages.add(stage)
                buildTable(st, q, 0, rnd)
                markBusy(q)
            }
            if (euro) friendlies(world, qDays, all.indices.filter { all[it].confed != Confed.UEFA }, busy, rnd)
        }
        // Mart arasi: herkes hazirlik
        friendlies(world, cal.intlDays.drop(6).toIntArray(), all.indices.toList(), busy, rnd)
        // Bos kalan milli takimlar (ornek: eleme grubunda bay) icin de hazirlik
        friendlies(world, qDays, all.indices.toList(), busy, rnd)
    }

    private fun groupCount(n: Int): Int = max(1, ceil(n / 4.0).toInt())

    /** Torbali kura: guce gore sirali takimlar, yilan yontemiyle gruplara. */
    private fun drawGroups(sorted: List<Int>, groups: Int, rnd: FbRandom): List<IntArray> {
        val g = List(groups) { ArrayList<Int>() }
        for ((pot, chunk) in sorted.chunked(groups).withIndex()) {
            val c = chunk.toMutableList(); rnd.shuffle(c)
            val slots = (0 until groups).toMutableList(); rnd.shuffle(slots)
            for ((i, t) in c.withIndex()) g[slots[i]].add(t)
            if (pot > 6) break
        }
        return g.filter { it.isNotEmpty() }.map { it.toIntArray() }
    }

    private fun friendlies(world: FbWorld, days: IntArray, teams: List<Int>, busy: HashMap<Int, HashSet<Int>>, rnd: FbRandom) {
        val st = world.state!!
        val comp = st.comps.firstOrNull { it.kind == CompKind.NAT_FRIENDLY } ?: newComp(st, CompKind.NAT_FRIENDLY)
        val all = FbCountries.all
        for (d in days) {
            val b = busy.getOrPut(d) { HashSet() }
            // Ayni konfederasyondan rakip tercih (karisik sira)
            val free = teams.filter { it !in b }.map { it to all[it].confed.ordinal * 1000 + rnd.int(1000) }.sortedBy { it.second }.map { it.first }
            var i = 0
            while (i + 1 < free.size) {
                val h = free[i]; val a = free[i + 1]
                st.addFixture(comp.id, -1, 0, 0, d, h, a)
                b.add(h); b.add(a); i += 2
            }
        }
    }

    // ================================================================== asama kurulumu

    /** Cember yontemiyle devreli fikstur (tek sayida takimda bay). */
    private fun roundRobin(teams: IntArray, cycles: Int): List<List<Pair<Int, Int>>> {
        val t = teams.toMutableList()
        if (t.size % 2 == 1) t.add(-1)
        val n = t.size
        val base = ArrayList<List<Pair<Int, Int>>>()
        val arr = t.toMutableList()
        for (r in 0 until n - 1) {
            val pairs = ArrayList<Pair<Int, Int>>()
            for (i in 0 until n / 2) {
                var h = arr[i]; var a = arr[n - 1 - i]
                if ((i == 0 && r % 2 == 1) || (i > 0 && i % 2 == 1)) { val x = h; h = a; a = x }
                if (h >= 0 && a >= 0) pairs.add(h to a)
            }
            base.add(pairs)
            // rotasyon (ilk sabit)
            val last = arr.removeAt(n - 1); arr.add(1, last)
        }
        val out = ArrayList<List<Pair<Int, Int>>>()
        for (c in 0 until cycles) for (r in base) out.add(if (c % 2 == 0) r else r.map { it.second to it.first })
        return out
    }

    private fun buildTable(st: FbSeasonState, comp: FbComp, si: Int, rnd: FbRandom) {
        val stage = comp.stages[si]
        for ((g, teams) in stage.groups.withIndex()) {
            val order = teams.toMutableList(); rnd.shuffle(order)
            val rounds = roundRobin(order.toIntArray(), stage.legs)
            for ((r, pairs) in rounds.withIndex()) {
                if (r >= stage.days.size) break
                for ((h, a) in pairs) st.addFixture(comp.id, si, g, r, stage.days[r], h, a)
            }
        }
        stage.built = true
    }

    private fun buildSwiss(world: FbWorld, comp: FbComp, si: Int, teams0: List<Int>, rnd: FbRandom) {
        val st = world.state!!
        val stage = comp.stages[si]
        val teams = if (teams0.size % 2 == 1) teams0.dropLast(1) else teams0
        stage.groups.clear(); stage.groups.add(teams.toIntArray())
        val rounds = min(stage.days.size, teams.size - 1)
        var best: List<List<Pair<Int, Int>>> = emptyList(); var bestBad = Int.MAX_VALUE
        repeat(40) {
            val perm = teams.toMutableList(); rnd.shuffle(perm)
            val rr = roundRobin(perm.toIntArray(), 1).take(rounds)
            val bad = rr.sumOf { r -> r.count { (h, a) -> world.clubs[h].country == world.clubs[a].country } }
            if (bad < bestBad) { bestBad = bad; best = rr }
            if (bad == 0) return@repeat
        }
        for ((r, pairs) in best.withIndex()) for ((h, a) in pairs) st.addFixture(comp.id, si, 0, r, stage.days[r], h, a)
        stage.built = true
        FbClubLife.onLeaguePhase(world, comp, teams)
    }

    /**
     * Eleme turu. [entrants] seri basi sirasinda. Bay sayisi = n - 2m; seri basi kurali [seeded] ise ust yari
     * alt yariyla (karisik) eslesir, degilse acik kura.
     */
    private fun buildKo(world: FbWorld, comp: FbComp, si: Int, entrants0: List<Int>, rnd: FbRandom, seeded: Boolean, avoidGroup: Map<Int, Int>? = null) {
        val st = world.state!!
        val stage = comp.stages[si]
        val entrants = entrants0.distinct()
        stage.built = true
        if (entrants.size <= 1) {
            stage.byes = entrants.toIntArray(); stage.complete = true
            onStageComplete(world, comp, si, rnd)
            return
        }
        val n = entrants.size
        val target = (if (stage.target > 0) stage.target else (n + 1) / 2).coerceIn((n + 1) / 2, n - 1)
        val m = n - target
        stage.byes = entrants.take(n - 2 * m).toIntArray()
        val rest = entrants.drop(n - 2 * m)
        val pairs = ArrayList<Pair<Int, Int>>()
        if (seeded) {
            val top = rest.take(m); val bottom = rest.drop(m).toMutableList(); rnd.shuffle(bottom)
            if (avoidGroup != null) {
                for (i in top.indices) if (avoidGroup[top[i]] == avoidGroup[bottom[i]]) {
                    val j = bottom.indices.firstOrNull { j -> j != i && avoidGroup[top[i]] != avoidGroup[bottom[j]] && avoidGroup[top[j]] != avoidGroup[bottom[i]] }
                    if (j != null) { val x = bottom[i]; bottom[i] = bottom[j]; bottom[j] = x }
                }
            }
            for (i in top.indices) pairs.add(top[i] to bottom[i])
        } else {
            val r = rest.toMutableList(); rnd.shuffle(r)
            for (i in 0 until m) pairs.add(r[2 * i] to r[2 * i + 1])
        }
        for ((ti, p) in pairs.withIndex()) {
            stage.ties.add(FbTie(p.first, p.second))
            if (stage.legs == 2) {
                st.addFixture(comp.id, si, ti, 0, stage.days[0], p.second, p.first, leg = 1)
                st.addFixture(comp.id, si, ti, 1, stage.days[1], p.first, p.second, leg = 2)
            } else {
                // Tek mac: alt seri basi evinde (kupa), tarafsiz sahada seri basi "ev"
                val neutral = isNeutral(comp, stage)
                if (neutral) st.addFixture(comp.id, si, ti, 0, stage.days[0], p.first, p.second)
                else st.addFixture(comp.id, si, ti, 0, stage.days[0], p.second, p.first)
            }
        }
    }

    private fun isNeutral(comp: FbComp, stage: FbStage) =
        comp.kind == CompKind.WORLD_CUP || comp.kind == CompKind.EURO || stage.label == StageLabel.FINAL &&
            comp.kind != CompKind.PLAYOFF || comp.kind == CompKind.SUPER_CUP || comp.kind == CompKind.UEFA_SUPER ||
            comp.kind == CompKind.NATIONS_LEAGUE && comp.stages.indexOf(stage) > 0

    fun isNeutral(world: FbWorld, f: FbFixture): Boolean {
        val c = world.state!!.comps[f.comp]
        return f.stage >= 0 && isNeutral(c, c.stages[f.stage])
    }

    // ================================================================== asama tamamlama

    private fun stageFixtures(st: FbSeasonState, comp: Int, si: Int) = st.compFixtures(comp).filter { it.stage == si }

    private fun checkStage(world: FbWorld, comp: FbComp, si: Int, rnd: FbRandom) {
        val stage = comp.stages[si]
        if (!stage.built || stage.complete) return
        val fx = stageFixtures(world.state!!, comp.id, si)
        if (fx.any { !it.played }) return
        if (stage.type == StageType.KO) {
            for ((ti, tie) in stage.ties.withIndex()) {
                val legs = fx.filter { it.group == ti }.sortedBy { it.leg }
                val last = legs.last()
                if (last.ph >= 0) { tie.winner = if (last.ph > last.pa) last.home else last.away; continue }
                var ga = 0; var gb = 0
                for (f in legs) { if (f.home == tie.a) { ga += f.hg; gb += f.ag } else { ga += f.ag; gb += f.hg } }
                tie.winner = if (ga > gb) tie.a else if (gb > ga) tie.b else if (rnd.chance(0.5f)) tie.a else tie.b
            }
            for (tie in stage.ties) FbClubLife.onTieWon(world, comp, stage, tie.winner, stage.label == StageLabel.FINAL)
        }
        stage.complete = true
        onStageComplete(world, comp, si, rnd)
    }

    private fun onStageComplete(world: FbWorld, comp: FbComp, si: Int, rnd: FbRandom) {
        val st = world.state!!
        val stage = comp.stages[si]
        val next = comp.stages.getOrNull(si + 1)
        when (stage.type) {
            StageType.TABLE -> when (comp.kind) {
                CompKind.LEAGUE -> {
                    val t = st.table(comp.id, si, 0)
                    finish(world, comp, t[0].team, t.getOrNull(1)?.team ?: -1)
                    // Play-off
                    st.comps.firstOrNull { it.kind == CompKind.PLAYOFF && it.league == comp.league }?.let { po ->
                        val lg = world.leagues[comp.league]
                        val auto = lg.promoted - 1
                        buildKo(world, po, 0, t.drop(auto).take(4).map { it.team }, rnd, seeded = true)
                    }
                }
                CompKind.NATIONS_LEAGUE -> {
                    val winners = (0 until min(4, stage.groups.size)).map { st.table(comp.id, si, it)[0] }
                        .sortedWith(compareByDescending<TableRow> { it.pts }.thenByDescending { it.gd }).map { it.team }
                    if (next != null) buildKo(world, comp, si + 1, winners, rnd, seeded = true)
                }
                CompKind.QUALIFIERS -> {
                    qualifiersDone(world, comp)
                    finish(world, comp, -1, -1)
                }
                CompKind.WORLD_CUP, CompKind.EURO -> {
                    val thirds = if (comp.kind == CompKind.WORLD_CUP) 8 else 4
                    val tables = stage.groups.indices.map { st.table(comp.id, si, it) }
                    val groupOf = HashMap<Int, Int>()
                    tables.forEachIndexed { g, t -> t.forEach { groupOf[it.team] = g } }
                    val cmp = compareByDescending<TableRow> { it.pts }.thenByDescending { it.gd }.thenByDescending { it.gf }
                    val firsts = tables.mapNotNull { it.getOrNull(0) }.sortedWith(cmp)
                    val seconds = tables.mapNotNull { it.getOrNull(1) }.sortedWith(cmp)
                    val third = tables.mapNotNull { it.getOrNull(2) }.sortedWith(cmp).take(thirds)
                    val q = (firsts + seconds + third).map { it.team }
                    if (next != null) buildKo(world, comp, si + 1, q, rnd, seeded = true, avoidGroup = groupOf)
                }
                else -> finish(world, comp, -1, -1)
            }
            StageType.SWISS -> {
                val t = st.table(comp.id, si, 0).map { it.team }
                // 1-8 son 16'ya, 9-24 play-off'a
                comp.stages.getOrNull(si + 2)?.extra?.addAll(t.take(8))
                if (next != null) buildKo(world, comp, si + 1, t.drop(8).take(16), rnd, seeded = true)
            }
            StageType.KO -> {
                val winners = stage.byes.toList() + stage.ties.map { it.winner }
                if (stage.losersTo >= 0) {
                    val target = st.comps[stage.losersTo]
                    target.stages.firstOrNull { it.type == StageType.SWISS }?.extra?.addAll(stage.ties.map { it.loser })
                }
                when {
                    next == null -> {
                        val fin = stage.ties.lastOrNull()
                        finish(world, comp, winners.firstOrNull() ?: -1, fin?.loser ?: -1)
                    }
                    next.type == StageType.SWISS -> next.extra.addAll(winners)
                    else -> {
                        buildKo(world, comp, si + 1, next.extra + winners, rnd, seeded = next.extra.isNotEmpty() || stage.byes.isNotEmpty())
                    }
                }
            }
        }
    }

    private fun finish(world: FbWorld, comp: FbComp, winner: Int, runnerUp: Int) {
        val st = world.state!!
        comp.done = true; comp.winner = winner; comp.runnerUp = runnerUp
        if (winner < 0 || !comp.kind.official) return
        val tier = if (comp.league >= 0) world.leagues[comp.league].tier else 0
        st.honours.add(FbHonour(world.season, comp.kind, comp.country, tier, winner, runnerUp))
    }

    /** Eleme gruplari bitince turnuvaya katilanlari belirle. */
    private fun qualifiersDone(world: FbWorld, comp: FbComp) {
        val st = world.state!!
        val tour = st.comps.firstOrNull { it.kind == CompKind.WORLD_CUP || it.kind == CompKind.EURO } ?: return
        val cf = Confed.entries[comp.confed]
        val euro = tour.kind == CompKind.EURO
        val quota = if (euro) 24 else when (cf) { Confed.UEFA -> 16; Confed.CONMEBOL -> 6; Confed.CONCACAF -> 4; Confed.AFC -> 8; Confed.CAF -> 9; Confed.OFC -> 1 }
        val stage = comp.stages[0]
        val tables = stage.groups.indices.map { st.table(comp.id, 0, it) }
        val cmp = compareByDescending<TableRow> { it.pts.toFloat() / max(1, it.p) }.thenByDescending { it.gd.toFloat() / max(1, it.p) }.thenByDescending { it.gf }
        val ordered = ArrayList<Int>()
        val maxSize = tables.maxOf { it.size }
        for (pos in 0 until maxSize) ordered.addAll(tables.mapNotNull { it.getOrNull(pos) }.sortedWith(cmp).map { it.team })
        tour.stages[0].extra.addAll(ordered.take(quota))
        // Dunya Kupasi: tum konfederasyon elemeleri bitince 48'e tamamla (kitalar arasi play-off yerine Elo)
        if (!euro && st.comps.filter { it.kind == CompKind.QUALIFIERS }.all { it.done || it === comp }) {
            val ex = tour.stages[0].extra
            val rest = FbCountries.all.indices.filter { it !in ex }.sortedByDescending { st.elo[it] }
            ex.addAll(rest.take(max(0, 48 - ex.size)))
        }
    }

    /** Turnuva gruplarini kur (kura). */
    private fun drawTournament(world: FbWorld, rnd: FbRandom) {
        val st = world.state!!
        val tour = st.comps.firstOrNull { (it.kind == CompKind.WORLD_CUP || it.kind == CompKind.EURO) && !it.stages[0].built } ?: return
        val stage = tour.stages[0]
        val teams = stage.extra.distinct().sortedByDescending { st.elo[it] }
        val groups = if (tour.kind == CompKind.WORLD_CUP) 12 else 6
        if (teams.size < groups * 2) { finish(world, tour, -1, -1); stage.built = true; stage.complete = true; return }
        stage.groups.clear(); stage.groups.addAll(drawGroups(teams.take(groups * 4), groups, rnd))
        buildTable(st, tour, 0, rnd)
    }

    // ================================================================== gun ilerletme

    fun todayFixtures(world: FbWorld): List<FbFixture> {
        val st = world.state!!
        return st.byDay[world.day]?.map { st.fixtures[it] } ?: emptyList()
    }

    /** Kullanicinin bugunku oynanmamis maci. */
    fun pendingUserFixture(world: FbWorld): FbFixture? {
        if (world.userClub < 0) return null
        val st = world.state!!
        return todayFixtures(world).firstOrNull { !it.played && !st.comps[it.comp].national && it.involves(world.userClub) }
    }

    /**
     * Bir gun ilerlet. Kullanicinin maci varsa ve [autoUser] degilse, mac oynanana kadar ilerlemez
     * ([DayReport.pending]); arayuz maci oynayip [submit] cagirir, sonra tekrar advanceDay.
     */
    fun advanceDay(world: FbWorld, autoUser: Boolean = false): DayReport {
        val st = world.state!!
        val pending = pendingUserFixture(world)
        if (pending != null && !autoUser) return DayReport(world.day, emptyList(), pending, false)
        val rnd = rng(world, 1000 + world.day)
        val today = todayFixtures(world).filter { !it.played }
        val played = ArrayList<Int>()
        var pools: Map<Int, List<FbPlayer>>? = null
        for (f in today) {
            val comp = st.comps[f.comp]
            if (comp.national && pools == null) pools = nationPools(world)
            val out = playFixture(world, f, pools)
            submit(world, f, out, checkStages = false)
            played.add(f.id)
        }
        // Asama kontrolleri
        val touched = today.map { it.comp to it.stage }.distinct()
        for ((c, s) in touched) if (s >= 0) checkStage(world, st.comps[c], s, rnd)
        // Planli kurulumlar
        val cal = calendar(world)
        if (world.day == cal.euroLeagueTuesdays[0] - 8) buildLeaguePhases(world, rnd)
        if (world.day == cal.tournamentDays[0] - 4) drawTournament(world, rnd)
        // Gunluk toparlanma ve kadro tazeleme
        dailyUpdate(world)
        world.day++
        if (world.day >= 365) {
            endSeason(world)
            return DayReport(364, played, null, true)
        }
        return DayReport(world.day - 1, played, null, false)
    }

    private fun buildLeaguePhases(world: FbWorld, rnd: FbRandom) {
        val st = world.state!!
        for (c in st.comps) {
            if (c.kind != CompKind.CHAMPIONS && c.kind != CompKind.EUROPA && c.kind != CompKind.CONFERENCE) continue
            val si = c.stages.indexOfFirst { it.type == StageType.SWISS }
            val stage = c.stages[si]
            if (stage.built) continue
            // Onceki asamalar bitmemisse (olmamali) kalanlari ekle
            val teams = stage.extra.distinct().sortedByDescending { world.clubs[it].reputation }
            buildSwiss(world, c, si, teams, rnd)
        }
    }

    /** Fiksturu oynat (kullanici maci haric): Avrupa ve milli maclar tam simulasyon, digerleri hizli model. */
    fun playFixture(world: FbWorld, f: FbFixture, pools: Map<Int, List<FbPlayer>>? = null): MatchOutcome {
        val st = world.state!!
        val comp = st.comps[f.comp]
        val stage = if (f.stage >= 0) comp.stages[f.stage] else null
        val needWinner = stage?.type == StageType.KO && (stage.legs == 1 || f.leg == 2)
        val agg = aggregateOffset(st, f)
        val neutral = isNeutral(world, f)
        val seed = world.seed xor (world.season.toLong() shl 20) xor (f.id.toLong() * 0x9E3779B97F4A7C15uL.toLong())
        val pl = if (comp.national) (pools ?: nationPools(world)) else null
        val hs = sheet(world, f.home, comp.national, pl); val aws = sheet(world, f.away, comp.national, pl)
        val detailed = comp.national || comp.kind in EURO_KINDS
        if (!detailed) return FbQuick.play(hs, aws, seed, needWinner, agg, neutral)
        val ht = FbLineup.aiTactics(hs.strength, aws.strength, !neutral, hs.formation, seed.toInt())
        val at = FbLineup.aiTactics(aws.strength, hs.strength, false, aws.formation, (seed shr 16).toInt())
        val sim = MatchSim(sideOf(hs, teamName(world, f.home, comp.national), ht, !neutral), sideOf(aws, teamName(world, f.away, comp.national), at, false), seed, needWinner, agg)
        sim.playToEnd()
        return FbQuick.fromSim(sim)
    }

    private val EURO_KINDS = setOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE, CompKind.UEFA_SUPER)

    /** Rovans icin ilk macin farki (rovans ev sahibinin bakisiyla). */
    fun aggregateOffset(st: FbSeasonState, f: FbFixture): Int {
        if (f.leg != 2) return 0
        val first = st.compFixtures(f.comp).firstOrNull { it.stage == f.stage && it.group == f.group && it.leg == 1 } ?: return 0
        if (!first.played) return 0
        // ilk macta ev sahibi = rovansin deplasmani
        return first.ag - first.hg
    }

    fun teamName(world: FbWorld, team: Int, national: Boolean) = if (national) FbCountries.all[team].englishName else world.clubs[team].name

    fun sideOf(s: TeamSheet, name: String, tactics: Tactics, home: Boolean): MatchSide {
        val side = MatchSide(s.team, name, tactics.copy(formation = s.formation), home)
        s.xi.forEachIndexed { i, p -> if (i < 11) side.onPitch[i] = MatchPlayer(p, s.formation.slots[i].pos) }
        s.bench.forEach { side.bench.add(MatchPlayer(it, it.bestPos())) }
        return side
    }

    /** Kulup veya milli takim mac kadrosu. */
    fun sheet(world: FbWorld, team: Int, national: Boolean, pools: Map<Int, List<FbPlayer>>? = null): TeamSheet =
        if (national) nationalSheet(world, team, pools ?: nationPools(world)) else clubSheet(world, team)

    fun clubSheet(world: FbWorld, club: Int): TeamSheet {
        val players = world.clubPlayers(club)
        val formation = if (club == world.userClub && world.userTactics != null) world.userTactics!!.formation
        else world.formationCache.getOrPut(club) { FbLineup.bestFormation(players) }
        val (xi, bench) = FbLineup.pick(players, formation)
        return TeamSheet(club, formation, xi, bench)
    }

    fun nationPools(world: FbWorld): Map<Int, List<FbPlayer>> {
        val m = HashMap<Int, ArrayList<FbPlayer>>()
        for (p in world.players) if (!p.retired) m.getOrPut(p.nation) { ArrayList() }.add(p)
        return m
    }

    /** Milli takim: en iyi 26 oyuncudan kadro. */
    fun nationalSheet(world: FbWorld, nation: Int, pools: Map<Int, List<FbPlayer>>): TeamSheet {
        val squad = (pools[nation] ?: emptyList()).filter { FbLineup.available(it) }.sortedByDescending { it.currentAbility() }.take(26)
        val formation = world.formationCache.getOrPut(-1000 - nation) { if (squad.size >= 11) FbLineup.bestFormation(squad) else Formation.F442 }
        val (xi, bench) = FbLineup.pick(squad, formation)
        return TeamSheet(nation, formation, xi, bench)
    }

    /** Mac sonucunu isle: skor, istatistik, kart cezalari, sakatlik, kondisyon, form, Elo, katsayi. */
    fun submit(world: FbWorld, f: FbFixture, out: MatchOutcome, checkStages: Boolean = true) {
        val st = world.state!!
        val comp = st.comps[f.comp]
        f.hg = out.hg; f.ag = out.ag; f.ph = out.ph; f.pa = out.pa; f.extraTime = out.extraTime
        val official = comp.kind.official
        // Ceza dusurme: resmi kulup macinda cezali oyuncularin cezasi bir azalir
        if (official && !comp.national) for (t in intArrayOf(f.home, f.away)) for (pid in world.clubs[t].players) {
            val p = world.players[pid]; if (p.suspendedMatches > 0) p.suspendedMatches--
        }
        val best = (out.home + out.away).maxByOrNull { it.rating }
        for ((side, lines) in listOf(0 to out.home, 1 to out.away)) {
            val gf = if (side == 0) out.hg else out.ag; val ga = if (side == 0) out.ag else out.hg
            for (l in lines) {
                val p = l.p
                val s = st.stat(f.comp, p.id)
                if (l.started) s.apps++ else s.subs++
                s.goals += l.goals; s.assists += l.assists; s.ratingSum += (l.rating * 10).toInt()
                if (l === best) s.motm++
                if (l.yellow > 0) s.yellows++
                if (l.red) s.reds++
                p.condition = l.conditionAfter
                if (l.injuryDays > 0) p.injuryDays = max(p.injuryDays, l.injuryDays)
                p.form = ((p.form * 4 + (l.rating * 10).toInt()) / 5).coerceIn(30, 100)
                p.morale = (p.morale + (if (gf > ga) 3 else if (gf < ga) -3 else 0) + if (l.rating >= 7.5f) 2 else if (l.rating < 5.8f) -2 else 0).coerceIn(5, 100)
                if (comp.national) {
                    p.caps++; p.internationalGoals += l.goals
                } else if (official) {
                    if (l.red) p.suspendedMatches += if (l.yellow >= 2) 1 else 3
                    else if (l.yellow > 0) {
                        val acc = (st.yellowAcc[p.id] ?: 0) + 1
                        st.yellowAcc[p.id] = acc
                        if (acc % 5 == 0) p.suspendedMatches += 1
                    }
                }
            }
        }
        // Milli takim Elo
        if (comp.national) {
            val neutral = isNeutral(world, f)
            val eh = st.elo[f.home] + if (neutral) 0f else 60f; val ea = st.elo[f.away]
            val exp = 1f / (1f + 10f.pow((ea - eh) / 400f))
            val res = if (out.hg > out.ag) 1f else if (out.hg < out.ag) 0f else 0.5f
            val k = if (comp.kind.official) 40f else 20f
            st.elo[f.home] += k * (res - exp); st.elo[f.away] -= k * (res - exp)
        }
        // Avrupa katsayisi: galibiyet 2, beraberlik 1 (on eleme yarim)
        if (comp.kind in EURO_KINDS && comp.kind != CompKind.UEFA_SUPER) {
            val qual = f.stage >= 0 && comp.stages[f.stage].label in QUAL_LABELS
            val mul = if (qual) 0.5f else 1f
            val ch = world.clubs[f.home].country; val ca = world.clubs[f.away].country
            when {
                out.hg > out.ag -> st.coefPts[ch] += 2f * mul
                out.hg < out.ag -> st.coefPts[ca] += 2f * mul
                else -> { st.coefPts[ch] += mul; st.coefPts[ca] += mul }
            }
        }
        FbClubLife.onFixture(world, f, out, comp)
        if (checkStages && f.stage >= 0) checkStage(world, comp, f.stage, rng(world, 1000 + world.day))
    }

    private fun dailyUpdate(world: FbWorld) {
        val nf = Attr.NATURAL_FITNESS.ordinal
        for (p in world.players) {
            if (p.injuryDays > 0) p.injuryDays--
            if (p.condition < 100) p.condition = min(100, p.condition + 4 + (p.attrs[nf].toInt() and 0xFF) / 35)
        }
        // Dizilis onbellegini kademeli tazele (her kulup ~4 haftada bir)
        val d = world.day
        val it = world.formationCache.keys.iterator()
        while (it.hasNext()) { val k = it.next(); if (Math.floorMod(k, 28) == d % 28) it.remove() }
        FbClubLife.onDay(world)
    }

    // ================================================================== dostluk maclari

    enum class FriendlyResult { OK, PAST, BUSY_US, BUSY_THEM, DECLINED }

    /** Kulubun o gun (ya da bir gun once/sonra) resmi/dostluk maci var mi */
    fun clubBusy(world: FbWorld, club: Int, day: Int): Boolean {
        val st = world.state!!
        for (d in day - 1..day + 1) for (id in st.byDay[d] ?: emptyList<Int>()) {
            val f = st.fixtures[id]
            if (!st.comps[f.comp].national && f.involves(club)) return true
        }
        return false
    }

    /** Dostluk maci ayarla ([home] evinde). Rakip itibari cok yuksekse reddedebilir. */
    fun arrangeFriendly(world: FbWorld, home: Int, away: Int, day: Int): FriendlyResult {
        val st = world.state!!
        if (day <= world.day || day >= 364) return FriendlyResult.PAST
        if (clubBusy(world, home, day)) return FriendlyResult.BUSY_US
        if (clubBusy(world, away, day)) return FriendlyResult.BUSY_THEM
        val h = world.clubs[home]; val a = world.clubs[away]
        if (a.reputation > h.reputation * 1.6f + 1500) return FriendlyResult.DECLINED
        val comp = st.comps.firstOrNull { it.kind == CompKind.FRIENDLY } ?: newComp(st, CompKind.FRIENDLY)
        st.addFixture(comp.id, -1, 0, 0, day, home, away)
        return FriendlyResult.OK
    }

    // ================================================================== sezon sonu

    var lastSummary: SeasonSummary? = null
        private set

    private fun endSeason(world: FbWorld) {
        val st = world.state!!
        FbClubLife.onSeasonEnd(world)
        val summary = SeasonSummary(world.season)
        // Devreden bilgiler
        val leagueOrder = HashMap<Int, IntArray>()
        val cupWinner = HashMap<Int, Int>()
        for (c in st.comps) {
            if (c.kind == CompKind.LEAGUE && world.leagues[c.league].tier == 1) leagueOrder[c.country] = st.table(c.id, 0, 0).map { it.team }.toIntArray()
            if (c.kind == CompKind.CUP && c.winner >= 0) cupWinner[c.country] = c.winner
        }
        val clW = st.comps.firstOrNull { it.kind == CompKind.CHAMPIONS }?.winner ?: -1
        val elW = st.comps.firstOrNull { it.kind == CompKind.EUROPA }?.winner ?: -1

        // Dusme / cikma
        for (ci in FbCountries.all.indices) {
            val tiers = world.leaguesOf(ci)
            if (tiers.size < 2) continue
            val tables = tiers.map { lg -> st.comps.firstOrNull { it.kind == CompKind.LEAGUE && it.league == lg.id }?.let { c -> st.table(c.id, 0, 0).map { it.team } } ?: lg.clubs.toList() }
            val moves = ArrayList<Triple<Int, Int, Int>>() // kulup, eski lig, yeni lig
            for (t in 0 until tiers.size - 1) {
                val upper = tiers[t]; val lower = tiers[t + 1]
                val n = min(upper.relegated, lower.promoted)
                if (n <= 0) continue
                val down = tables[t].takeLast(n)
                val po = st.comps.firstOrNull { it.kind == CompKind.PLAYOFF && it.league == lower.id && it.winner >= 0 }
                val up = if (po != null) tables[t + 1].take(n - 1).filter { it != po.winner } + po.winner else tables[t + 1].take(n)
                for (c in down) moves.add(Triple(c, upper.id, lower.id))
                for (c in up.take(n)) moves.add(Triple(c, lower.id, upper.id))
            }
            for ((c, from, to) in moves) {
                world.leagues[from].clubs.remove(c); world.leagues[to].clubs.add(c); world.clubs[c].league = to
                if (world.leagues[to].tier < world.leagues[from].tier) summary.promoted.add(c to to) else summary.relegated.add(c to to)
            }
        }

        // Katsayilar
        for (ci in st.coef.indices) {
            val row = st.coef[ci]
            for (k in 0 until 4) row[k] = row[k + 1]
            row[4] = if (st.coefClubs[ci] > 0) st.coefPts[ci] / st.coefClubs[ci] else if (FbCountries.all[ci].confed == Confed.UEFA) row[3] * 0.5f else 0f
        }

        lastSummary = summary
        world.season++
        world.day = 0
        world.formationCache.clear()
        for (p in world.players) { p.suspendedMatches = 0; p.condition = 100 }
        st.stats.clear(); st.yellowAcc.clear()
        world.compactPlayers()
        buildSeason(world, Carry(leagueOrder, cupWinner, clW, elW))
    }
}
