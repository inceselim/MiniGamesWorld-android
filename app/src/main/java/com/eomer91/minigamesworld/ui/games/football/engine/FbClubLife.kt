package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Defter kalemleri. */
enum class Fin(val income: Boolean) {
    TICKETS(true), TV(true), MERCH(true), SPONSOR(true), ADS(true), PRIZE(true), TRANSFER_IN(true), OTHER_IN(true),
    WAGES(false), STAFF_WAGES(false), FACILITIES(false), YOUTH(false), MATCHDAY(false), TRANSFER_OUT(false), UPGRADES(false), INTEREST(false), OTHER_OUT(false),
}

/** Yatirim yapilabilir tesisler. */
enum class Facility { TRAINING, YOUTH, RECRUITMENT, STADIUM, SOCIAL, CAPACITY }

/** Antrenman alanlari: ilgili yetenekler ve sorumlu antrenor yetenegi. */
enum class TrainArea(val coach: StaffAttr, val attrs: List<Attr>) {
    ATTACKING(StaffAttr.COACH_ATTACK, listOf(Attr.FINISHING, Attr.OFF_THE_BALL, Attr.COMPOSURE, Attr.DRIBBLING, Attr.LONG_SHOTS, Attr.HEADING, Attr.FLAIR)),
    DEFENDING(StaffAttr.COACH_DEFENCE, listOf(Attr.TACKLING, Attr.MARKING, Attr.POSITIONING, Attr.HEADING, Attr.CONCENTRATION, Attr.ANTICIPATION, Attr.BRAVERY)),
    TECHNICAL(StaffAttr.COACH_TECHNICAL, listOf(Attr.PASSING, Attr.FIRST_TOUCH, Attr.TECHNIQUE, Attr.DRIBBLING, Attr.CROSSING)),
    TACTICAL(StaffAttr.COACH_TACTICAL, listOf(Attr.DECISIONS, Attr.POSITIONING, Attr.TEAMWORK, Attr.VISION, Attr.ANTICIPATION, Attr.WORK_RATE, Attr.OFF_THE_BALL)),
    PHYSICAL(StaffAttr.COACH_FITNESS, listOf(Attr.PACE, Attr.ACCELERATION, Attr.STAMINA, Attr.STRENGTH, Attr.AGILITY, Attr.BALANCE, Attr.JUMPING, Attr.NATURAL_FITNESS)),
    MENTAL(StaffAttr.COACH_MENTAL, listOf(Attr.DETERMINATION, Attr.LEADERSHIP, Attr.COMPOSURE, Attr.CONCENTRATION, Attr.AGGRESSION, Attr.BRAVERY, Attr.WORK_RATE)),
    SET_PIECES(StaffAttr.COACH_TECHNICAL, listOf(Attr.CORNERS, Attr.FREE_KICK, Attr.PENALTY, Attr.HEADING, Attr.LONG_SHOTS)),
    GOALKEEPING(StaffAttr.COACH_GK, listOf(Attr.REFLEXES, Attr.HANDLING, Attr.ONE_ON_ONES, Attr.AERIAL_REACH, Attr.COMMAND_OF_AREA, Attr.KICKING)),
}

class SponsorOffer(val name: String, val yearly: Long, val years: Int)

/** Kulubun antrenman/gelisim kalitesi (haftalik hesaplanir). */
class TrainQuality(
    /** TrainArea sirasinda antrenor kalitesi 1..20 */
    val coach: FloatArray,
    val facilities: Float,
    val youthCoach: Float,
    val youthFacilities: Float,
    val physio: Float,
    val fitness: Float,
    val manMgmt: Float,
    val intensity: Int,
    val area: Int,
    val social: Float,
)

/**
 * Kulup yasami: finans (gelir/gider defteri), antrenman ve oyuncu gelisimi/yaslanmasi, moral,
 * altyapi alimi, personel, tesis yatirimi, sponsorluk, itibar ve sezon sonu islemleri.
 * FbSeason gunluk/haftalik/mac/sezon sonu kancalarindan cagrilir.
 */
object FbClubLife {

    // ================================================================== olcekler

    private fun lv(ci: Int) = (FbCountries.all[ci].level / 92f).coerceIn(0.1f, 1.05f)
    /** Ulkeye gore maliyet olcegi */
    fun costScale(ci: Int) = 0.3f + 0.7f * lv(ci).pow(2f)
    /** Lig gelir olcegi (FbClubLife.init'te kalibre edilir) */
    private fun fm(world: FbWorld, league: Int) = world.state?.leagueMul?.getOrNull(league) ?: 1f
    fun revenueMul(world: FbWorld, c: FbClub) = fm(world, c.league)

    /** Lig yayin havuzu (yillik, Euro) */
    fun tvPool(world: FbWorld, lg: FbLeague): Double =
        max(2e6, 2.4e9 * lv(lg.country).toDouble().pow(9.0)) * 0.11.pow(lg.tier - 1.0) * fm(world, lg.id)

    fun ticketPrice(world: FbWorld, c: FbClub): Float {
        val tier = world.leagues.getOrNull(c.league)?.tier ?: 3
        val base = (10f + 60f * lv(c.country).pow(2f)) * (1f - 0.28f * (tier - 1)).coerceAtLeast(0.3f)
        return base * (0.7f + 0.3f * c.reputation / 10000f) * (1f + 0.25f * c.ticketLevel) * (0.9f + c.stadiumLevel / 100f)
    }

    fun merchYear(world: FbWorld, c: FbClub): Double {
        val stars = c.players.map { world.players[it].reputation }.sortedDescending().take(3).sum() / 30000.0
        return (120e6 * (c.reputation / 10000.0).pow(4.0) * (0.4 + 0.6 * lv(c.country)) + c.fanBase * 8.0) *
            (1.0 + stars) * (0.9 + c.socialFacilities / 100.0) * fm(world, c.league)
    }

    fun adsYear(world: FbWorld, c: FbClub): Double = c.capacity * 40.0 * (0.3 + c.reputation / 10000.0) * fm(world, c.league)

    fun upkeepWeek(c: FbClub): Double {
        val cs = costScale(c.country)
        return ((c.trainingFacilities.sq() * 60 + c.youthFacilities.sq() * 45 + c.youthRecruitment.sq() * 30 +
            c.stadiumLevel.sq() * 35 + c.socialFacilities.sq() * 25) * cs + c.capacity * 0.5 * cs)
    }

    fun youthWeek(c: FbClub): Double = c.youthRecruitment * c.youthFacilities * 120.0 * costScale(c.country)

    private fun Int.sq() = (this * this).toDouble()

