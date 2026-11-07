package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** Bir takimin mac kadrosu (kulup veya milli takim). */
class TeamSheet(val team: Int, val formation: Formation, val xi: List<FbPlayer>, val bench: List<FbPlayer>) {
    val strength: Float = if (xi.isEmpty()) 3f else
        xi.withIndex().sumOf { (i, p) -> FbLineup.slotScore(p, formation.slots[i].pos).toDouble() }.toFloat() / 11f * (xi.size / 11f)
    fun posOf(i: Int): Pos = formation.slots.getOrNull(i)?.pos ?: Pos.MC
}

/** Tek bir oyuncunun mac sonucu (hizli veya tam simulasyondan). */
class PlayerLine(val p: FbPlayer, val pos: Pos, val started: Boolean) {
    var goals = 0; var assists = 0; var yellow = 0; var red = false
    var injuryDays = 0
    var rating = 6.5f
    var conditionAfter = 100
}

/** Mac sonucu ozeti (hizli ve tam simulasyon icin ortak). */
class MatchOutcome(val hg: Int, val ag: Int) {
    var ph = -1; var pa = -1; var extraTime = false
    val home = ArrayList<PlayerLine>()
    val away = ArrayList<PlayerLine>()
}

/**
 * Hizli istatistiksel mac modeli: tam simulasyonla kalibre edilmis Poisson (log-dogrusal) model.
 * lambda_ev = exp(A + H + B*dx), lambda_dep = exp(A - B*dx), dx = guc farki.
 * Yapay zeka maclarinin buyuk cogunlugu bununla oynanir; kullanici maci ve izlenen maclar tam simulasyonla.
 */
object FbQuick {
    const val A = -0.03f
    const val H = 0.21f
    const val B = 0.24f

    fun play(h: TeamSheet, a: TeamSheet, seed: Long, needWinner: Boolean = false, aggregateOffset: Int = 0, neutral: Boolean = false): MatchOutcome {
        val rnd = FbRandom(seed)
        val dx = (h.strength - a.strength).coerceIn(-9f, 9f)
        val ha = if (neutral) 0f else H
        val lh = exp(A + ha + B * dx).coerceIn(0.05f, 7f)
        val la = exp(A - B * dx).coerceIn(0.05f, 7f)
        var hg = poisson(rnd, lh); var ag = poisson(rnd, la)
        var et = false; var ph = -1; var pa = -1
        if (needWinner && hg - ag + aggregateOffset == 0) {
            et = true
            hg += poisson(rnd, lh / 3.2f); ag += poisson(rnd, la / 3.2f)
            if (hg - ag + aggregateOffset == 0) {
                // Penaltilar: hafif guc etkisi
                val pHome = (0.5f + dx * 0.012f).coerceIn(0.35f, 0.65f)
                var x = 0; var y = 0
                for (k in 0 until 5) { if (rnd.chance(0.76f)) x++; if (rnd.chance(0.76f)) y++ }
                while (x == y) { val sx = rnd.chance(0.76f); val sy = rnd.chance(0.76f); if (sx) x++; if (sy) y++ }
                if ((x > y) != rnd.chance(pHome)) { val t = x; x = y; y = t }
                ph = x; pa = y
            }
        }
        val out = MatchOutcome(hg, ag)
        out.extraTime = et; out.ph = ph; out.pa = pa
        fill(out.home, h, hg, ag, rnd, et)
        fill(out.away, a, ag, hg, rnd, et)
        return out
    }

    private fun fill(lines: MutableList<PlayerLine>, s: TeamSheet, gf: Int, ga: Int, rnd: FbRandom, et: Boolean) {
        val starters = s.xi.mapIndexed { i, p -> PlayerLine(p, s.posOf(i), true) }
        lines.addAll(starters)
        // Yedekten girenler: kaleci disi ilk 3
        val subs = s.bench.filter { it.bestPos() != Pos.GK }.take(3).map { PlayerLine(it, it.bestPos(), false) }
        lines.addAll(subs)
        if (lines.isEmpty()) return
        val field = lines.filter { it.pos != Pos.GK }
        repeat(gf) {
            val scorer = pickW(rnd, field) { goalW(it) } ?: return@repeat
            scorer.goals++
            if (rnd.chance(0.74f)) pickW(rnd, field.filter { it !== scorer }) { assistW(it) }?.let { it.assists++ }
        }
        // Kartlar
        val yel = poisson(rnd, 1.65f)
        repeat(yel) { pickW(rnd, field) { cardW(it) }?.let { if (it.yellow == 1 && rnd.chance(0.25f)) { it.yellow++; it.red = true } else if (it.yellow == 0) it.yellow = 1 } }
        if (rnd.chance(0.035f)) pickW(rnd, field) { cardW(it) }?.red = true
        // Sakatlik
        if (rnd.chance(0.11f)) {
            pickW(rnd, starters) { 0.5f + it.p.hidden(Hidden.INJURY_PRONENESS) / 10f }?.let {
                it.injuryDays = 3 + rnd.int(10) + if (rnd.chance(0.25f + it.p.hidden(Hidden.INJURY_PRONENESS) / 80f)) rnd.int(40) else 0
            }
        }
        // Puan ve kondisyon
        val res = (gf - ga).coerceIn(-3, 3)
        for (l in lines) {
            var r = 6.55f + res * 0.17f + l.goals * 0.95f + l.assists * 0.5f + rnd.gauss() * 0.42f
            if (l.pos.line == Pos.Line.DEF || l.pos == Pos.GK) r += if (ga == 0) 0.45f else -0.12f * min(ga, 4)
            if (l.red) r -= 1.2f
            if (!l.started) r = 6.4f + (r - 6.55f) * 0.6f
            l.rating = r.coerceIn(4f, 10f)
            val nf = l.p.attr(Attr.NATURAL_FITNESS) + l.p.attr(Attr.STAMINA)
            val drop = if (l.started) 34f - nf * 0.5f + (if (et) 8f else 0f) else 10f
            l.conditionAfter = (l.p.condition - drop + rnd.gauss() * 3f).toInt().coerceIn(30, 100)
        }
    }

