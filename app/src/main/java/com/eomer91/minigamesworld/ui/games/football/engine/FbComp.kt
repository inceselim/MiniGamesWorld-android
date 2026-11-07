package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.max

/** Organizasyon turleri. [national] ise takim kimlikleri ulke indeksidir. */
enum class CompKind(val national: Boolean = false, val official: Boolean = true) {
    LEAGUE, PLAYOFF, CUP, SUPER_CUP,
    CHAMPIONS, EUROPA, CONFERENCE, UEFA_SUPER,
    FRIENDLY(official = false),
    NATIONS_LEAGUE(national = true), QUALIFIERS(national = true), WORLD_CUP(national = true), EURO(national = true),
    NAT_FRIENDLY(national = true, official = false),
}

enum class StageType { TABLE, SWISS, KO }

/** Asama etiketi (arayuz ceviri anahtari). */
enum class StageLabel { LEAGUE, GROUPS, LEAGUE_PHASE, Q1, Q2, Q3, QPO, KPO, R128, R64, R32, R16, QF, SF, FINAL, PRELIM, MATCH;
    companion object {
        /** Kalan takim sayisina gore eleme etiketi */
        fun forTeams(n: Int): StageLabel = when {
            n <= 2 -> FINAL; n <= 4 -> SF; n <= 8 -> QF; n <= 16 -> R16; n <= 32 -> R32; n <= 64 -> R64; n <= 128 -> R128; else -> PRELIM
        }
    }
}

class FbFixture(
    val id: Int,
    val comp: Int,
    val stage: Int,
    /** TABLE/SWISS: grup indeksi; KO: eslesme indeksi */
    val group: Int,
    val round: Int,
    var day: Int,
    val home: Int,
    val away: Int,
) {
    /** 0 tek mac, 1 ilk ayak, 2 rovans */
    var leg: Int = 0
    var hg: Int = -1
    var ag: Int = -1
    /** Penalti atislari (yoksa -1) */
    var ph: Int = -1
    var pa: Int = -1
    var extraTime = false
    val played: Boolean get() = hg >= 0
    fun involves(t: Int) = home == t || away == t
}

/** Eleme eslesmesi; [a] seri basi (rovansi evinde oynar). */
class FbTie(val a: Int, val b: Int) {
    var winner: Int = -1
    val loser: Int get() = if (winner == a) b else if (winner == b) a else -1
}

class FbStage(
    val type: StageType,
    val label: StageLabel,
    /** KO: 1 veya 2 ayak. TABLE: devre sayisi (1 tek, 2 cift devre, 3-4 cok devreli). */
    val legs: Int,
    /** TABLE/SWISS: tur gunleri, KO: ayak gunleri */
    val days: IntArray,
) {
    val groups = ArrayList<IntArray>()
    val ties = ArrayList<FbTie>()
    /** KO'da tur atlayan (bay) takimlar */
    var byes: IntArray = IntArray(0)
    /** Disaridan bu asamaya dogrudan katilanlar (seri basi olarak once yazilir) */
    val extra = ArrayList<Int>()
    var built = false
    var complete = false
    /** KO: tur atlayacak takim sayisi (0 = yari) */
    var target = 0
    /** KO: kaybedenlerin aktarildigi organizasyon (lig asamasina), yoksa -1 */
    var losersTo = -1
}

class FbComp(
    val id: Int,
    val kind: CompKind,
    /** Ulke indeksi (ulusal organizasyonlar), yoksa -1 */
    val country: Int = -1,
    /** Lig ise lig id */
    val league: Int = -1,
    /** Konfederasyon (milli eleme) */
    val confed: Int = -1,
) {
    val stages = ArrayList<FbStage>()
    var winner = -1
    var runnerUp = -1
    var done = false
    val national: Boolean get() = kind.national
}

/** Puan tablosu satiri. */
class TableRow(val team: Int) {
    var p = 0; var w = 0; var d = 0; var l = 0; var gf = 0; var ga = 0
    val pts: Int get() = w * 3 + d
    val gd: Int get() = gf - ga
    /** Son 5 mac: 'G','B','M' */
    val form = StringBuilder()
}

/** Kupa/sampiyonluk kaydi. */
class FbHonour(val season: Int, val kind: CompKind, val country: Int, val tier: Int, val winner: Int, val runnerUp: Int)

/** Oyuncunun bir organizasyondaki sezon istatistigi. */
class PlayerCompStats {
    var apps = 0; var subs = 0; var goals = 0; var assists = 0
    /** Puan toplami x10 */
    var ratingSum = 0
    var yellows = 0; var reds = 0; var motm = 0
    val avgRating: Float get() = if (apps + subs == 0) 0f else ratingSum / 10f / (apps + subs)
}