    /** Alt liglerde tesis ve altyapi giderleri daha dusuk (yerel isci, daha kucuk tesis). */
    fun tierCost(world: FbWorld, c: FbClub): Double = when (world.leagues.getOrNull(c.league)?.tier ?: 3) { 1 -> 1.0; 2 -> 0.45; else -> 0.25 }

    /** Tahmini seyirci (talep, kapasiteyle sinirli). */
    fun attendance(world: FbWorld, c: FbClub, opp: Int, kind: CompKind): Int {
        val oppRep = if (opp >= 0 && !kind.national) world.clubs[opp].reputation else 3000
        val comp = when (kind) {
            CompKind.CHAMPIONS -> 1.35f; CompKind.EUROPA -> 1.15f; CompKind.CONFERENCE -> 1.0f
            CompKind.CUP, CompKind.PLAYOFF -> 0.85f; CompKind.FRIENDLY -> 0.35f; else -> 1f
        }
        val price = 1f - 0.18f * c.ticketLevel
        val demand = c.fanBase * (0.6f + 0.4f * c.reputation / 10000f) * (1f + oppRep / 20000f) * comp * price * (0.9f + c.stadiumLevel / 100f)
        return min(c.capacity.toFloat(), demand).roundToInt()
    }

    // ================================================================== kurulum

    fun init(world: FbWorld) {
        val st = world.state!!
        val rnd = FbRandom(world.seed xor 0x5EEDL)
        // Ulke bazinda gelir olcegi: uretilen maas yapisi ~%62 maas/gelir oranina otursun
        st.leagueMul = FloatArray(world.leagues.size) { 1f }
        for (lg0 in world.leagues) {
            val clubs = lg0.clubs.map { world.clubs[it] }
            if (clubs.isEmpty()) continue
            var target = 0.0; var model = 0.0
            for (c in clubs) {
                val wages = c.players.sumOf { world.players[it].wage.toLong() } + c.staff.sumOf { world.staff[it].wage.toLong() }
                target += wages * 52.0 / 0.78 - c.sponsorIncome
                val lg = world.leagues[c.league]
                val homeGames = lg.clubs.size - 1
                model += tvPool(world, lg) / lg.clubs.size + merchYear(world, c) + adsYear(world, c) +
                    homeGames * attendance(world, c, -1, CompKind.LEAGUE) * ticketPrice(world, c) * 0.88
            }
            st.leagueMul[lg0.id] = (target / model).toFloat().coerceIn(0.1f, 12f)
        }
        // Sponsorlar
        for (c in world.clubs) if (c.league >= 0) {
            c.sponsorName = SPONSORS[rnd.int(SPONSORS.size)]
            c.sponsorEnd = world.season + rnd.range(0, 3)
            c.trainArea = -1; c.intensity = 1
            c.balanceHistory.clear(); c.balanceHistory.add((c.balance / 1000).toInt())
        }
        // Serbest personel havuzu
        val gen = FbGen(world.seed xor 0xC0FFEEL, world)
        repeat(260) {
            val ci = rnd.int(FbCountries.all.size)
            val role = StaffRole.entries[rnd.int(StaffRole.entries.size)]
            gen.makeStaffMember(ci, FbCountries.all[ci], role, 5f + rnd.float() * 11f).club = CLUB_NONE
        }
    }

    // ================================================================== kancalar

    /** Her gun (FbSeason.dailyUpdate sonrasi). */
    fun onDay(world: FbWorld) {
        val cal = FbSeason.calendar(world)
        // Ay basi: aylik gelir/gider toplamlarini devret
        if (cal.date(world.day).third == 1) for (c in world.clubs) {
            c.lastMonthIn = c.monthIn; c.lastMonthOut = c.monthOut; c.monthIn = 0; c.monthOut = 0
        }
        if (cal.weekday(world.day) == FbCalendar.MON) weekly(world)
        if (world.day == cal.dayOf(3, 15)) youthIntake(world)
        FbTransfer.onDay(world)
        FbNewsFeed.daily(world)
        // Yogunluk ve fizyo etkisi (kulup oyunculari)
        for (c in world.clubs) {
            if (c.league < 0) continue
            val adj = when (c.intensity) { 0 -> 1; 2 -> -1; else -> 0 }
            if (adj == 0) continue
            for (pid in c.players) { val p = world.players[pid]; p.condition = (p.condition + adj).coerceIn(30, 100) }
        }
    }

    /** Mac sonucu islendikten sonra (FbSeason.submit). */
    fun onFixture(world: FbWorld, f: FbFixture, out: MatchOutcome, comp: FbComp) {
        val national = comp.national
        for ((side, lines) in listOf(0 to out.home, 1 to out.away)) {
            for (l in lines) {
                val p = l.p
                p.playTime += if (l.started) 90 else 25
                if (l.started && p.fam(l.pos) < 20 && (p.id * 31 + f.id) % 7 == 0) p.posFam[l.pos.ordinal] = (p.fam(l.pos) + 1).toByte().also { p.invalidate() }
            }
            // Fizyoterapi: sakatlik suresi
            if (!national) {
                val club = world.clubs[if (side == 0) f.home else f.away]
                val physio = bestStaff(world, club, StaffAttr.PHYSIOTHERAPY, StaffRole.PHYSIO)
                for (l in lines) if (l.injuryDays > 0) l.p.injuryDays = max(1, (l.p.injuryDays * (1.2f - physio / 20f * 0.45f)).roundToInt())
            }
        }
        if (national) return
        // Bilet geliri (ev sahibi; tarafsiz sahada iki takim paylasir)
        val home = world.clubs[f.home]
        val neutral = FbSeason.isNeutral(world, f)
        val att = attendance(world, home, f.away, comp.kind)
        val gross = (att * ticketPrice(world, home) * fm(world, home.league) * if (comp.kind in EURO) 1.4f else if (comp.kind == CompKind.FRIENDLY) 0.5f else 1f).roundToLong()
        if (neutral) {
            for (t in intArrayOf(f.home, f.away)) { world.clubs[t].book(Fin.TICKETS, gross / 3); world.clubs[t].book(Fin.MATCHDAY, gross / 30) }
        } else {
            home.book(Fin.TICKETS, gross); home.book(Fin.MATCHDAY, (gross * 0.12).roundToLong())
            if (att >= home.capacity) home.soldOut++
        }
        // Avrupa galibiyet/beraberlik primi
        val prize = EURO_PRIZE[comp.kind]
        if (prize != null && f.stage >= 0 && comp.stages[f.stage].type == StageType.SWISS) {
            when {
                out.hg > out.ag -> world.clubs[f.home].book(Fin.PRIZE, prize[1])
                out.hg < out.ag -> world.clubs[f.away].book(Fin.PRIZE, prize[1])
                else -> { world.clubs[f.home].book(Fin.PRIZE, prize[2]); world.clubs[f.away].book(Fin.PRIZE, prize[2]) }
            }
        }
    }

