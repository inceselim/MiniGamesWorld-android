package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Tekrarlanabilir rastgele sayi (SplitMix64). */
class FbRandom(seed: Long) {
    private var s = seed
    fun nextLong(): Long {
        s += -0x61c8864680b583ebL
        var z = s
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
    fun int(n: Int): Int = ((nextLong() ushr 1) % n).toInt()
    fun range(a: Int, b: Int): Int = a + int(b - a + 1)
    fun float(): Float = (nextLong() ushr 40).toFloat() / (1L shl 24).toFloat()
    fun chance(p: Float) = float() < p
    /** Standart normal (Box-Muller) */
    fun gauss(): Float {
        val u1 = max(1e-7f, float()); val u2 = float()
        return (sqrt(-2.0 * ln(u1.toDouble())) * kotlin.math.cos(2.0 * Math.PI * u2)).toFloat()
    }
    fun <T> pick(l: List<T>): T = l[int(l.size)]
    fun <T> weighted(l: List<Pair<T, Float>>): T {
        var r = float() * l.sumOf { it.second.toDouble() }.toFloat()
        for ((v, w) in l) { r -= w; if (r <= 0f) return v }
        return l.last().first
    }
    fun <T> shuffle(l: MutableList<T>) { for (i in l.size - 1 downTo 1) { val j = int(i + 1); val t = l[i]; l[i] = l[j]; l[j] = t } }
}

object FbEconomy {
    /**
     * Oyuncu degeri (Euro). Yetenek, potansiyel, yas, sozlesme ve ligin buyuklugune gore.
     * Kabaca: CA 60 ~ 50 bin, CA 100 ~ 600 bin, CA 140 ~ 8 milyon, CA 180 ~ 100 milyon.
     */
    fun value(p: FbPlayer, age: Int, ca: Int, yearsLeft: Int, leagueLevel: Int): Long {
        val base = 50_000.0 * 10.0.pow((ca - 60) * 0.0275)
        val potBonus = if (age <= 23) 10.0.pow(max(0, p.potential - ca) * 0.012) else 1.0
        val ageMul = when {
            age <= 20 -> 1.15
            age <= 24 -> 1.2
            age <= 28 -> 1.0
            age <= 30 -> 0.7
            age <= 32 -> 0.45
            else -> 0.22
        }
        val contract = when {
            yearsLeft <= 0 -> 0.3
            yearsLeft == 1 -> 0.65
            else -> 1.0
        }
        val league = 0.55 + leagueLevel / 200.0
        return roundValue((base * potBonus * ageMul * contract * league).toLong())
    }

    fun roundValue(v: Long): Long = when {
        v < 10_000 -> (v / 1_000).coerceAtLeast(1) * 1_000
        v < 1_000_000 -> v / 5_000 * 5_000
        v < 10_000_000 -> v / 50_000 * 50_000
        else -> v / 500_000 * 500_000
    }

    /** Haftalik maas (Euro). Kabaca: CA 60 ~ 700, CA 100 ~ 5 bin, CA 140 ~ 35 bin, CA 180 ~ 250 bin (buyuk kulupte). */
    fun wage(ca: Int, age: Int, clubReputation: Int): Int {
        val base = 700.0 * 10.0.pow((ca - 60) * 0.0212)
        val rep = 0.35 + clubReputation / 10000.0 * 1.0
        val ageMul = if (age < 21) 0.6 else 1.0
        val w = base * rep * ageMul
        return when {
            w < 1000 -> (w / 50).roundToInt() * 50
            w < 20000 -> (w / 250).roundToInt() * 250
            else -> (w / 1000).roundToInt() * 1000
        }.coerceAtLeast(150)
    }
}

/**
 * Dunyayi bastan uretir: ligler, kulupler, oyuncular, personel ve Avrupa disi milli oyuncu havuzlari.
 * Ayni tohum ayni dunyayi uretir.
 */
class FbGen(private val seed: Long) {
    private val rnd = FbRandom(seed)
    private val countries = FbCountries.all
    private val world = FbWorld(seed)
    private val usedClubNames = HashSet<String>()
    private val usedPlayerNames = HashSet<String>()

