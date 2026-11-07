package com.eomer91.minigamesworld.ui.games.football.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** Mac motoru ayar katsayilari (testlerle kalibre edildi). */
object FbCal {
    /** Beceri farklarinin etkisi (1 = ham fark). Kucukse surpriz sonuclar artar. */
    @JvmField var comp = 0.62f
    /** Ileri pas temel basari (bolgeye gore) */
    @JvmField var fwdBase = floatArrayOf(1.6f, 1.0f, -0.6f, -3.5f, -1.0f)
    /** Aksiyon agirliklari */
    @JvmField var shotW = floatArrayOf(0f, 0f, 0.006f, 0.025f, 0.45f)
    @JvmField var throughBase = -1.6f
    @JvmField var xgScale = 1.0f
    @JvmField var dribbleBase = -0.6f
    @JvmField var crossBase = 0.2f
    @JvmField var headerBase = -1.8f
    @JvmField var recycleBase = 2.4f
    @JvmField var tempoSec = 6.8f
    @JvmField var foulMul = 0.55f
    /** Test sayaclari: aksiyon secimleri ve sut kaynaklari */
    @JvmField val debug = IntArray(32)
}

/** Mac motorunun kullandigi bilesik beceriler. */
enum class Skill {
    PASS_S, PASS_L, VISION, DRIBBLE, CROSS, SHOOT, LONG_SHOT, HEAD, PACE, TACKLE, MARK, INTERCEPT,
    PRESS, OFF_BALL, GK_SHOT, GK_AIR, COMPOSURE, STRENGTH, AGGRESSION, PENALTY, FREE_KICK, TEAMWORK,
}

enum class EvType {
    KICKOFF, PASS, LONG_BALL, DRIBBLE, THROUGH_BALL, CROSS, SHOT, GOAL, SAVE, MISS, BLOCK, TACKLE, INTERCEPT,
    FOUL, YELLOW, RED, INJURY, SUB, CORNER, FREE_KICK, PENALTY, OFFSIDE, HALF_TIME, FULL_TIME, EXTRA_TIME, SHOOTOUT_GOAL, SHOOTOUT_MISS,
}

/**
 * Mac olayi. [x] top konumu ev sahibinin kalesinden deplasmanin kalesine (0..1), [y] genislik (0..1).
 * [side] olayin takimi (0 ev, 1 deplasman), [player]/[other] FbPlayer id (-1 yok).
 */
class MatchEvent(val sec: Int, val type: EvType, val side: Int, val player: Int, val other: Int, val x: Float, val y: Float, val value: Float = 0f) {
    val minute: Int get() = sec / 60 + 1
}

class MatchPlayer(val p: FbPlayer, var pos: Pos) {
    val skill = FloatArray(Skill.entries.size)
    var condition = p.condition.toFloat()
    var yellow = 0
    var red = false
    var injured = false
    var injuryDays = 0
    var onPitch = false
    var minutes = 0
    var goals = 0
    var assists = 0
    var shots = 0
    var passes = 0
    var passesOk = 0
    var tackles = 0
    var saves = 0
    var keyPasses = 0
    var ratingAcc = 0f
    val rating: Float get() = (6.4f + ratingAcc).coerceIn(3f, 10f)

    /** Kondisyona gore anlik deger */
    fun s(k: Skill): Float = skill[k.ordinal] * (0.72f + 0.28f * condition / 100f)
}

class SideStats {
    var shots = 0
    var onTarget = 0
    var xg = 0f
    var possessionSec = 0
    var passes = 0
    var passesOk = 0
    var corners = 0
    var fouls = 0
    var yellows = 0
    var reds = 0
    var offsides = 0
    var tackles = 0
}

/** Bir takimin mac icindeki durumu. */
class MatchSide(val club: Int, val name: String, var tactics: Tactics, val home: Boolean) {
    /** Sahadakiler, dizilis sirasinda (atilan/sakatlanan oyuncu cikinca null olabilir) */
    val onPitch: Array<MatchPlayer?> = arrayOfNulls(11)
    val bench = ArrayList<MatchPlayer>()
    val used = ArrayList<MatchPlayer>()
    var subsLeft = 5
    var goals = 0
    val stats = SideStats()
    private var activeCache: List<MatchPlayer>? = null
    private val snapshot = arrayOfNulls<MatchPlayer>(11)
    /** Sahadaki oyuncular (onPitch degisince yeniden kurulur) */
    val active: List<MatchPlayer> get() {
        var same = activeCache != null
        if (same) for (i in 0 until 11) if (snapshot[i] !== onPitch[i]) { same = false; break }
        if (!same) { for (i in 0 until 11) snapshot[i] = onPitch[i]; activeCache = onPitch.filterNotNull() }
        return activeCache!!
    }
    fun all(): List<MatchPlayer> = onPitch.filterNotNull() + used + bench
}

/**
 * Adim adim mac simulasyonu. Her [step] bir top hareketi (pas, cali, sut...) uretir.
 * Hizli simulasyon [playToEnd], canli mac ekrani [step] kullanir; mac icinde taktik ve oyuncu degisebilir.
 */