    private var recentCache: Map<Int, List<Int>> = emptyMap()

    private val AREA_MASK = Array(TrainArea.entries.size) { i -> BooleanArray(Attr.COUNT) { Attr.entries[it] in TrainArea.entries[i].attrs } }
    private val NO_MASK = BooleanArray(Attr.COUNT)

    private val EURO = setOf(CompKind.CHAMPIONS, CompKind.EUROPA, CompKind.CONFERENCE)

    /** Avrupa primleri: [katilim, galibiyet, beraberlik, play-off, son16, ceyrek, yari, final, sampiyon] */
    private val EURO_PRIZE = mapOf(
        CompKind.CHAMPIONS to longArrayOf(18_600_000, 2_100_000, 700_000, 1_000_000, 11_000_000, 12_500_000, 15_000_000, 18_500_000, 6_500_000),
        CompKind.EUROPA to longArrayOf(4_300_000, 450_000, 150_000, 300_000, 1_750_000, 2_500_000, 4_000_000, 7_000_000, 6_000_000),
        CompKind.CONFERENCE to longArrayOf(3_200_000, 400_000, 130_000, 200_000, 800_000, 1_300_000, 2_500_000, 4_000_000, 3_000_000),
    )

    /** Lig asamasi kuruldu: katilim primi. */
    fun onLeaguePhase(world: FbWorld, comp: FbComp, teams: List<Int>) {
        val pr = EURO_PRIZE[comp.kind] ?: return
        for (t in teams) world.clubs[t].book(Fin.PRIZE, pr[0])
    }

    /** Eleme turu kazanildi (bir sonraki tura cikis primi). */
    fun onTieWon(world: FbWorld, comp: FbComp, stage: FbStage, winner: Int, isFinal: Boolean) {
        if (comp.national || winner < 0) return
        val c = world.clubs[winner]
        val pr = EURO_PRIZE[comp.kind]
        if (pr != null) {
            val v = when (stage.label) {
                StageLabel.Q1, StageLabel.Q2, StageLabel.Q3, StageLabel.QPO -> 300_000L
                StageLabel.KPO -> pr[4]; StageLabel.R16 -> pr[5]; StageLabel.QF -> pr[6]; StageLabel.SF -> pr[7]
                StageLabel.FINAL -> pr[8]
                else -> 0L
            }
            c.book(Fin.PRIZE, v)
            return
        }
        val base = 6e6 * lv(c.country).toDouble().pow(4.0) * fm(world, c.league)
        val v = when (comp.kind) {
            CompKind.CUP -> base * when (stage.label) { StageLabel.FINAL -> 0.5; StageLabel.SF -> 0.2; StageLabel.QF -> 0.08; else -> 0.025 }
            CompKind.SUPER_CUP -> base * 0.15
            CompKind.UEFA_SUPER -> 1_000_000.0
            CompKind.PLAYOFF -> if (isFinal) base * 0.3 else 0.0
            else -> 0.0
        }
        if (v > 0) c.book(Fin.PRIZE, v.roundToLong())
    }

    // ================================================================== haftalik

    fun weekly(world: FbWorld) {
        val rnd = FbRandom(world.seed xor (world.season * 1000L + world.day))
        val q = HashMap<Int, TrainQuality>()
        recentCache = world.state?.recentAll(world.day, 5) ?: emptyMap()
        for (c in world.clubs) {
            if (c.league < 0) continue
            val t = trainQuality(world, c)
            q[c.id] = t
            finances(world, c)
            upgradeCheck(world, c)
            morale(world, c, t, rnd)
        }
        FbBanking.weekly(world)
        val defaults = HashMap<Int, TrainQuality>()
        for (p in world.players) {
            if (p.retired) continue
            if (p.loanFrom >= 0 && p.loanWagePct < 100) world.clubs[p.loanFrom].book(Fin.WAGES, p.wage.toLong() * (100 - p.loanWagePct) / 100)
            val t = if (p.club >= 0) q[p.club]!! else defaults.getOrPut(p.nation) { defaultQuality(p.nation) }
            develop(world, p, t, rnd)
            p.playTime = (p.playTime * 0.75f).toInt()
            if (p.club < 0) p.form += ((65 - p.form) * 0.1f).roundToInt()
        }
        FbNewsFeed.weekly(world)
    }

    private fun finances(world: FbWorld, c: FbClub) {
        c.book(Fin.WAGES, c.players.sumOf { val p = world.players[it]; if (p.loanFrom >= 0) p.wage.toLong() * p.loanWagePct / 100 else p.wage.toLong() })
        c.book(Fin.STAFF_WAGES, c.staff.sumOf { world.staff[it].wage.toLong() })
        val tf = tierCost(world, c)
        c.book(Fin.FACILITIES, (upkeepWeek(c) * tf).roundToLong())
        c.book(Fin.YOUTH, (youthWeek(c) * tf).roundToLong())
        c.book(Fin.SPONSOR, c.sponsorIncome / 52)
        c.book(Fin.MERCH, (merchYear(world, c) / 52).roundToLong())
        c.book(Fin.ADS, (adsYear(world, c) / 52).roundToLong())
        val lg = world.leagues[c.league]
        c.book(Fin.TV, (tvPool(world, lg) * 0.5 / lg.clubs.size / 52).roundToLong())
        if (c.balance < 0) c.book(Fin.INTEREST, (-c.balance * 0.0012).roundToLong())
        c.balanceHistory.add((c.balance / 1000).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())
        while (c.balanceHistory.size > 60) c.balanceHistory.removeAt(0)
    }

    // ================================================================== antrenman ve gelisim

    fun bestStaff(world: FbWorld, c: FbClub, a: StaffAttr, vararg roles: StaffRole): Float {
        var best = 0; var second = 0
        for (sid in c.staff) {
            val s = world.staff[sid]
            if (roles.isNotEmpty() && s.role !in roles) continue
            val v = s.attr(a)
            if (v > best) { second = best; best = v } else if (v > second) second = v
        }
        if (best == 0) return 4f
        return if (second == 0) best * 0.85f else best * 0.65f + second * 0.35f
    }

    private val COACH_ROLES = arrayOf(StaffRole.COACH, StaffRole.ASSISTANT, StaffRole.GK_COACH, StaffRole.FITNESS_COACH)