    /** Ust lig ortalama ilk 11 CA'si */
    private fun leagueMean(level: Int, tier: Int): Float {
        val top = 40f + level * 1.05f
        return top - (tier - 1) * (14f + level * 0.05f)
    }

    fun generate(): FbWorld {
        // Avrupa ligleri
        countries.forEachIndexed { ci, c ->
            if (c.confed != Confed.UEFA || c.tiers.isEmpty()) return@forEachIndexed
            val cityPool = c.cities.toMutableList()
            c.tiers.forEachIndexed { ti, n ->
                val tier = ti + 1
                val lg = FbLeague(world.leagues.size, ci, tier)
                world.leagues.add(lg)
                lg.promoted = if (tier > 1) promotedCount(c, tier) else 0
                lg.relegated = if (tier < c.tiers.size) promotedCount(c, tier + 1) else 0
                lg.playoffSpots = if (tier > 1 && n >= 16) 4 else 0
                val mean = leagueMean(c.level, tier)
                val spread = 14f + c.level * 0.12f - (tier - 1) * 4f
                for (k in 0 until n) {
                    // Sirali guc: ilk kulupler guclu; ust ligde birkac dev
                    val frac = k / max(1f, n - 1f)
                    val offset = spread * (1f - 2f * frac.pow(0.75f)) + rnd.gauss() * 3f
                    val q = mean + offset
                    val club = makeClub(ci, c, cityPool, q, tier)
                    club.league = lg.id
                    lg.clubs.add(club.id)
                }
            }
        }
        // Lihtenstayn kulupleri Isvicre liginde: oyunculari Isvicre kuluplerine dagilir (asagidaki yabanci secimi).
        // Avrupa disi milli havuzlar
        countries.forEachIndexed { ci, c ->
            if (c.confed == Confed.UEFA) {
                if (c.tiers.isEmpty()) repeat(30) { makePlayer(ci, c, poolQuality(c), null, CLUB_ABROAD) }
                return@forEachIndexed
            }
            repeat(34) { makePlayer(ci, c, poolQuality(c), null, CLUB_ABROAD) }
        }
        // Serbest oyuncular
        repeat(250) {
            val ci = pickNationality(-1, 0.5f)
            val p = makePlayer(ci, countries[ci], 70f + rnd.gauss() * 18f, null, CLUB_NONE)
            p.contractEnd = world.season - 1
        }
        for (p in world.players) p.invalidate()
        return world
    }

    private fun poolQuality(c: CountryDef) = 40f + c.level * 1.0f + rnd.gauss() * 14f

    private fun promotedCount(c: CountryDef, lowerTier: Int): Int {
        val upper = c.tiers[lowerTier - 2]
        return when {
            upper >= 18 -> 3
            upper >= 12 -> 2
            else -> 1
        }
    }