/** Sezonun tum mac/organizasyon durumu + kalici gecmis. */
class FbSeasonState(countries: Int) {
    val comps = ArrayList<FbComp>()
    val fixtures = ArrayList<FbFixture>()
    /** (comp shl 32) or player -> istatistik */
    val stats = HashMap<Long, PlayerCompStats>()
    /** Sezon icindeki sari kart birikimi (her 5 sari = 1 mac ceza) */
    val yellowAcc = HashMap<Int, Int>()
    /** Ulke katsayisi: son 5 sezon (en eski -> en yeni) */
    val coef = Array(countries) { FloatArray(5) }
    /** Milli takim Elo puani */
    val elo = FloatArray(countries)
    val honours = ArrayList<FbHonour>()
    /** Bu sezon Avrupa katsayi puanlari ve katilan kulup sayisi */
    val coefPts = FloatArray(countries)
    val coefClubs = IntArray(countries)
    /** Ulke gelir olcegi (FbClubLife.init) */
    val finMul = FloatArray(countries) { 1f }
    /** Lig gelir olcegi (lig id) */
    var leagueMul = FloatArray(0)

    /** Gun -> fikstur indeksleri (kayit edilmez, yeniden kurulur) */
    var byDay: HashMap<Int, MutableList<Int>> = HashMap()
    /** Organizasyon -> fikstur indeksleri (kaydedilmez) */
    var byComp: HashMap<Int, MutableList<Int>> = HashMap()

    fun compFixtures(comp: Int): List<FbFixture> = byComp[comp]?.map { fixtures[it] } ?: emptyList()

    fun stat(comp: Int, player: Int): PlayerCompStats = stats.getOrPut((comp.toLong() shl 32) or player.toLong()) { PlayerCompStats() }

    fun indexDays() {
        byDay = HashMap(); byComp = HashMap()
        for (f in fixtures) { byDay.getOrPut(f.day) { ArrayList() }.add(f.id); byComp.getOrPut(f.comp) { ArrayList() }.add(f.id) }
    }

    fun addFixture(comp: Int, stage: Int, group: Int, round: Int, day: Int, home: Int, away: Int, leg: Int = 0): FbFixture {
        val f = FbFixture(fixtures.size, comp, stage, group, round, day, home, away)
        f.leg = leg
        fixtures.add(f)
        byDay.getOrPut(day) { ArrayList() }.add(f.id)
        byComp.getOrPut(comp) { ArrayList() }.add(f.id)
        return f
    }

    /** Kulubun [day] gununden onceki son [n] resmi kulup macinin puanlari (3/1/0), en yeni sonda. */
    fun recentResults(club: Int, day: Int, n: Int): List<Int> {
        val out = ArrayList<Int>()
        var d = day
        while (out.size < n && d >= 0) {
            byDay[d]?.let { ids ->
                for (id in ids) {
                    val f = fixtures[id]
                    if (!f.played || !f.involves(club) || !comps[f.comp].kind.official || comps[f.comp].national) continue
                    val gf = if (f.home == club) f.hg else f.ag; val ga = if (f.home == club) f.ag else f.hg
                    out.add(0, if (gf > ga) 3 else if (gf == ga) 1 else 0)
                }
            }
            d--
        }
        return out.takeLast(n)
    }

    /** Tum kulupler icin son [n] resmi mac puani (son 40 gun taranir). */
    fun recentAll(day: Int, n: Int): Map<Int, List<Int>> {
        val m = HashMap<Int, ArrayList<Int>>()
        for (d in max(0, day - 40)..day) {
            val ids = byDay[d] ?: continue
            for (id in ids) {
                val f = fixtures[id]
                if (!f.played || comps[f.comp].national || !comps[f.comp].kind.official) continue
                m.getOrPut(f.home) { ArrayList() }.add(if (f.hg > f.ag) 3 else if (f.hg == f.ag) 1 else 0)
                m.getOrPut(f.away) { ArrayList() }.add(if (f.ag > f.hg) 3 else if (f.hg == f.ag) 1 else 0)
            }
        }
        return m.mapValues { it.value.takeLast(n) }
    }

    fun countryRanking(): List<Int> = coef.indices.sortedByDescending { coef[it].sum() }

    /** Bir asamanin grubuna ait puan tablosu (siralama: puan, averaj, atilan gol, ikili/rastgele yerine kimlik). */
    fun table(comp: Int, stage: Int, group: Int): List<TableRow> {
        val st = comps[comp].stages[stage]
        val teams = st.groups.getOrNull(group) ?: return emptyList()
        val rows = HashMap<Int, TableRow>()
        for (t in teams) rows[t] = TableRow(t)
        for (f in compFixtures(comp)) {
            if (f.stage != stage || f.group != group || !f.played) continue
            val h = rows[f.home] ?: continue; val a = rows[f.away] ?: continue
            h.p++; a.p++; h.gf += f.hg; h.ga += f.ag; a.gf += f.ag; a.ga += f.hg
            when {
                f.hg > f.ag -> { h.w++; a.l++; h.form.append('G'); a.form.append('M') }
                f.hg < f.ag -> { h.l++; a.w++; h.form.append('M'); a.form.append('G') }
                else -> { h.d++; a.d++; h.form.append('B'); a.form.append('B') }
            }
        }
        for (r in rows.values) if (r.form.length > 5) r.form.delete(0, r.form.length - 5)
        return rows.values.sortedWith(compareByDescending<TableRow> { it.pts }.thenByDescending { it.gd }.thenByDescending { it.gf }.thenBy { it.team })
    }

    fun fixturesOf(team: Int, national: Boolean): List<FbFixture> =
        fixtures.filter { it.involves(team) && comps[it.comp].national == national }.sortedBy { it.day }
}