    fun trainQuality(world: FbWorld, c: FbClub): TrainQuality {
        val coaches = c.staff.count { world.staff[it].role in COACH_ROLES }
        // Az antrenor = antrenorler cok alana bolunur
        val load = (coaches / 5f).coerceIn(0.55f, 1.1f)
        val coach = FloatArray(TrainArea.entries.size) { i ->
            val a = TrainArea.entries[i]
            val roles = when (a) { TrainArea.GOALKEEPING -> arrayOf(StaffRole.GK_COACH); TrainArea.PHYSICAL -> arrayOf(StaffRole.FITNESS_COACH, StaffRole.COACH); else -> arrayOf(StaffRole.COACH, StaffRole.ASSISTANT) }
            (bestStaff(world, c, a.coach, *roles) * load).coerceIn(1f, 20f)
        }
        return TrainQuality(
            coach = coach, facilities = c.trainingFacilities.toFloat(),
            youthCoach = bestStaff(world, c, StaffAttr.WORKING_WITH_YOUTH, StaffRole.YOUTH_COACH, StaffRole.COACH),
            youthFacilities = c.youthFacilities.toFloat(),
            physio = bestStaff(world, c, StaffAttr.PHYSIOTHERAPY, StaffRole.PHYSIO),
            fitness = bestStaff(world, c, StaffAttr.COACH_FITNESS, StaffRole.FITNESS_COACH),
            manMgmt = (bestStaff(world, c, StaffAttr.MAN_MANAGEMENT, StaffRole.ASSISTANT) + bestStaff(world, c, StaffAttr.MOTIVATING)) / 2f,
            intensity = c.intensity, area = c.trainArea, social = c.socialFacilities.toFloat(),
        )
    }

    private fun defaultQuality(nation: Int): TrainQuality {
        val q = 5f + lv(nation) * 9f
        return TrainQuality(FloatArray(TrainArea.entries.size) { q }, q, q, q, q, q, q, 1, -1, q)
    }

    /** Yasa gore yillik "potansiyele yaklasma" orani. */
    fun growthRate(age: Int): Float = when {
        age <= 17 -> 0.16f; age <= 19 -> 0.15f; age <= 21 -> 0.13f; age <= 23 -> 0.10f
        age <= 25 -> 0.065f; age <= 27 -> 0.035f; age <= 29 -> 0.012f; else -> 0f
    }

    /** Yasa gore yillik CA kaybi (kaleciler 2 yil gec). */
    fun declineRate(age: Int, gk: Boolean): Float {
        val a = if (gk) age - 2 else age
        return when { a <= 29 -> 0f; a == 30 -> 1f; a == 31 -> 2f; a == 32 -> 3f; a == 33 -> 4f; a == 34 -> 5.5f; else -> 7f + (a - 35) * 1.5f }
    }

    /** Bir haftalik gelisim/yaslanma. Kulup antrenman kalitesi, kisilik, oynama suresi, sakatlik etkili. */
    fun develop(world: FbWorld, p: FbPlayer, q: TrainQuality, rnd: FbRandom) {
        val age = world.age(p)
        val best = p.bestPos()
        val gk = best == Pos.GK
        val ca = p.currentAbility()
        val prof = p.hidden(Hidden.PROFESSIONALISM)
        val det = p.attr(Attr.DETERMINATION)
        val personality = 0.6f + (prof + det) / 40f * 0.8f // 0.64 .. 1.4
        val intensityMul = when (q.intensity) { 0 -> 0.85f; 2 -> 1.15f; else -> 1f }
        val injured = p.injuryDays > 0
        // Antrenor kalitesi: oyuncunun alanina gore agirlikli
        val coachQ = if (gk) q.coach[TrainArea.GOALKEEPING.ordinal] * 0.6f + q.coach.average().toFloat() * 0.4f else q.coach.average().toFloat()
        val young = age < 19
        val cq = if (young) coachQ * 0.6f + q.youthCoach * 0.4f else coachQ
        val fq = if (young) q.facilities * 0.5f + q.youthFacilities * 0.5f else q.facilities
        val coachMul = 0.6f + cq / 20f * 0.8f
        val facMul = 0.72f + fq / 20f * 0.6f
        val play = (p.playTime / 300f).coerceIn(0f, 1.2f)
        val playMul = if (age <= 23) 0.8f + 0.35f * play else 0.95f + 0.1f * play
        val moraleMul = 0.9f + 0.2f * p.morale / 100f

        var deltaCa = 0f
        val gap = p.potential - ca
        if (gap > 0) deltaCa += growthRate(age) * gap / 52f * personality * coachMul * facMul * playMul * moraleMul * intensityMul * (if (injured) 0.3f else 1f)
        val dec = declineRate(age, gk)
        if (dec > 0f) {
            val fitnessMul = 1.25f - q.fitness / 20f * 0.4f - prof / 20f * 0.25f - p.attr(Attr.NATURAL_FITNESS) / 20f * 0.2f
            deltaCa -= dec / 52f * fitnessMul.coerceIn(0.5f, 1.2f)
        }
        // Tesadufi dalgalanma (tutarlilik)
        if (deltaCa != 0f) deltaCa += rnd.gauss() * 0.04f
        p.devAcc += deltaCa
        if (abs(p.devAcc) >= 0.6f) { applyCaDelta(p, p.devAcc, best, age, q, rnd); p.devAcc = 0f }

        // Mevki antrenmani
        if (p.trainPos >= 0 && !injured) {
            val pos = Pos.entries[p.trainPos]
            val f = p.fam(pos)
            if (f < 20) {
                val chance = (0.08f + p.hidden(Hidden.VERSATILITY) / 20f * 0.18f + q.coach[TrainArea.TACTICAL.ordinal] / 20f * 0.1f) * (if (age < 24) 1.3f else 1f)
                if (rnd.chance(chance)) { p.posFam[pos.ordinal] = (f + 1).toByte(); p.invalidate() }
            }
        }
        // Antrenman sakatligi
        if (!injured && p.club >= 0) {
            val im = when (q.intensity) { 0 -> 0.5f; 2 -> 1.8f; else -> 1f }
            val pr = 0.0035f * im * (0.5f + p.hidden(Hidden.INJURY_PRONENESS) / 14f) * (1.25f - q.physio / 20f * 0.6f)
            if (rnd.chance(pr)) { p.injuryDays = 3 + rnd.int(18); FbNewsFeed.trainingInjury(world, p) }
        }
    }