    private fun makeClub(ci: Int, c: CountryDef, cityPool: MutableList<String>, quality: Float, tier: Int): FbClub {
        val city = if (cityPool.isNotEmpty() && (tier > 1 || rnd.chance(0.85f))) cityPool.removeAt(0) else rnd.pick(c.cities)
        val culture = rnd.weighted(c.cultures.map { it.first to it.second })
        var name: String
        var tries = 0
        do {
            val pattern = rnd.pick(culture.clubPatterns)
            name = pattern.replace("{c}", city)
            tries++
        } while (name in usedClubNames && tries < 30)
        if (name in usedClubNames) name = "$city ${tries}"
        usedClubNames.add(name)
        val club = FbClub(world.clubs.size, name, shortName(name, city), ci, city)
        world.clubs.add(club)
        val qn = ((quality - 40f) / 130f).coerceIn(0f, 1f) // 0..1
        club.reputation = (qn.pow(1.4f) * 9500f + 300f + rnd.gauss() * 250f).roundToInt().coerceIn(100, 10000)
        club.color1 = PALETTE[rnd.int(PALETTE.size)]
        club.color2 = PALETTE.filter { it != club.color1 }.let { it[rnd.int(it.size)] }
        club.capacity = ((2_000 + qn.pow(1.6f) * 75_000) * (0.7f + rnd.float() * 0.6f)).roundToInt() / 100 * 100
        club.stadiumName = "$city Stadium"
        fun fac(): Int = (2 + qn * 16 + rnd.gauss() * 2f).roundToInt().coerceIn(1, 20)
        club.trainingFacilities = fac(); club.youthFacilities = fac(); club.youthRecruitment = fac()
        club.stadiumLevel = fac(); club.socialFacilities = fac()
        club.fanBase = (club.capacity * (0.8f + rnd.float())).roundToInt()

        // Kadro
        val size = when (tier) { 1 -> 25; 2 -> 23; else -> 21 }
        val template = SQUAD_TEMPLATE.take(size)
        val foreignShare = when (tier) {
            1 -> ((c.level - 35) / 110f).coerceIn(0.05f, 0.55f)
            2 -> ((c.level - 45) / 200f).coerceIn(0.03f, 0.25f)
            else -> 0.05f
        }
        for ((i, pos) in template.withIndex()) {
            val nat = if (rnd.chance(foreignShare)) pickNationality(ci, 1f) else ci
            // Ilk 11 daha iyi, yedekler biraz zayif
            val starter = i < 14
            val q = quality + (if (starter) 4f else -6f) + rnd.gauss() * 7f + (if (nat != ci) 4f else 0f)
            val p = makePlayer(nat, countries[nat], q, pos, club.id)
            club.players.add(p.id)
        }
        // Forma numaralari
        val used = HashSet<Int>()
        for (pid in club.players) {
            val p = world.players[pid]
            val pref = when (p.bestPos()) {
                Pos.GK -> listOf(1, 12, 13, 25, 30)
                Pos.DR, Pos.WBR -> listOf(2, 22, 15, 24)
                Pos.DL, Pos.WBL -> listOf(3, 23, 26)
                Pos.DC -> listOf(4, 5, 6, 15, 16, 32)
                Pos.DM -> listOf(6, 8, 16, 18)
                Pos.MC -> listOf(8, 14, 18, 20)
                Pos.MR, Pos.ML -> listOf(7, 11, 17, 19)
                Pos.AMC -> listOf(10, 20, 21)
                Pos.AMR, Pos.AML -> listOf(7, 11, 17, 27)
                Pos.ST -> listOf(9, 19, 29, 39)
            }
            p.squadNumber = pref.firstOrNull { it !in used } ?: (1..99).first { it !in used }
            used.add(p.squadNumber)
        }
        val weeklyWages = club.players.sumOf { world.players[it].wage.toLong() }
        club.wageBudget = (weeklyWages * (1.05 + rnd.float() * 0.15)).toLong()
        // Yillik gelir: maaslar gelirin ~%55-70'i olacak sekilde
        val revenue = weeklyWages * 52.0 / (0.55 + rnd.float() * 0.15)
        club.sponsorIncome = (revenue * (0.15 + rnd.float() * 0.1)).toLong()
        club.balance = (revenue * (0.05 + rnd.float() * 0.4)).toLong()
        club.transferBudget = (revenue * (0.04 + rnd.float() * 0.18)).toLong()
        makeStaff(club, ci, c, qn)
        return club
    }

    private fun shortName(name: String, city: String): String {
        val c = city.filter { it.isLetter() }.uppercase()
        return if (c.length >= 3) c.substring(0, 3) else name.filter { it.isLetter() }.uppercase().take(3)
    }

    /** Yabanci oyuncu uyrugu: futbol seviyesi yuksek ulkeler daha cok "ihrac" eder. */
    private fun pickNationality(exclude: Int, power: Float): Int {
        val list = countries.indices.filter { it != exclude }.map { it to (countries[it].level / 50f).pow(4f * power) }
        return rnd.weighted(list)
    }