    private fun goalW(l: PlayerLine): Float {
        val base = when (l.pos) { Pos.ST -> 1f; Pos.AMC, Pos.AML, Pos.AMR -> 0.5f; Pos.MC, Pos.ML, Pos.MR -> 0.18f; Pos.DM -> 0.07f; Pos.DC -> 0.08f; else -> 0.05f }
        val q = (l.p.attr(Attr.FINISHING) * 0.6f + l.p.attr(Attr.OFF_THE_BALL) * 0.25f + l.p.attr(Attr.HEADING) * 0.15f) / 11f
        return base * q * kotlin.math.sqrt(q) * (if (l.started) 1f else 0.3f)
    }

    private fun assistW(l: PlayerLine): Float {
        val base = when (l.pos) { Pos.AMC, Pos.AML, Pos.AMR -> 1f; Pos.ML, Pos.MR -> 0.8f; Pos.MC, Pos.ST -> 0.55f; Pos.WBL, Pos.WBR, Pos.DL, Pos.DR -> 0.4f; Pos.DM -> 0.25f; else -> 0.1f }
        val q = (l.p.attr(Attr.PASSING) * 0.4f + l.p.attr(Attr.VISION) * 0.35f + l.p.attr(Attr.CROSSING) * 0.25f) / 11f
        return base * q * q * (if (l.started) 1f else 0.3f)
    }

    private fun cardW(l: PlayerLine): Float {
        val base = when (l.pos.line) { Pos.Line.DEF -> 1.2f; Pos.Line.MID -> 1f; Pos.Line.ATT -> 0.6f; Pos.Line.GK -> 0.1f }
        return base * (0.4f + l.p.attr(Attr.AGGRESSION) / 12f) * (if (l.started) 1f else 0.35f)
    }

    private inline fun <T> pickW(rnd: FbRandom, l: List<T>, w: (T) -> Float): T? {
        if (l.isEmpty()) return null
        var sum = 0f; for (x in l) sum += max(0.0001f, w(x))
        var r = rnd.float() * sum
        for (x in l) { r -= max(0.0001f, w(x)); if (r <= 0f) return x }
        return l.last()
    }

    fun poisson(rnd: FbRandom, lambda: Float): Int {
        val l = exp(-lambda); var k = 0; var p = 1f
        do { k++; p *= rnd.float() } while (p > l && k < 15)
        return k - 1
    }

    /** Tam simulasyon sonucunu ortak bicime cevir. */
    fun fromSim(sim: MatchSim): MatchOutcome {
        val out = MatchOutcome(sim.home.goals, sim.away.goals)
        sim.shootout?.let { out.ph = it.first; out.pa = it.second }
        out.extraTime = sim.events.any { it.type == EvType.EXTRA_TIME }
        for ((side, list) in listOf(sim.home to out.home, sim.away to out.away)) {
            val starters = HashSet<MatchPlayer>()
            side.onPitch.filterNotNull().forEach { starters.add(it) }
            for (mp in side.all()) {
                if (mp.minutes <= 0 && !mp.onPitch) continue
                val l = PlayerLine(mp.p, mp.pos, mp.minutes >= 45 || mp in starters)
                l.goals = mp.goals; l.assists = mp.assists; l.yellow = mp.yellow; l.red = mp.red
                l.injuryDays = if (mp.injured) mp.injuryDays else 0
                l.rating = mp.rating; l.conditionAfter = mp.condition.toInt().coerceIn(20, 100)
                list.add(l)
            }
        }
        return out
    }
}