    /**
     * CA degisimini yeteneklere dagit. Buyume: mevki agirliklari + takim/bireysel odak + yas egilimi;
     * gerileme: once fiziksel, zihinsel yetenekler yasla hafif artmaya devam eder.
     * Yetenekler 0.1'lik adimlarla tutuldugu icin olasiliksal yuvarlama.
     */
    private fun applyCaDelta(p: FbPlayer, deltaCa: Float, best: Pos, age: Int, q: TrainQuality, rnd: FbRandom) {
        val w = PosWeights.arr(best)
        val teamFocus = if (q.area >= 0) AREA_MASK[q.area] else NO_MASK
        val indFocus = if (p.trainArea >= 0) AREA_MASK[p.trainArea] else NO_MASK
        val gk = best == Pos.GK
        val weights = FloatArray(Attr.COUNT)
        var sum = 0f; var n = 0
        for (a in Attr.entries) {
            if ((a.group == AttrGroup.GOALKEEPING) != gk && a.group != AttrGroup.MENTAL && a.group != AttrGroup.PHYSICAL) {
                if (!(gk && a.group == AttrGroup.TECHNICAL)) continue
            }
            val wa = w[a.ordinal]
            var x = if (wa > 0f) wa else if (gk && a.group == AttrGroup.TECHNICAL) 0.1f else 0.35f
            if (teamFocus[a.ordinal]) x *= 1.5f
            if (indFocus[a.ordinal]) x *= 2.2f
            if (deltaCa > 0) {
                if (a.group == AttrGroup.PHYSICAL) x *= if (age <= 21) 1.4f else if (age >= 26) 0.4f else 1f
                if (a.group == AttrGroup.MENTAL) x *= if (age >= 24) 1.3f else 0.9f
            } else {
                x = when (a.group) { AttrGroup.PHYSICAL -> x * 3f; AttrGroup.MENTAL -> x * 0.25f; else -> x }
            }
            weights[a.ordinal] = x; sum += x; n++
        }
        if (n == 0) return
        val mean = sum / n
        var changed = false
        for (a in Attr.entries) {
            val x = weights[a.ordinal]
            if (x <= 0f) continue
            // attr birimi (1..20) cinsinden beklenen degisim -> 0.1'lik adim sayisi
            val steps = deltaCa / 10f * (x / mean) * 10f
            var k = steps.toInt()
            if (rnd.float() < abs(steps - k)) k += if (steps > 0) 1 else -1
            if (k == 0) continue
            val i = a.ordinal
            val cur = p.attrs[i].toInt() and 0xFF
            val nv = (cur + k).coerceIn(10, 200)
            if (nv != cur) { p.attrs[i] = nv.toByte(); changed = true }
        }
        // Zihinsel olgunlasma (yasli oyuncular gerilerken de)
        if (deltaCa < 0 && rnd.chance(0.15f)) {
            val a = listOf(Attr.DECISIONS, Attr.POSITIONING, Attr.ANTICIPATION, Attr.COMPOSURE, Attr.LEADERSHIP)[rnd.int(5)]
            val cur = p.attrs[a.ordinal].toInt() and 0xFF
            if (cur < 200) { p.attrs[a.ordinal] = (cur + 1).toByte(); changed = true }
        }
        if (changed) p.invalidate()
    }

    // ================================================================== moral

    /** Kadro statusu: CA sirasina gore 0 kilit, 1 rotasyon, 2 yedek, 3 genc. */
    fun squadStatus(world: FbWorld, c: FbClub, p: FbPlayer, rank: Int): Int = when {
        world.age(p) < 20 && rank > 14 -> 3
        rank < 11 -> 0
        rank < 18 -> 1
        else -> 2
    }

    private fun morale(world: FbWorld, c: FbClub, q: TrainQuality, rnd: FbRandom) {
        val st = world.state ?: return
        // Son 5 resmi mac
        val recent = recentCache[c.id] ?: emptyList()
        val resultAdj = if (recent.isEmpty()) 0f else (recent.sum() / (recent.size * 3f) - 0.45f) * 30f
        val sorted = c.players.map { world.players[it] }.sortedByDescending { it.currentAbility() }
        for ((rank, p) in sorted.withIndex()) {
            val status = squadStatus(world, c, p, rank)
            val share = (p.playTime / 300f).coerceIn(0f, 1.2f)
            val expect = when (status) { 0 -> 0.6f; 1 -> 0.3f; 2 -> 0.08f; else -> 0.1f }
            val playAdj = if (p.injuryDays > 0) 0f else ((share - expect).coerceAtMost(0.2f) * 25f).coerceAtLeast(-18f)
            val expectedWage = FbEconomy.wage(p.currentAbility(), world.age(p), c.reputation)
            val wageAdj = ((p.wage.toFloat() / expectedWage - 1f) * 12f).coerceIn(-10f, 6f)
            val target = 62f + resultAdj + playAdj + wageAdj + (q.manMgmt - 10f) * 0.6f + (q.social - 10f) * 0.3f +
                (p.hidden(Hidden.PROFESSIONALISM) - 10) * 0.3f
            p.morale = (p.morale + (target - p.morale) * 0.3f + rnd.gauss() * 1.5f).roundToInt().coerceIn(5, 100)
            if (p.playTime < 30) p.form += ((65 - p.form) * 0.06f).roundToInt()
        }
    }

    // ================================================================== altyapi

    fun youthIntake(world: FbWorld) {
        val rnd = FbRandom(world.seed xor (world.season * 31L + 7))
        val gen = FbGen(world.seed xor (world.season * 7919L), world)
        for (c in world.clubs) {
            if (c.league < 0) continue
            val cd = FbCountries.all[c.country]
            val count = 2 + c.youthRecruitment / 5 + rnd.int(3)
            val yc = bestStaff(world, c, StaffAttr.WORKING_WITH_YOUTH, StaffRole.YOUTH_COACH)
            val newcomers = ArrayList<FbPlayer>()
            repeat(count) {
                val golden = rnd.chance(0.02f)
                val pa = (55f + c.youthRecruitment * 2.5f + c.youthFacilities * 1.5f + cd.level * 0.45f + (yc - 10f) +
                    rnd.gauss() * 20f + if (golden) 35f else 0f).roundToInt().coerceIn(40, 200)
                val age = 15 + rnd.int(2)
                val ca = (pa * (0.33f + rnd.float() * 0.14f) + (age - 15) * 6).roundToInt().coerceIn(25, pa)
                val nat = if (rnd.chance(0.9f)) c.country else rnd.int(FbCountries.all.size)
                val p = gen.makePlayer(nat, FbCountries.all[nat], 0f, null, c.id, fixedAge = age, fixedCa = ca, fixedPotential = pa)
                p.wage = max(150, (FbEconomy.wage(ca, age, c.reputation) * 0.5).roundToInt() / 50 * 50)
                p.contractEnd = world.season + 3
                p.reputation = (c.reputation * 0.05f).roundToInt()
                p.squadNumber = (30..99).firstOrNull { n -> c.players.none { world.players[it].squadNumber == n } } ?: 99
                p.youth = true; p.caStart = ca
                c.players.add(p.id)
                newcomers.add(p)
            }
            FbNewsFeed.youthIntake(world, c, newcomers)
        }
    }