    private fun randomAge(): Int {
        val a = 24f + rnd.gauss() * 4.2f
        return a.roundToInt().coerceIn(17, 37)
    }

    /**
     * Oyuncu uretir. [quality] hedef CA (yasa gore duzeltilir). [pos] null ise rastgele mevki.
     */
    fun makePlayer(nat: Int, c: CountryDef, quality: Float, pos: Pos?, club: Int): FbPlayer {
        val p0 = pos ?: rnd.pick(SQUAD_TEMPLATE)
        val age = randomAge()
        // Yas: gencler henuz zirvede degil
        val ageAdj = when {
            age <= 18 -> -28f
            age <= 20 -> -16f
            age <= 22 -> -7f
            age <= 29 -> 0f
            age <= 32 -> -3f
            else -> -9f
        }
        val ca = (quality + ageAdj).roundToInt().coerceIn(25, 195)
        val growth = when {
            age <= 18 -> 30f + rnd.gauss() * 18f
            age <= 20 -> 20f + rnd.gauss() * 14f
            age <= 23 -> 10f + rnd.gauss() * 8f
            age <= 26 -> 3f + abs(rnd.gauss()) * 4f
            else -> abs(rnd.gauss()) * 2f
        }
        val potential = (ca + max(0f, growth)).roundToInt().coerceIn(ca, 200)

        val culture = rnd.weighted(c.cultures.map { it.first to it.second })
        var first: String; var last: String; var tries = 0
        do {
            first = rnd.pick(culture.firstNames); last = rnd.pick(culture.lastNames); tries++
        } while ("$first $last" in usedPlayerNames && tries < 6)
        usedPlayerNames.add("$first $last")

        val attrs = makeAttributes(p0, ca, age)
        val hidden = ByteArray(Hidden.entries.size) { (8 + rnd.gauss() * 4f).roundToInt().coerceIn(1, 20).toByte() }
        val fam = positionFamiliarity(p0)
        val foot = when {
            p0 == Pos.DL || p0 == Pos.WBL || p0 == Pos.ML || p0 == Pos.AML -> if (rnd.chance(0.75f)) Foot.LEFT else Foot.RIGHT
            rnd.chance(0.08f) -> Foot.BOTH
            rnd.chance(0.22f) -> Foot.LEFT
            else -> Foot.RIGHT
        }
        val height = when (p0) {
            Pos.GK -> 186 + rnd.gauss() * 4f
            Pos.DC -> 186 + rnd.gauss() * 4f
            Pos.ST -> 182 + rnd.gauss() * 6f
            else -> 178 + rnd.gauss() * 6f
        }.roundToInt().coerceIn(160, 204)
        val birthYear = world.season - age
        val p = FbPlayer(world.players.size, first, last, nat, birthYear, rnd.int(365), attrs, hidden, fam, potential, foot, height)
        p.club = club
        val clubRep = if (club >= 0) world.clubs[club].reputation else 2000
        p.wage = FbEconomy.wage(ca, age, clubRep)
        p.contractEnd = world.season + rnd.range(0, 4) + if (age < 24) 1 else 0
        p.reputation = (((ca - 40) / 150f).coerceIn(0f, 1f).pow(1.5f) * 8000f + clubRep * 0.2f).roundToInt().coerceIn(0, 10000)
        p.morale = rnd.range(55, 85)
        p.form = 65
        world.players.add(p)
        return p
    }