class MatchSim(
    val home: MatchSide,
    val away: MatchSide,
    seed: Long,
    /** Berabere biterse uzatma + penaltilar (eleme maci) */
    val needWinner: Boolean = false,
    /** Iki ayakli eslesmede ilk macin ev sahibi lehine farki (ev sahibinin bakisiyla) */
    val aggregateOffset: Int = 0,
    /** Kucuk olaylari (pas, top kapma) kaydet: canli mac icin */
    val recordAll: Boolean = false,
    /** Kullanici takimi (yapay zeka degisiklik yapmasin): -1 yok, 0 ev, 1 deplasman */
    val userSide: Int = -1,
) {
    private val rnd = FbRandom(seed)
    val events = ArrayList<MatchEvent>()
    var sec = 0
        private set
    var period = 1
        private set
    var finished = false
        private set
    var shootout: Pair<Int, Int>? = null
        private set

    private var att = 0 // top kimde (0 ev, 1 dep)
    private var zone = 1 // hucum eden takimin bakisiyla 0 (kendi ceza sahasi) .. 4 (rakip ceza sahasi)
    private var lane = 1 // 0 sol, 1 orta, 2 sag
    private var holder: MatchPlayer? = null
    private var counterBoost = 0
    private var lastPasser: MatchPlayer? = null
    private var lastMinuteTick = 0
    private var periodEnd = 45 * 60
    private var stoppage = 0

    private val sides = arrayOf(home, away)

    init {
        for (s in sides) {
            for (mp in s.all()) prepare(mp, s)
            s.active.forEach { it.onPitch = true }
        }
        stoppage = rnd.range(0, 3) * 60
        kickoff(0)
    }

    // ------------------------------------------------------------------ hazirlik

    private fun prepare(mp: MatchPlayer, side: MatchSide) {
        val p = mp.p
        fun a(x: Attr) = p.attr(x)
        val sk = mp.skill
        sk[Skill.PASS_S.ordinal] = a(Attr.PASSING) * 0.5f + a(Attr.TECHNIQUE) * 0.2f + a(Attr.FIRST_TOUCH) * 0.15f + a(Attr.DECISIONS) * 0.15f
        sk[Skill.PASS_L.ordinal] = a(Attr.PASSING) * 0.5f + a(Attr.VISION) * 0.3f + a(Attr.TECHNIQUE) * 0.2f
        sk[Skill.VISION.ordinal] = a(Attr.VISION) * 0.55f + a(Attr.PASSING) * 0.25f + a(Attr.FLAIR) * 0.2f
        sk[Skill.DRIBBLE.ordinal] = a(Attr.DRIBBLING) * 0.45f + a(Attr.AGILITY) * 0.15f + a(Attr.ACCELERATION) * 0.15f + a(Attr.BALANCE) * 0.1f + a(Attr.TECHNIQUE) * 0.1f + a(Attr.FLAIR) * 0.05f
        sk[Skill.CROSS.ordinal] = a(Attr.CROSSING) * 0.75f + a(Attr.TECHNIQUE) * 0.25f
        sk[Skill.SHOOT.ordinal] = a(Attr.FINISHING) * 0.55f + a(Attr.COMPOSURE) * 0.25f + a(Attr.TECHNIQUE) * 0.1f + a(Attr.OFF_THE_BALL) * 0.1f
        sk[Skill.LONG_SHOT.ordinal] = a(Attr.LONG_SHOTS) * 0.7f + a(Attr.TECHNIQUE) * 0.3f
        sk[Skill.HEAD.ordinal] = a(Attr.HEADING) * 0.55f + a(Attr.JUMPING) * 0.3f + a(Attr.BRAVERY) * 0.15f + (p.heightCm - 180) * 0.12f
        sk[Skill.PACE.ordinal] = a(Attr.PACE) * 0.6f + a(Attr.ACCELERATION) * 0.4f
        sk[Skill.TACKLE.ordinal] = a(Attr.TACKLING) * 0.55f + a(Attr.ANTICIPATION) * 0.15f + a(Attr.POSITIONING) * 0.15f + a(Attr.STRENGTH) * 0.15f
        sk[Skill.MARK.ordinal] = a(Attr.MARKING) * 0.5f + a(Attr.POSITIONING) * 0.25f + a(Attr.CONCENTRATION) * 0.25f
        sk[Skill.INTERCEPT.ordinal] = a(Attr.ANTICIPATION) * 0.4f + a(Attr.POSITIONING) * 0.35f + a(Attr.DECISIONS) * 0.25f
        sk[Skill.PRESS.ordinal] = a(Attr.WORK_RATE) * 0.35f + a(Attr.STAMINA) * 0.25f + a(Attr.AGGRESSION) * 0.15f + a(Attr.TEAMWORK) * 0.15f + a(Attr.ACCELERATION) * 0.1f
        sk[Skill.OFF_BALL.ordinal] = a(Attr.OFF_THE_BALL) * 0.55f + a(Attr.ANTICIPATION) * 0.2f + a(Attr.ACCELERATION) * 0.25f
        sk[Skill.GK_SHOT.ordinal] = a(Attr.REFLEXES) * 0.4f + a(Attr.ONE_ON_ONES) * 0.25f + a(Attr.HANDLING) * 0.2f + a(Attr.POSITIONING) * 0.15f
        sk[Skill.GK_AIR.ordinal] = a(Attr.AERIAL_REACH) * 0.45f + a(Attr.COMMAND_OF_AREA) * 0.4f + a(Attr.HANDLING) * 0.15f
        sk[Skill.COMPOSURE.ordinal] = a(Attr.COMPOSURE) * 0.6f + a(Attr.DECISIONS) * 0.4f
        sk[Skill.STRENGTH.ordinal] = a(Attr.STRENGTH) * 0.7f + a(Attr.BALANCE) * 0.3f
        sk[Skill.AGGRESSION.ordinal] = a(Attr.AGGRESSION)
        sk[Skill.PENALTY.ordinal] = a(Attr.PENALTY) * 0.7f + a(Attr.COMPOSURE) * 0.3f
        sk[Skill.FREE_KICK.ordinal] = a(Attr.FREE_KICK) * 0.7f + a(Attr.TECHNIQUE) * 0.3f
        sk[Skill.TEAMWORK.ordinal] = a(Attr.TEAMWORK) * 0.5f + a(Attr.CONCENTRATION) * 0.5f

        // Mac gunu carpanlari: moral, form, mevki yatkinligi, istikrar, ic saha
        val fam = p.fam(mp.pos)
        val famMul = 0.62f + 0.38f * fam / 20f
        val morale = 0.94f + 0.12f * p.morale / 100f
        val form = 0.97f + 0.06f * ((p.form - 50) / 50f).coerceIn(-1f, 1f)
        val cons = p.hidden(Hidden.CONSISTENCY)
        val day = 1f + rnd.gauss() * (0.012f + (20 - cons) * 0.0025f)
        val homeAdv = if (side.home) 1.045f else 1f
        val mul = morale * form * day * homeAdv
        for (k in Skill.entries) {
            val positional = k != Skill.AGGRESSION && k != Skill.PENALTY && k != Skill.STRENGTH
            val v = sk[k.ordinal] * mul * (if (positional) famMul else 1f)
            // Beceri farklarini sikistir: futbolda en zayif takim da bazen kazanir
            sk[k.ordinal] = 11f + (v - 11f) * FbCal.comp
        }
    }

    // ------------------------------------------------------------------ yardimcilar

    private fun sig(x: Float) = 1f / (1f + exp(-x))
    private fun other(s: Int) = 1 - s
    private fun side(s: Int) = sides[s]
    private fun t(s: Int) = sides[s].tactics
    private fun numbers(s: Int) = side(s).active.count { !it.red }.toFloat() / 11f

    private fun log(type: EvType, s: Int, p: MatchPlayer?, o: MatchPlayer? = null, value: Float = 0f, minor: Boolean = false) {
        if (minor && !recordAll) return
        val (x, y) = ballXY()
        events.add(MatchEvent(sec, type, s, p?.p?.id ?: -1, o?.p?.id ?: -1, x, y, value))
    }

    /** Topun saha uzerindeki yaklasik konumu (ev sahibi bakisiyla). */
    private fun ballXY(): Pair<Float, Float> {
        val zx = floatArrayOf(0.06f, 0.27f, 0.5f, 0.73f, 0.93f)[zone] + (rnd.float() - 0.5f) * 0.08f
        val x = if (att == 0) zx else 1f - zx
        val ly = floatArrayOf(0.18f, 0.5f, 0.82f)[lane] + (rnd.float() - 0.5f) * 0.14f
        val y = if (att == 0) ly else 1f - ly
        return x.coerceIn(0.01f, 0.99f) to y.coerceIn(0.03f, 0.97f)
    }

    private fun lineWeight(mp: MatchPlayer, z: Int, ln: Int): Float {
        val line = mp.pos.line
        val w = when (z) {
            0 -> when (line) { Pos.Line.GK -> 0.6f; Pos.Line.DEF -> 1f; Pos.Line.MID -> 0.3f; Pos.Line.ATT -> 0.02f }
            1 -> when (line) { Pos.Line.GK -> 0.08f; Pos.Line.DEF -> 1f; Pos.Line.MID -> 0.9f; Pos.Line.ATT -> 0.1f }
            2 -> when (line) { Pos.Line.GK -> 0f; Pos.Line.DEF -> 0.35f; Pos.Line.MID -> 1f; Pos.Line.ATT -> 0.5f }
            3 -> when (line) { Pos.Line.GK -> 0f; Pos.Line.DEF -> 0.15f; Pos.Line.MID -> 0.8f; Pos.Line.ATT -> 1f }
            else -> when (line) { Pos.Line.GK -> 0f; Pos.Line.DEF -> 0.08f; Pos.Line.MID -> 0.5f; Pos.Line.ATT -> 1f }
        }
        val lw = laneOf(mp.pos)
        val laneMul = if (lw == ln) 1.6f else if (lw == 1 || ln == 1) 0.8f else 0.35f
        return w * laneMul
    }

    private fun laneOf(p: Pos): Int = when (p) {
        Pos.DL, Pos.WBL, Pos.ML, Pos.AML -> 0
        Pos.DR, Pos.WBR, Pos.MR, Pos.AMR -> 2
        else -> 1
    }

    private fun pickPlayer(s: Int, z: Int, ln: Int, exclude: MatchPlayer? = null, extra: ((MatchPlayer) -> Float)? = null): MatchPlayer {
        val list = side(s).active.filter { it !== exclude && !it.red }
        val ws = list.map { mp -> lineWeight(mp, z, ln) * (extra?.invoke(mp) ?: 1f) + 0.001f }
        var r = rnd.float() * ws.sum()
        for (i in list.indices) { r -= ws[i]; if (r <= 0f) return list[i] }
        return list.last()
    }

    /** Savunan takimdan, hucum bolgesine gore uygun oyuncu */
    private fun defenderFor(z: Int, ln: Int): MatchPlayer {
        val dz = 4 - z
        val dl = 2 - ln
        return pickPlayer(other(att), dz, dl)
    }

    private fun gk(s: Int): MatchPlayer? = side(s).active.firstOrNull { it.pos == Pos.GK } ?: side(s).active.minByOrNull { it.s(Skill.GK_SHOT) * -1 }

    private fun teamAvg(s: Int, k: Skill, line: Pos.Line? = null): Float {
        val l = side(s).active.filter { line == null || it.pos.line == line }
        return if (l.isEmpty()) 8f else l.sumOf { it.s(k).toDouble() }.toFloat() / l.size
    }

    // ------------------------------------------------------------------ akis

    private fun kickoff(s: Int) {
        att = s; zone = 2; lane = 1; counterBoost = 0
        holder = pickPlayer(s, 2, 1)
        log(EvType.KICKOFF, s, holder, minor = true)
    }

    private fun turnover(winner: MatchPlayer?, newZone: Int) {
        val loserTactics = t(att)
        att = other(att)
        zone = newZone.coerceIn(0, 4)
        lane = 2 - lane
        holder = winner ?: pickPlayer(att, zone, lane)
        lastPasser = null
        // Kontra atak: topu kendi yari sahasinda kazanip rakip ileri cikmissa
        val tac = t(att)
        val exposed = loserTactics.mentality.risk >= 1f || loserTactics.defLine == DefLine.HIGH
        counterBoost = when {
            zone <= 2 && tac.counter && exposed && rnd.chance(0.7f) -> 2
            zone <= 2 && tac.counter && rnd.chance(0.3f) -> 1
            zone <= 2 && exposed && rnd.chance(0.2f) -> 1
            else -> 0
        }
    }

    fun playToEnd(): MatchSim { while (!finished) step(); return this }

    /** Bir aksiyon ilerlet. */
    fun step() {
        if (finished) return
        val h = holder ?: pickPlayer(att, zone, lane).also { holder = it }
        val z0 = zone; val a0 = att
        val a = att
        val d = other(a)
        val ta = t(a)
        val td = t(d)
        val leading = side(a).goals - side(d).goals + (if (a == 0) aggregateOffset else -aggregateOffset) > 0
        val wasting = ta.timeWasting && leading && sec > 60 * 60

        // --- aksiyon secimi
        val risk = ta.mentality.risk
        val passMul = when (ta.passing) { PassingStyle.SHORT -> 1.3f; PassingStyle.MIXED -> 1f; PassingStyle.DIRECT -> 0.55f }
        val tempoMul = when (ta.tempo) { Tempo.SLOW -> 1.2f; Tempo.NORMAL -> 1f; Tempo.FAST -> 0.7f }
        var wRecycle = floatArrayOf(0f, 0.7f, 0.8f, 0.6f, 0.3f)[zone] * passMul * tempoMul * (1f - risk * 0.15f) * (if (wasting) 2.2f else 1f)
        var wForward = floatArrayOf(1f, 1f, 1f, 0.55f, 0.15f)[zone] * (if (ta.passing == PassingStyle.SHORT) 1.15f else 1f) * (1f + risk * 0.08f)
        val wLong = floatArrayOf(0.5f, 0.4f, 0.15f, 0f, 0f)[zone] * when (ta.passing) { PassingStyle.SHORT -> 0.25f; PassingStyle.MIXED -> 1f; PassingStyle.DIRECT -> 2.4f }
        val wDribble = floatArrayOf(0f, 0.025f, 0.06f, 0.11f, 0.1f)[zone] * (h.s(Skill.DRIBBLE) / 11f)
        var wThrough = floatArrayOf(0f, 0f, 0.035f, 0.07f, 0f)[zone] * (h.s(Skill.VISION) / 11f) *
            when (ta.focus) { AttackFocus.CENTER -> 1.35f; AttackFocus.BALANCED -> 1f; AttackFocus.FLANKS -> 0.7f }
        val wideLane = lane != 1
        val wCross = if (zone == 3 && wideLane) 0.55f * when (ta.focus) { AttackFocus.FLANKS -> 1.6f; AttackFocus.BALANCED -> 1f; AttackFocus.CENTER -> 0.5f } *
            when (ta.width) { Width.WIDE -> 1.35f; Width.NORMAL -> 1f; Width.NARROW -> 0.6f } * (h.s(Skill.CROSS) / 11f) else 0f
        val wShot = FbCal.shotW[zone] * (1f + risk * 0.12f) * (if (zone < 4) h.s(Skill.LONG_SHOT) / 11f else 1f)
        if (counterBoost > 0) { wRecycle *= 0.2f; wForward *= 1.3f; wThrough *= 1.5f }
        else if (ta.counter) { wForward *= 0.9f; wThrough *= 0.85f } // kontra icin geride bekleyen oyuncular
        if (wasting) wForward *= 0.6f
        val ws = floatArrayOf(wRecycle, wForward, wLong, wDribble, wThrough, wCross, wShot)
        var r = rnd.float() * ws.sum()
        var choice = 0
        for (i in ws.indices) { r -= ws[i]; if (r <= 0f) { choice = i; break } }
        FbCal.debug[choice]++

        // --- sure
        val baseT = FbCal.tempoSec * when (ta.tempo) { Tempo.SLOW -> 1.12f; Tempo.NORMAL -> 1f; Tempo.FAST -> 0.82f }
        var dt = baseT * (0.6f + rnd.float() * 0.8f) * (if (choice == 0) 1.35f else 1f) * (if (wasting) 1.6f else 1f) * (if (counterBoost > 0) 0.6f else 1f)
        if (wasting && rnd.chance(0.006f)) card(h, a, yellowOnly = true)

        val num = numbers(a) / numbers(d) // kirmizi kartlarin etkisi
        val numAdj = (num - 1f) * 3f
        val boost = if (counterBoost > 0) 0.45f else 0f
        // Pres: savunan takimin baskisi hucumcunun kendi yari sahasinda etkili
        val pressLvl = when (td.pressing) { Pressing.LOW -> -0.7f; Pressing.MEDIUM -> 0f; Pressing.HIGH -> 1f }
        val pressEff = if (zone <= 2) pressLvl * (teamAvg(d, Skill.PRESS) / 11f) * (1.15f - h.s(Skill.COMPOSURE) / 40f) else 0f
        val compact = when (td.defLine) { DefLine.HIGH -> 0.35f; DefLine.NORMAL -> 0f; DefLine.DEEP -> -0.2f } // orta sahada alan daralmasi
        val tempoErr = when (ta.tempo) { Tempo.SLOW -> 0.15f; Tempo.NORMAL -> 0f; Tempo.FAST -> -0.25f }
        val passAcc = when (ta.passing) { PassingStyle.SHORT -> 0.22f; PassingStyle.MIXED -> 0f; PassingStyle.DIRECT -> -0.15f } // kisa pas daha isabetli

        when (choice) {
            0 -> { // geri/yana pas, topu tutma
                val def = defenderFor(zone, lane)
                val p = sig(FbCal.recycleBase + (h.s(Skill.PASS_S) - def.s(Skill.PRESS) * 0.6f) / 5f - pressEff * 0.9f + tempoErr + numAdj + boost)
                h.passes++; side(a).stats.passes++
                if (rnd.float() < p) {
                    h.passesOk++; side(a).stats.passesOk++
                    if (zone == 4) zone = 3 else if (zone > 0 && rnd.chance(0.3f)) zone--
                    lane = rnd.int(3)
                    lastPasser = h
                    holder = pickPlayer(a, zone, lane, exclude = h)
                    log(EvType.PASS, a, h, holder, minor = true)
                } else if (!duelFoul(def, d, h, a, 0.05f)) {
                    def.tackles++; def.ratingAcc += 0.04f
                    log(EvType.INTERCEPT, d, def, h, minor = true)
                    turnover(def, 4 - zone)
                }
            }
            1 -> { // ileri pas
                val nz = min(4, zone + 1)
                val recv = pickPlayer(a, nz, lane, exclude = h) { it.s(Skill.OFF_BALL) / 10f }
                val def = defenderFor(nz, lane)
                val baseZ = FbCal.fwdBase[zone]
                val defBlock = if (nz == 4) when (td.defLine) { DefLine.DEEP -> 0.35f; else -> 0f } + td.mentality.risk * -0.15f else 0f
                val p = sig(baseZ + (h.s(Skill.PASS_S) * 0.6f + recv.s(Skill.OFF_BALL) * 0.25f - def.s(Skill.INTERCEPT) * 0.5f - def.s(Skill.MARK) * 0.25f) / 3.2f -
                    pressEff - (if (nz in 2..3) compact else 0f) - defBlock + tempoErr + passAcc + numAdj + boost - marking(d, recv))
                h.passes++; side(a).stats.passes++
                if (rnd.float() < p) {
                    h.passesOk++; side(a).stats.passesOk++
                    lastPasser = h
                    if (nz >= 3) h.ratingAcc += 0.03f
                    zone = nz
                    holder = recv
                    if (counterBoost > 0) counterBoost--
                    log(EvType.PASS, a, h, recv, minor = true)
                } else if (!duelFoul(def, d, recv, a, 0.11f)) {
                    def.ratingAcc += 0.05f; def.tackles++
                    log(EvType.INTERCEPT, d, def, h, minor = true)
                    turnover(def, 4 - zone)
                }
            }
            2 -> { // uzun top
                val target = pickPlayer(a, min(4, zone + 2), lane, exclude = h) { if (it.pos.line == Pos.Line.ATT) 2f + it.s(Skill.HEAD) / 10f else 0.4f }
                val def = defenderFor(min(4, zone + 2), lane)
                h.passes++; side(a).stats.passes++
                // Yuksek savunma hattinin arkasina: hiz yarisi
                val overTop = td.defLine == DefLine.HIGH && rnd.chance(0.2f)
                if (overTop) {
                    val p = sig(-0.4f + (target.s(Skill.PACE) - def.s(Skill.PACE)) / 3f + (h.s(Skill.PASS_L) - 11f) / 6f)
                    if (offside(d, target)) return finishStep(dt)
                    if (rnd.float() < p) {
                        h.passesOk++; side(a).stats.passesOk++; lastPasser = h
                        zone = 4; lane = 1; holder = target
                        log(EvType.LONG_BALL, a, h, target, minor = true)
                        shoot(target, a, xgBase = 0.28f, oneOnOne = true)
                    } else {
                        log(EvType.INTERCEPT, d, def, target, minor = true)
                        turnover(def, 4 - min(4, zone + 2))
                    }
                } else {
                    val p = sig(-0.25f + ((target.s(Skill.HEAD) + target.s(Skill.STRENGTH)) - (def.s(Skill.HEAD) + def.s(Skill.STRENGTH))) / 6f + (h.s(Skill.PASS_L) - 11f) / 7f + numAdj + boost)
                    if (rnd.float() < p) {
                        h.passesOk++; side(a).stats.passesOk++; lastPasser = h
                        zone = min(3, zone + 2)
                        holder = target
                        log(EvType.LONG_BALL, a, h, target, minor = true)
                    } else if (!duelFoul(def, d, target, a, 0.14f)) {
                        def.ratingAcc += 0.04f
                        log(EvType.INTERCEPT, d, def, target, minor = true)
                        turnover(def, 4 - min(4, zone + 2))
                    }
                }
            }
            3 -> { // cali
                val def = defenderFor(zone, lane)
                val hard = if (td.hardTackling) 0.35f else 0f
                val p = sig(FbCal.dribbleBase + (h.s(Skill.DRIBBLE) - def.s(Skill.TACKLE) - hard * 3f) / 3.5f + numAdj + boost - marking(d, h))
                if (rnd.float() < p) {
                    h.ratingAcc += 0.04f
                    log(EvType.DRIBBLE, a, h, def, minor = true)
                    if (zone == 4) shoot(h, a, xgBase = 0.16f) else zone++
                } else {
                    val foulP = 0.3f * FbCal.foulMul * (if (td.hardTackling) 1.6f else 1f) * (def.s(Skill.AGGRESSION) / 10f)
                    if (rnd.float() < foulP) foul(def, d, h, a) else {
                        def.tackles++; def.ratingAcc += 0.07f; side(d).stats.tackles++
                        log(EvType.TACKLE, d, def, h, minor = true)
                        turnover(def, 4 - zone)
                    }
                }
            }
            4 -> { // ara pasi
                val runner = pickPlayer(a, 4, 1, exclude = h) { if (it.pos.line == Pos.Line.ATT) 2.5f * it.s(Skill.OFF_BALL) / 10f else 0.3f }
                val def = defenderFor(4, 1)
                val lineAdj = when (td.defLine) { DefLine.HIGH -> 0.3f; DefLine.NORMAL -> 0f; DefLine.DEEP -> -0.6f }
                val p = sig(FbCal.throughBase + ((h.s(Skill.VISION) + h.s(Skill.PASS_S)) / 2f - (def.s(Skill.MARK) + def.s(Skill.INTERCEPT)) / 2f) / 3f +
                    (runner.s(Skill.PACE) - def.s(Skill.PACE)) / 6f + lineAdj + numAdj + boost * 1.2f)
                h.passes++; side(a).stats.passes++
                if (offside(d, runner)) return finishStep(dt)
                if (rnd.float() < p) {
                    h.passesOk++; side(a).stats.passesOk++; h.keyPasses++; h.ratingAcc += 0.08f
                    lastPasser = h
                    zone = 4; lane = 1; holder = runner
                    log(EvType.THROUGH_BALL, a, h, runner, minor = true)
                    shoot(runner, a, xgBase = 0.30f, oneOnOne = true)
                } else {
                    def.ratingAcc += 0.05f
                    log(EvType.INTERCEPT, d, def, h, minor = true)
                    turnover(def, 4 - zone)
                }
            }
            5 -> { // orta
                val fb = defenderFor(3, lane)
                val pd = sig(FbCal.crossBase + (h.s(Skill.CROSS) - fb.s(Skill.MARK) * 0.7f - fb.s(Skill.PRESS) * 0.3f) / 4f + numAdj)
                log(EvType.CROSS, a, h, minor = true)
                if (rnd.float() < pd) {
                    val target = pickPlayer(a, 4, 1, exclude = h) { mp -> if (mp.pos.line == Pos.Line.ATT || mp.pos == Pos.DC) 1.5f * mp.s(Skill.HEAD) / 10f else 0.3f }
                    val cb = pickPlayer(d, 0, 1) { mp -> if (mp.pos == Pos.DC) 2f else if (mp.pos.line == Pos.Line.DEF) 0.6f else 0.1f }
                    val keeper = gk(d)
                    val pw = sig((target.s(Skill.HEAD) - cb.s(Skill.HEAD)) / 3.5f - ((keeper?.s(Skill.GK_AIR) ?: 10f) - 11f) / 9f + FbCal.headerBase)
                    if (rnd.float() < pw) {
                        lastPasser = h; h.keyPasses++
                        zone = 4; lane = 1; holder = target
                        shoot(target, a, xgBase = 0.11f, header = true)
                    } else {
                        cb.ratingAcc += 0.06f
                        if (rnd.chance(0.2f)) corner(a) else turnover(cb, 0)
                    }
                } else {
                    fb.ratingAcc += 0.04f
                    if (rnd.chance(0.14f)) corner(a) else turnover(fb, 1)
                }
            }
            else -> { // sut
                val xg = when (zone) {
                    2 -> 0.025f
                    3 -> 0.05f
                    else -> 0.105f + (h.s(Skill.OFF_BALL) - 10f) * 0.004f
                }
                shoot(h, a, xgBase = xg, longRange = zone < 4)
            }
        }
        if (zone == 4 && (z0 < 4 || a0 != att)) FbCal.debug[20]++
        finishStep(dt)
    }

    /** Ikili mucadelede faul: top kaybi yerine serbest vurus. */
    private fun duelFoul(def: MatchPlayer, d: Int, victim: MatchPlayer, a: Int, base: Float): Boolean {
        val p = base * FbCal.foulMul * (if (t(d).hardTackling) 1.5f else 1f) * (0.55f + def.s(Skill.AGGRESSION) / 22f) *
            (if (t(d).pressing == Pressing.HIGH && zone <= 2) 1.25f else 1f)
        if (rnd.float() < p) { foul(def, d, victim, a); return true }
        return false
    }

    /** Adam adama savunmada, iyi markajli savunmaci hedef oyuncuyu zorlar; hareketli oyuncular kurtulur. */
    private fun marking(d: Int, target: MatchPlayer): Float {
        if (t(d).marking != Marking.MAN) return 0f
        val mk = teamAvg(d, Skill.MARK, Pos.Line.DEF)
        return ((mk - target.s(Skill.OFF_BALL)) / 10f).coerceIn(-0.4f, 0.5f)
    }

    private fun offside(d: Int, runner: MatchPlayer): Boolean {
        val trap = t(d).offsideTrap
        val tw = teamAvg(d, Skill.TEAMWORK, Pos.Line.DEF)
        val pOff = 0.07f + (if (trap) 0.12f + (tw - 10f) * 0.02f else 0f) - (runner.s(Skill.OFF_BALL) - 10f) * 0.006f
        if (rnd.float() < pOff) {
            side(att).stats.offsides++
            log(EvType.OFFSIDE, att, runner)
            turnover(null, 1)
            return true
        }
        return false
    }

    private fun shoot(shooter: MatchPlayer, a: Int, xgBase: Float, oneOnOne: Boolean = false, header: Boolean = false, longRange: Boolean = false, setPiece: Boolean = false) {
        val d = other(a)
        val keeper = gk(d)
        val skill = when {
            header -> shooter.s(Skill.HEAD)
            longRange -> shooter.s(Skill.LONG_SHOT)
            else -> shooter.s(Skill.SHOOT)
        }
        val gks = keeper?.s(Skill.GK_SHOT) ?: 5f
        var xg = xgBase * FbCal.xgScale * exp((skill - 11f) / 11f)
        if (oneOnOne) xg *= exp((shooter.s(Skill.COMPOSURE) - gks) / 14f)
        xg = xg.coerceIn(0.01f, 0.85f)
        shooter.shots++
        FbCal.debug[10 + when { oneOnOne -> 0; header && setPiece -> 1; header -> 2; setPiece -> 3; longRange -> 4; else -> 5 }]++
        val st = side(a).stats
        st.shots++; st.xg += xg
        val pGoal = (xg * exp((11f - gks) / 9f)).coerceIn(0.005f, 0.9f)
        val r = rnd.float()
        val pBlock = if (longRange) 0.28f else if (oneOnOne || header) 0.05f else 0.22f
        val pSave = 0.32f
        when {
            r < pGoal -> {
                st.onTarget++
                goal(shooter, a)
            }
            r < pGoal + pBlock * (1f - pGoal) -> {
                val blocker = pickPlayer(d, 0, 1)
                blocker.ratingAcc += 0.06f
                log(EvType.BLOCK, d, blocker, shooter)
                shooter.ratingAcc -= 0.02f
                if (rnd.chance(0.22f)) corner(a) else turnover(blocker, 0)
            }
            r < pGoal + (pBlock + pSave) * (1f - pGoal) -> {
                st.onTarget++
                keeper?.let { it.saves++; it.ratingAcc += 0.12f + xg * 0.8f }
                log(EvType.SAVE, d, keeper, shooter, value = xg)
                if (rnd.chance(0.16f)) corner(a) else turnover(keeper, 0)
            }
            else -> {
                shooter.ratingAcc -= 0.04f
                log(EvType.MISS, a, shooter, value = xg)
                turnover(keeper, 0)
            }
        }
    }

    private fun goal(scorer: MatchPlayer, a: Int) {
        side(a).goals++
        scorer.goals++; scorer.ratingAcc += 0.9f
        val assist = lastPasser?.takeIf { it !== scorer && it.onPitch }
        assist?.let { it.assists++; it.ratingAcc += 0.5f }
        val d = other(a)
        for (mp in side(d).active) if (mp.pos.line == Pos.Line.DEF || mp.pos == Pos.GK) mp.ratingAcc -= if (mp.pos == Pos.GK) 0.35f else 0.18f
        for (mp in side(a).active) mp.ratingAcc += 0.05f
        log(EvType.GOAL, a, scorer, assist)
        kickoff(d)
    }

    private fun corner(a: Int) {
        side(a).stats.corners++
        log(EvType.CORNER, a, holder)
        val taker = side(a).active.maxBy { it.p.attr(Attr.CORNERS) }
        val target = pickPlayer(a, 4, 1, exclude = taker) { mp -> 0.2f + mp.s(Skill.HEAD) / 8f }
        val defender = pickPlayer(other(a), 0, 1) { mp -> 0.2f + mp.s(Skill.HEAD) / 8f }
        val keeper = gk(other(a))
        val p = sig((target.s(Skill.HEAD) - defender.s(Skill.HEAD)) / 3.5f + (taker.p.attr(Attr.CORNERS) - 11f) / 8f - ((keeper?.s(Skill.GK_AIR) ?: 10f) - 11f) / 9f - 1.0f)
        zone = 4; lane = 1
        if (rnd.float() < p) {
            lastPasser = taker
            shoot(target, a, xgBase = 0.085f, header = true, setPiece = true)
        } else {
            defender.ratingAcc += 0.03f
            att = a
            turnover(defender, 0)
        }
    }

    private fun foul(fouler: MatchPlayer, d: Int, victim: MatchPlayer, a: Int) {
        side(d).stats.fouls++
        fouler.ratingAcc -= 0.05f
        log(EvType.FOUL, d, fouler, victim)
        // Kart
        val yellowP = 0.15f * (if (t(d).hardTackling) 1.4f else 1f) * (0.6f + fouler.s(Skill.AGGRESSION) / 25f) * (if (zone >= 3) 1.2f else 1f)
        if (rnd.float() < 0.006f) card(fouler, d, straightRed = true)
        else if (rnd.float() < yellowP) card(fouler, d)
        // Sakatlik
        val injP = 0.022f * (0.5f + victim.p.hidden(Hidden.INJURY_PRONENESS) / 14f) * (if (t(d).hardTackling) 1.5f else 1f)
        if (rnd.float() < injP) injure(victim, a)
        // Serbest vurus / penalti
        when {
            zone == 4 && rnd.chance(0.45f) -> penalty(a)
            zone >= 3 -> {
                val taker = side(a).active.maxBy { it.s(Skill.FREE_KICK) }
                log(EvType.FREE_KICK, a, taker)
                if (rnd.chance(0.4f)) shoot(taker, a, xgBase = 0.055f * taker.s(Skill.FREE_KICK) / 12f, longRange = true, setPiece = true)
                else { zone = 3; holder = taker }
            }
            else -> { holder = victim }
        }
    }

    private fun penalty(a: Int) {
        val taker = side(a).active.maxBy { it.s(Skill.PENALTY) }
        log(EvType.PENALTY, a, taker)
        val keeper = gk(other(a))
        val p = (0.76f + (taker.s(Skill.PENALTY) - 12f) * 0.012f - ((keeper?.s(Skill.GK_SHOT) ?: 10f) - 12f) * 0.008f).coerceIn(0.55f, 0.92f)
        side(a).stats.shots++; side(a).stats.xg += 0.76f; taker.shots++
        if (rnd.float() < p) { side(a).stats.onTarget++; lastPasser = null; goal(taker, a) }
        else {
            keeper?.let { it.saves++; it.ratingAcc += 0.5f }
            taker.ratingAcc -= 0.4f
            log(EvType.SAVE, other(a), keeper, taker, value = 0.76f)
            turnover(keeper, 0)
        }
    }

    private fun card(mp: MatchPlayer, s: Int, straightRed: Boolean = false, @Suppress("UNUSED_PARAMETER") yellowOnly: Boolean = false) {
        if (!straightRed) {
            mp.yellow++
            side(s).stats.yellows++
            mp.ratingAcc -= 0.25f
            log(EvType.YELLOW, s, mp)
            if (mp.yellow >= 2) sendOff(mp, s)
        } else sendOff(mp, s)
    }

    private fun sendOff(mp: MatchPlayer, s: Int) {
        mp.red = true; mp.onPitch = false
        mp.ratingAcc -= 1.2f
        side(s).stats.reds++
        log(EvType.RED, s, mp)
        val idx = side(s).onPitch.indexOf(mp)
        if (idx >= 0) { side(s).onPitch[idx] = null; side(s).used.add(mp) }
        if (holder === mp) holder = null
    }

    private fun injure(mp: MatchPlayer, s: Int) {
        if (mp.injured) return
        mp.injured = true
        val prone = mp.p.hidden(Hidden.INJURY_PRONENESS)
        mp.injuryDays = (3 + rnd.int(10) + if (rnd.chance(0.25f + prone / 80f)) rnd.int(40) else 0)
        mp.condition = min(mp.condition, 40f)
        log(EvType.INJURY, s, mp, value = mp.injuryDays.toFloat())
        autoSub(s, force = mp) // sakatlanan oyuncu (kullanici takiminda da) hemen degisir
    }

    // ------------------------------------------------------------------ zaman

    private fun finishStep(dt: Float) {
        val add = max(2, dt.toInt())
        side(att).stats.possessionSec += add
        sec += add
        // Dakikalik isler: yorgunluk, oyuncu degisikligi
        while (sec / 60 > lastMinuteTick) {
            lastMinuteTick++
            for (s in 0..1) fatigue(s)
            if (lastMinuteTick >= 55) for (s in 0..1) if (s != userSide) autoSub(s)
        }
        if (sec >= periodEnd + stoppage) endPeriod()
    }

    private fun fatigue(s: Int) {
        val tc = t(s)
        val tempo = when (tc.tempo) { Tempo.SLOW -> 0.88f; Tempo.NORMAL -> 1f; Tempo.FAST -> 1.15f }
        val press = when (tc.pressing) { Pressing.LOW -> 0.88f; Pressing.MEDIUM -> 1f; Pressing.HIGH -> 1.22f }
        for (mp in side(s).active) {
            mp.minutes++
            val stam = mp.p.attr(Attr.STAMINA)
            val nf = mp.p.attr(Attr.NATURAL_FITNESS)
            val rate = 0.36f * tempo * press * (1.45f - stam / 20f * 0.75f) * (1.25f - nf / 20f * 0.4f) * (if (mp.pos == Pos.GK) 0.3f else 1f)
            mp.condition = (mp.condition - rate).coerceAtLeast(20f)
        }
    }

    /** Yapay zeka: yorgun/sakat oyuncuyu degistir. */
    private fun autoSub(s: Int, force: MatchPlayer? = null) {
        val side = side(s)
        if (side.subsLeft <= 0 || side.bench.isEmpty()) return
        val out = force ?: run {
            if (lastMinuteTick < 58 || rnd.float() > 0.08f) return
            side.active.filter { it.pos != Pos.GK }.minByOrNull { it.condition }?.takeIf { it.condition < 68f || (lastMinuteTick > 75 && it.rating < 6.2f) } ?: return
        }
        val best = side.bench.filter { !it.injured && (out.pos == Pos.GK) == (it.p.bestPos() == Pos.GK) }
            .maxByOrNull { it.p.posRating(out.pos) * (0.6f + 0.4f * it.p.fam(out.pos) / 20f) } ?: return
        substitute(s, out, best)
    }

    /** Oyuncu degisikligi (kullanici veya yapay zeka). */
    fun substitute(s: Int, out: MatchPlayer, inn: MatchPlayer): Boolean {
        val side = side(s)
        if (side.subsLeft <= 0 || !side.bench.contains(inn)) return false
        val idx = side.onPitch.indexOf(out)
        if (idx < 0) return false
        inn.pos = out.pos
        prepare(inn, side)
        side.onPitch[idx] = inn
        inn.onPitch = true
        out.onPitch = false
        side.bench.remove(inn)
        side.used.add(out)
        side.subsLeft--
        if (holder === out) holder = inn
        log(EvType.SUB, s, inn, out)
        return true
    }

    fun setTactics(s: Int, tc: Tactics) { side(s).tactics = tc }

    private fun endPeriod() {
        when (period) {
            1 -> {
                log(EvType.HALF_TIME, -1, null)
                period = 2; periodEnd = 90 * 60; stoppage = rnd.range(1, 5) * 60
                sec = 45 * 60; lastMinuteTick = 45
                for (s in sides) for (mp in s.active) mp.condition = min(100f, mp.condition + 6f)
                kickoff(1)
            }
            2 -> {
                val diff = home.goals - away.goals + aggregateOffset
                if (needWinner && diff == 0) {
                    log(EvType.EXTRA_TIME, -1, null)
                    period = 3; periodEnd = 105 * 60; stoppage = 60; sec = 90 * 60; lastMinuteTick = 90
                    kickoff(0)
                } else end()
            }
            3 -> { period = 4; periodEnd = 120 * 60; stoppage = 60; sec = 105 * 60; lastMinuteTick = 105; kickoff(1) }
            else -> {
                val diff = home.goals - away.goals + aggregateOffset
                if (diff == 0) penaltyShootout()
                end()
            }
        }
    }

    private fun penaltyShootout() {
        var h = 0; var a = 0
        var kh = 0; var ka = 0
        val takers = arrayOf(home.active.sortedByDescending { it.s(Skill.PENALTY) }, away.active.sortedByDescending { it.s(Skill.PENALTY) })
        while (true) {
            for (s in 0..1) {
                val k = if (s == 0) kh else ka
                val tk = takers[s][k % takers[s].size]
                val keeper = gk(1 - s)
                val p = (0.75f + (tk.s(Skill.PENALTY) - 12f) * 0.012f - ((keeper?.s(Skill.GK_SHOT) ?: 10f) - 12f) * 0.008f).coerceIn(0.5f, 0.92f)
                val ok = rnd.float() < p
                if (s == 0) { kh++; if (ok) h++ } else { ka++; if (ok) a++ }
                log(if (ok) EvType.SHOOTOUT_GOAL else EvType.SHOOTOUT_MISS, s, tk)
                if (kh <= 5 && ka <= 5) {
                    // Ilk 5 atista: biri yetisemeyecekse biter
                    if (h + (5 - kh) < a || a + (5 - ka) < h) { shootout = h to a; return }
                }
            }
            if (kh >= 5 && kh == ka && h != a) { shootout = h to a; return }
        }
    }

    private fun end() {
        finished = true
        log(EvType.FULL_TIME, -1, null)
        // Mac sonucu puan duzeltmesi
        val gd = home.goals - away.goals
        for ((i, s) in sides.withIndex()) {
            val res = if (i == 0) gd else -gd
            for (mp in s.all()) if (mp.minutes > 0) mp.ratingAcc += (res.coerceIn(-3, 3) * 0.12f)
        }
    }

    // ------------------------------------------------------------------ sonuc

    /** Oyuncu oynadiysa mac puani */
    fun ratingOf(mp: MatchPlayer): Float? = if (mp.minutes > 0) mp.rating else null

    val homeGoals get() = home.goals
    val awayGoals get() = away.goals
    val minute get() = sec / 60
}