    // ================================================================== tesis yatirimi

    fun facilityLevel(c: FbClub, f: Facility): Int = when (f) {
        Facility.TRAINING -> c.trainingFacilities; Facility.YOUTH -> c.youthFacilities; Facility.RECRUITMENT -> c.youthRecruitment
        Facility.STADIUM -> c.stadiumLevel; Facility.SOCIAL -> c.socialFacilities; Facility.CAPACITY -> c.capacity
    }

    fun capacityStep(c: FbClub) = max(1000, (c.capacity * 0.1f).roundToInt() / 500 * 500)

    fun upgradeCost(c: FbClub, f: Facility): Long {
        val cs = costScale(c.country)
        return if (f == Facility.CAPACITY) (capacityStep(c) * 2500.0 * cs).roundToLong()
        else FbEconomy.roundValue((400_000.0 * (facilityLevel(c, f) + 1.0).pow(1.7) * cs).roundToLong())
    }

    fun upgradeDays(c: FbClub, f: Facility): Int = if (f == Facility.CAPACITY) 240 else 60 + facilityLevel(c, f) * 12

    /** Yatirim baslat. Basarisizsa neden (null = basladi). */
    fun startUpgrade(world: FbWorld, c: FbClub, f: Facility): String? {
        if (c.upgradeType >= 0) return "BUSY"
        if (f != Facility.CAPACITY && facilityLevel(c, f) >= 20) return "MAX"
        val cost = upgradeCost(c, f)
        if (c.balance < cost) return "FUNDS"
        c.book(Fin.UPGRADES, cost)
        c.upgradeType = f.ordinal
        val end = world.day + upgradeDays(c, f)
        c.upgradeSeason = world.season + end / 365
        c.upgradeDay = end % 365
        return null
    }

    private fun upgradeCheck(world: FbWorld, c: FbClub) {
        if (c.upgradeType < 0) return
        if (world.season < c.upgradeSeason || (world.season == c.upgradeSeason && world.day < c.upgradeDay)) return
        when (Facility.entries[c.upgradeType]) {
            Facility.TRAINING -> c.trainingFacilities++
            Facility.YOUTH -> c.youthFacilities++
            Facility.RECRUITMENT -> c.youthRecruitment++
            Facility.STADIUM -> c.stadiumLevel++
            Facility.SOCIAL -> c.socialFacilities++
            Facility.CAPACITY -> c.capacity += capacityStep(c)
        }
        FbNewsFeed.facilityDone(world, c, Facility.entries[c.upgradeType])
        c.upgradeType = -1
    }

    // ================================================================== sponsorluk

    /** Bu sezon icin sponsor teklifleri (belirli: ayni sezon ayni teklifler). */
    fun sponsorOffers(world: FbWorld, c: FbClub): List<SponsorOffer> {
        val rnd = FbRandom(world.seed xor (c.id * 977L) xor (world.season * 31L))
        val base = max(c.sponsorIncome.toDouble(), 3e6 * (c.reputation / 10000.0).pow(2.5) * fm(world, c.league) + 50_000)
        return List(3) { i ->
            val years = intArrayOf(1, 3, 5)[i]
            val mul = (1.12 - 0.06 * years) * (0.9 + rnd.float() * 0.25)
            SponsorOffer(SPONSORS[rnd.int(SPONSORS.size)], FbEconomy.roundValue((base * mul).roundToLong()), years)
        }
    }

    fun acceptSponsor(world: FbWorld, c: FbClub, offer: SponsorOffer) {
        c.sponsorName = offer.name; c.sponsorIncome = offer.yearly; c.sponsorEnd = world.season + offer.years - 1
    }

    // ================================================================== reklam bonusu

    const val AD_BONUS_PER_WEEK = 3

    /**
     * Reklam izleyince kulube eklenecek tutar. Buyuk kulupte yillik gelirin %1.5'i; gelir kuculdukce
     * oran %5'e kadar cikar, alt liglerde ek carpan (2. lig x1.25, 3. lig ve alti x1.5). En az 100 bin.
     */
    fun adBonusAmount(world: FbWorld, c: FbClub): Long {
        val rev = FbBanking.yearRevenue(world, c).coerceAtLeast(1L)
        var pct = (0.015 * kotlin.math.sqrt(30_000_000.0 / rev)).coerceIn(0.015, 0.05)
        val tier = world.leagues.getOrNull(c.league)?.tier ?: 1
        pct *= when { tier >= 3 -> 1.5; tier == 2 -> 1.25; else -> 1.0 }
        return FbEconomy.roundValue(max(100_000L, (rev * pct).toLong()))
    }

    fun adBonusLeft(world: FbWorld): Int {
        val wk = world.season * 100 + world.day / 7
        return if (world.adBonusWeek != wk) AD_BONUS_PER_WEEK else (AD_BONUS_PER_WEEK - world.adBonusCount).coerceAtLeast(0)
    }

    /** Odul verildi: tutari ekle (hak yoksa 0). */
    fun claimAdBonus(world: FbWorld): Long {
        if (world.userClub < 0 || adBonusLeft(world) <= 0) return 0
        val wk = world.season * 100 + world.day / 7
        if (world.adBonusWeek != wk) { world.adBonusWeek = wk; world.adBonusCount = 0 }
        world.adBonusCount++
        val c = world.clubs[world.userClub]
        val v = adBonusAmount(world, c)
        c.book(Fin.OTHER_IN, v)
        return v
    }

    // ================================================================== personel

    fun freeStaff(world: FbWorld): List<FbStaff> = world.staff.filter { it.club == CLUB_NONE }

    /** Personel istenen maas (kulup itibarina gore). */
    fun staffAskingWage(c: FbClub, s: FbStaff): Int = (s.wage * (0.9f + c.reputation / 10000f * 0.4f)).roundToInt() / 50 * 50

    fun hireStaff(world: FbWorld, c: FbClub, s: FbStaff, years: Int = 2): String? {
        if (s.club != CLUB_NONE) return "TAKEN"
        if (c.staff.size >= 16) return "FULL"
        if (s.reputation > c.reputation + 3000) return "REJECT"
        s.wage = staffAskingWage(c, s); s.club = c.id; s.contractEnd = world.season + years - 1
        c.staff.add(s.id)
        return null
    }