    /** Mevkiye uygun yetenek dagilimi uretir ve CA'yi hedefe oturtur. */
    private fun makeAttributes(pos: Pos, targetCa: Int, age: Int): ByteArray {
        val w = PosWeights.of(pos)
        val maxW = w.values.max()
        val level = targetCa / 10f
        val a = FloatArray(Attr.COUNT)
        for (attr in Attr.entries) {
            val imp = (w[attr] ?: 0f) / maxW // 0..1
            var v = when {
                attr.group == AttrGroup.GOALKEEPING && pos != Pos.GK -> 1f + rnd.float() * 4f
                pos == Pos.GK && attr.group == AttrGroup.TECHNICAL && attr !in GK_TECH -> 1f + rnd.float() * 6f + level * 0.15f
                else -> level * (0.62f + imp * 0.45f) + rnd.gauss() * 1.8f
            }
            // Yasa gore fiziksel/zihinsel
            if (attr.group == AttrGroup.PHYSICAL && (attr == Attr.PACE || attr == Attr.ACCELERATION || attr == Attr.AGILITY)) {
                v += when { age <= 21 -> 1f; age >= 31 -> -1.5f - (age - 31) * 0.4f; else -> 0f }
            }
            if (attr.group == AttrGroup.MENTAL && attr != Attr.FLAIR) {
                v += when { age <= 20 -> -1.2f; age >= 29 -> 1f; else -> 0f }
            }
            a[attr.ordinal] = v.coerceIn(1f, 20f)
        }
        val bytes = ByteArray(Attr.COUNT)
        fun toBytes() { for (i in a.indices) bytes[i] = (a[i] * 10f).roundToInt().coerceIn(10, 200).toByte() }
        toBytes()
        // CA hedefine yaklastir (birkac tur)
        repeat(8) {
            val ca = PosWeights.currentAbility(bytes, pos)
            val diff = (targetCa - ca) / 10f
            if (abs(diff) < 0.15f) return bytes
            for (attr in Attr.entries) {
                if (attr.group == AttrGroup.GOALKEEPING && pos != Pos.GK) continue
                a[attr.ordinal] = (a[attr.ordinal] + diff * 1.05f).coerceIn(1f, 20f)
            }
            toBytes()
        }
        return bytes
    }

    private fun positionFamiliarity(p: Pos): ByteArray {
        val f = ByteArray(Pos.COUNT) { 1 }
        f[p.ordinal] = 20
        val near = NEIGHBOURS[p] ?: emptyList()
        for (n in near) if (rnd.chance(0.55f)) f[n.ordinal] = rnd.range(8, 18).toByte()
        if (p != Pos.GK && rnd.chance(0.25f)) {
            val any = Pos.entries.filter { it != Pos.GK && it != p }
            f[rnd.pick(any).ordinal] = rnd.range(6, 15).toByte()
        }
        return f
    }

    private fun makeStaff(club: FbClub, ci: Int, c: CountryDef, qn: Float) {
        val roles = mutableListOf(StaffRole.ASSISTANT, StaffRole.COACH, StaffRole.COACH, StaffRole.GK_COACH, StaffRole.FITNESS_COACH, StaffRole.SCOUT, StaffRole.PHYSIO, StaffRole.YOUTH_COACH)
        if (qn > 0.45f) roles += listOf(StaffRole.COACH, StaffRole.SCOUT, StaffRole.PHYSIO)
        if (qn > 0.7f) roles += listOf(StaffRole.COACH, StaffRole.SCOUT)
        for (r in roles) {
            val s = makeStaffMember(ci, c, r, 4f + qn * 12f + rnd.gauss() * 2.5f)
            s.club = club.id
            club.staff.add(s.id)
        }
        val mc = rnd.weighted(c.cultures.map { it.first to it.second })
        club.managerName = rnd.pick(mc.firstNames) + " " + rnd.pick(mc.lastNames)
    }

    fun makeStaffMember(ci: Int, c: CountryDef, role: StaffRole, quality: Float): FbStaff {
        val culture = rnd.weighted(c.cultures.map { it.first to it.second })
        val attrs = ByteArray(StaffAttr.entries.size) { (quality + rnd.gauss() * 3f).roundToInt().coerceIn(1, 20).toByte() }
        // Rolune gore one cikan yetenekler
        val focus = when (role) {
            StaffRole.GK_COACH -> listOf(StaffAttr.COACH_GK)
            StaffRole.FITNESS_COACH -> listOf(StaffAttr.COACH_FITNESS)
            StaffRole.SCOUT -> listOf(StaffAttr.JUDGING_ABILITY, StaffAttr.JUDGING_POTENTIAL)
            StaffRole.PHYSIO -> listOf(StaffAttr.PHYSIOTHERAPY)
            StaffRole.YOUTH_COACH -> listOf(StaffAttr.WORKING_WITH_YOUTH)
            StaffRole.ASSISTANT -> listOf(StaffAttr.COACH_TACTICAL, StaffAttr.MAN_MANAGEMENT, StaffAttr.JUDGING_ABILITY)
            StaffRole.COACH -> listOf(rnd.pick(listOf(StaffAttr.COACH_ATTACK, StaffAttr.COACH_DEFENCE, StaffAttr.COACH_TECHNICAL, StaffAttr.COACH_TACTICAL, StaffAttr.COACH_MENTAL)))
        }
        for (f in focus) attrs[f.ordinal] = (attrs[f.ordinal] + 3).coerceAtMost(20).toByte()
        val s = FbStaff(world.staff.size, rnd.pick(culture.firstNames), rnd.pick(culture.lastNames), ci, world.season - rnd.range(30, 62), role, attrs)
        s.wage = (150 + quality.coerceIn(1f, 20f).pow(2.4f) * 6f).roundToInt() / 50 * 50
        s.contractEnd = world.season + rnd.range(0, 3)
        s.reputation = (quality * 450).roundToInt().coerceIn(0, 10000)
        world.staff.add(s)
        return s
    }

    companion object {
        /** Kadro sablonu: ilk 11 + yedekler (mevki sirasi) */
        val SQUAD_TEMPLATE = listOf(
            Pos.GK, Pos.DR, Pos.DC, Pos.DC, Pos.DL, Pos.DM, Pos.MC, Pos.MC, Pos.AMR, Pos.AML, Pos.ST,
            Pos.GK, Pos.DC, Pos.ST, Pos.MC, Pos.DR, Pos.DL, Pos.AMC, Pos.MR, Pos.DC, Pos.ML,
            Pos.DM, Pos.ST, Pos.GK, Pos.AMC,
        )
        private val GK_TECH = setOf(Attr.PASSING, Attr.FIRST_TOUCH)
        private val NEIGHBOURS = mapOf(
            Pos.DR to listOf(Pos.WBR, Pos.DC, Pos.MR), Pos.DL to listOf(Pos.WBL, Pos.DC, Pos.ML),
            Pos.DC to listOf(Pos.DM, Pos.DR, Pos.DL), Pos.WBR to listOf(Pos.DR, Pos.MR), Pos.WBL to listOf(Pos.DL, Pos.ML),
            Pos.DM to listOf(Pos.MC, Pos.DC), Pos.MC to listOf(Pos.DM, Pos.AMC, Pos.MR, Pos.ML),
            Pos.MR to listOf(Pos.AMR, Pos.MC, Pos.WBR), Pos.ML to listOf(Pos.AML, Pos.MC, Pos.WBL),
            Pos.AMC to listOf(Pos.MC, Pos.ST, Pos.AMR, Pos.AML), Pos.AMR to listOf(Pos.MR, Pos.AML, Pos.ST, Pos.AMC),
            Pos.AML to listOf(Pos.ML, Pos.AMR, Pos.ST, Pos.AMC), Pos.ST to listOf(Pos.AMC, Pos.AMR, Pos.AML),
        )
        /** Forma renkleri */
        val PALETTE = listOf(
            0xFFD32F2F.toInt(), 0xFF1565C0.toInt(), 0xFFFFFFFF.toInt(), 0xFF212121.toInt(), 0xFFFFC107.toInt(), 0xFF2E7D32.toInt(),
            0xFF6A1B9A.toInt(), 0xFFEF6C00.toInt(), 0xFF00838F.toInt(), 0xFF8D1C2B.toInt(), 0xFF81D4FA.toInt(), 0xFF0D1B4C.toInt(),
        )
    }
}