    /** Sozlesme feshi: kalan sozlesmenin yarisi tazminat. */
    fun fireStaff(world: FbWorld, c: FbClub, s: FbStaff) {
        val years = max(0, s.contractEnd - world.season) + 1
        val weeksLeft = years * 52 - world.day / 7
        c.book(Fin.OTHER_OUT, s.wage.toLong() * weeksLeft / 2)
        c.staff.remove(s.id); s.club = CLUB_NONE
    }

    fun severance(world: FbWorld, s: FbStaff): Long {
        val years = max(0, s.contractEnd - world.season) + 1
        return s.wage.toLong() * (years * 52 - world.day / 7) / 2
    }

    // ================================================================== sezon sonu

    /** Sezon biterken (yeni sezon kurulmadan, tablolar hazirken) cagrilir. */
    fun onSeasonEnd(world: FbWorld) {
        val st = world.state!!
        val rnd = FbRandom(world.seed xor (world.season * 104729L) xor 0xEE)
        // ---- lig sirasi primi (yayin gelirinin performans payi) ve itibar
        val honoursNow = st.honours.filter { it.season == world.season }
        for (comp in st.comps) {
            if (comp.kind != CompKind.LEAGUE) continue
            val lg = world.leagues[comp.league]
            val table = st.table(comp.id, 0, 0)
            val n = table.size
            val pool = tvPool(world, lg) * 0.5
            val totalW = n * (n + 1) / 2.0
            val byRep = lg.clubs.sortedByDescending { world.clubs[it].reputation }
            for ((pos, row) in table.withIndex()) {
                val c = world.clubs[row.team]
                c.book(Fin.TV, (pool * (n - pos) / totalW).roundToLong())
                c.lastPosition = pos + 1
                // Itibar: beklentiye gore
                val expected = byRep.indexOf(row.team)
                var d = (expected - pos) * 28f * (if (lg.tier == 1) 1f else 0.6f)
                if (pos == 0) d += if (lg.tier == 1) 300f else 150f
                if (pos >= n - lg.relegated && lg.relegated > 0) d -= 350f
                if (lg.tier > 1 && pos < lg.promoted) d += 220f
                // Lig itibari: ust ligler kulubu yukari, alt ligler asagi ceker
                val leagueRep = lg.clubs.map { world.clubs[it].reputation }.average().toFloat()
                d += (leagueRep - c.reputation) * 0.03f
                c.reputation = (c.reputation + d).roundToInt().coerceIn(100, 10000)
                c.fanBase = (c.fanBase * (1f + d / 12000f)).roundToInt().coerceAtLeast(500)
            }
        }
        for (h in honoursNow) {
            if (h.kind.national || h.winner < 0) continue
            val bonus = when (h.kind) { CompKind.CHAMPIONS -> 800; CompKind.EUROPA -> 400; CompKind.CONFERENCE -> 250; CompKind.CUP -> 150; else -> 0 }
            if (bonus > 0) world.clubs[h.winner].reputation = (world.clubs[h.winner].reputation + bonus).coerceAtMost(10000)
        }

        // ---- oyuncu itibari
        val seasonApps = HashMap<Int, Pair<Int, Int>>() // oyuncu -> (mac, puan toplam x10)
        for ((k, s) in st.stats) {
            val comp = st.comps[(k shr 32).toInt()]
            if (!comp.kind.official) continue
            val pid = (k and 0xffffffffL).toInt()
            val cur = seasonApps[pid] ?: (0 to 0)
            seasonApps[pid] = (cur.first + s.apps + s.subs) to (cur.second + s.ratingSum)
        }
        for (p in world.players) {
            if (p.retired) continue
            val clubRep = if (p.club >= 0) world.clubs[p.club].reputation else 1500
            val (apps, rs) = seasonApps[p.id] ?: (0 to 0)
            val avg = if (apps > 0) rs / 10f / apps else 6.5f
            val target = ((p.currentAbility() - 40) / 150f).coerceIn(0f, 1f).pow(1.5f) * 7500f + clubRep * 0.2f + p.caps * 15f +
                (if (apps >= 15) (avg - 6.8f) * 1200f else 0f)
            p.reputation = (p.reputation + (target - p.reputation) * 0.3f).roundToInt().coerceIn(0, 10000)
        }

        FbNewsFeed.seasonAwards(world)
        FbTransfer.onSeasonEnd(world)
        // Altyapidan mezuniyet: 19 yasina giren gencler A takima
        for (p in world.players) if (p.youth && p.club >= 0 && world.age(p) + 1 >= 19) { p.youth = false; FbNewsFeed.graduate(world, p) }
        retirements(world, rnd)
        contracts(world, rnd)
        squads(world, rnd)
        pools(world, rnd)

        // ---- yapay zeka yatirimlari ve sponsor yenileme, personel sozlesmeleri
        for (c in world.clubs) {
            if (c.league < 0) continue
            if (c.id != world.userClub) {
                val yearlyRev = c.ledger.withIndex().filter { Fin.entries[it.index].income }.sumOf { it.value }
                if (c.upgradeType < 0 && c.balance > yearlyRev * 0.35 && rnd.chance(0.5f)) {
                    val f = if (c.soldOut > 12) Facility.CAPACITY else listOf(Facility.TRAINING, Facility.YOUTH, Facility.RECRUITMENT, Facility.STADIUM, Facility.SOCIAL).minBy { facilityLevel(c, it) }
                    startUpgrade(world, c, f)
                }
                if (c.sponsorEnd < world.season + 1) acceptSponsor(world, c, sponsorOffers(world, c)[1].let { SponsorOffer(it.name, it.yearly, it.years) })
                // Personel: sozlesmesi biten yenilenir
                for (sid in c.staff) { val s = world.staff[sid]; if (s.contractEnd <= world.season) s.contractEnd = world.season + 1 + rnd.int(3) }
            } else {
                if (c.sponsorEnd < world.season + 1) acceptSponsor(world, c, sponsorOffers(world, c)[0])
                for (sid in c.staff.toList()) { val s = world.staff[sid]; if (s.contractEnd <= world.season) { c.staff.remove(sid); s.club = CLUB_NONE } }
            }
            c.soldOut = 0
            for (i in c.ledger.indices) { c.lastLedger[i] = c.ledger[i]; c.ledger[i] = 0 }
        }
    }

    private fun retirements(world: FbWorld, rnd: FbRandom) {
        for (p in world.players) {
            if (p.retired) continue
            val age = world.age(p) + 1 // sezon sonu: bir yas buyuyor sayilir
            val gk = p.bestPos() == Pos.GK
            val a = if (gk) age - 2 else age
            var pr = when { a < 33 -> 0f; a == 33 -> 0.08f; else -> 0.25f + (a - 34) * 0.2f }
            if (a >= 31 && p.currentAbility() < 55) pr += 0.2f
            if (p.club == CLUB_NONE) {
                if (a >= 30) pr += 0.5f
                else if (a >= 23 && p.currentAbility() < 75) pr += 0.45f
                else if (a >= 20 && p.potential < 70) pr += 0.5f
            }
            if (pr > 0f && rnd.chance(pr.coerceAtMost(0.98f))) {
                val former = p.club
                if (p.club >= 0) world.clubs[p.club].players.remove(p.id)
                p.club = CLUB_RETIRED; p.wage = 0
                FbNewsFeed.retired(world, p, former)
            }
        }
    }

    /** Sozlesmesi biten oyuncular: kulup isterse yenilenir, yoksa serbest kalir. (Kullanici icin pazarlik 5. adimda.) */
    private fun contracts(world: FbWorld, rnd: FbRandom) {
        for (c in world.clubs) {
            if (c.league < 0 || c.id == world.userClub) continue
            val sorted = c.players.map { world.players[it] }.sortedByDescending { it.currentAbility() }
            val cut = sorted.getOrNull(21)?.currentAbility() ?: 0
            for (p in sorted) {
                if (p.contractEnd > world.season) continue
                val age = world.age(p) + 1
                val wanted = p.currentAbility() >= cut || (age <= 23 && p.potential > cut + 10)
                val willing = p.morale > 35 || rnd.chance(0.5f)
                if (wanted && willing && age < 35) {
                    p.contractEnd = world.season + 1 + rnd.int(if (age < 29) 4 else 2)
                    p.wage = FbEconomy.wage(p.currentAbility(), age, c.reputation)
                } else {
                    c.players.remove(p.id); p.club = CLUB_NONE
                    p.contractEnd = world.season
                }
            }
        }
    }

    /** Yapay zeka kadro tamamlama/budama (tam transfer pazari 5. adimda). */
    private fun squads(world: FbWorld, rnd: FbRandom) {
        val free = world.players.filter { it.club == CLUB_NONE }.toMutableList()
        for (c in world.clubs) {
            if (c.league < 0 || c.id == world.userClub) continue
            val tier = world.leagues[c.league].tier
            val minSize = when (tier) { 1 -> 24; 2 -> 22; else -> 20 }
            // Fazla kalabalik: once umutsuz gencler, sonra en zayiflar
            val maxSize = when (tier) { 1 -> 30; 2 -> 28; else -> 26 }
            while (c.players.size > maxSize) {
                val ps = c.players.map { world.players[it] }
                val worst = ps.minBy { val age = world.age(it); it.currentAbility() + (if (age <= 20) (it.potential - it.currentAbility()) * 0.6f else 0f) + (if (age <= 18) 15f else 0f) }
                c.players.remove(worst.id); worst.club = CLUB_NONE; worst.wage = 0; free.add(worst)
            }
            if (c.players.size >= minSize) continue
            val level = c.players.map { world.players[it].currentAbility() }.sortedDescending().take(16).average().takeIf { !it.isNaN() } ?: 80.0
            while (c.players.size < minSize && free.isNotEmpty()) {
                // Eksik mevkiler: kaleci 3, savunma 8, orta saha 8, hucum 5
                val lines = c.players.map { world.players[it].bestPos().line }
                val need = when {
                    lines.count { it == Pos.Line.GK } < 3 -> Pos.Line.GK
                    lines.count { it == Pos.Line.DEF } < 8 -> Pos.Line.DEF
                    lines.count { it == Pos.Line.MID } < 7 -> Pos.Line.MID
                    else -> Pos.Line.ATT
                }
                val cand = free.filter { it.bestPos().line == need && it.currentAbility() <= level + 10 }
                    .maxByOrNull { it.currentAbility() - abs(it.currentAbility() - level).toInt() / 2 } ?: free.maxBy { it.currentAbility() }
                free.remove(cand)
                cand.club = c.id
                val age = world.age(cand) + 1
                cand.wage = FbEconomy.wage(cand.currentAbility(), age, c.reputation)
                cand.contractEnd = world.season + 1 + rnd.int(2)
                cand.squadNumber = (2..99).firstOrNull { n -> c.players.none { world.players[it].squadNumber == n } } ?: 99
                c.players.add(cand.id)
            }
        }
    }

    /** Avrupa disi milli havuzlari ve serbest oyuncu havuzunu yeni oyuncularla tazele. */
    private fun pools(world: FbWorld, rnd: FbRandom) {
        val gen = FbGen(world.seed xor (world.season * 3571L), world)
        val abroad = HashMap<Int, Int>()
        for (p in world.players) if (p.club == CLUB_ABROAD) abroad[p.nation] = (abroad[p.nation] ?: 0) + 1
        for ((ci, c) in FbCountries.all.withIndex()) {
            val hasLeague = world.leaguesOf(ci).isNotEmpty()
            if (hasLeague) continue
            val target = if (c.confed == Confed.UEFA) 30 else 34
            val missing = target - (abroad[ci] ?: 0)
            repeat(max(0, missing)) {
                val pa = (40f + c.level * 1.0f + rnd.gauss() * 14f + 12f).roundToInt().coerceIn(45, 195)
                val age = 17 + rnd.int(3)
                gen.makePlayer(ci, c, 0f, null, CLUB_ABROAD, fixedAge = age, fixedCa = (pa * 0.62f).roundToInt(), fixedPotential = pa)
            }
        }
        // Serbest havuz siniri: en umutsuzlar futbolu birakir
        val freeList = world.players.filter { it.club == CLUB_NONE }
        if (freeList.size > 1500) for (p in freeList.sortedBy { it.potential + it.currentAbility() }.take(freeList.size - 1500)) p.club = CLUB_RETIRED
        val freeCount = world.players.count { it.club == CLUB_NONE }
        repeat(max(0, 250 - freeCount)) {
            val ci = rnd.int(FbCountries.all.size)
            val p = gen.makePlayer(ci, FbCountries.all[ci], 70f + rnd.gauss() * 18f, null, CLUB_NONE)
            p.contractEnd = world.season
        }
    }

    val SPONSORS = listOf(
        "Nova Telekom", "Atlas Bank", "Orion Air", "Vega Motors", "Helix Energy", "Polaris Bet", "Zenit Foods", "Kestrel Tech",
        "Aurora Insurance", "Titan Steel", "Lumen Mobile", "Cobalt Cola", "Meridian Hotels", "Falcon Logistics", "Saga Games",
        "Nimbus Cloud", "Arcadia Travel", "Pioneer Oil", "Crest Pharma", "Quasar Electronics", "Delta Market", "Summit Water",
    )
}
