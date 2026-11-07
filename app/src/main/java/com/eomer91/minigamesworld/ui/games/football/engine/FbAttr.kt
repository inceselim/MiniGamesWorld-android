package com.eomer91.minigamesworld.ui.games.football.engine

/**
 * Oyuncu yetenekleri (FM tarzi 1-20). Bellekte x10 tutulur (10..200) ki antrenmanla
 * kucuk adimlarla gelisebilsin.
 */
enum class AttrGroup { TECHNICAL, MENTAL, PHYSICAL, GOALKEEPING }

enum class Attr(val group: AttrGroup) {
    // Teknik
    CORNERS(AttrGroup.TECHNICAL), CROSSING(AttrGroup.TECHNICAL), DRIBBLING(AttrGroup.TECHNICAL),
    FINISHING(AttrGroup.TECHNICAL), FIRST_TOUCH(AttrGroup.TECHNICAL), FREE_KICK(AttrGroup.TECHNICAL),
    HEADING(AttrGroup.TECHNICAL), LONG_SHOTS(AttrGroup.TECHNICAL), MARKING(AttrGroup.TECHNICAL),
    PASSING(AttrGroup.TECHNICAL), PENALTY(AttrGroup.TECHNICAL), TACKLING(AttrGroup.TECHNICAL),
    TECHNIQUE(AttrGroup.TECHNICAL),
    // Zihinsel
    AGGRESSION(AttrGroup.MENTAL), ANTICIPATION(AttrGroup.MENTAL), BRAVERY(AttrGroup.MENTAL),
    COMPOSURE(AttrGroup.MENTAL), CONCENTRATION(AttrGroup.MENTAL), DECISIONS(AttrGroup.MENTAL),
    DETERMINATION(AttrGroup.MENTAL), FLAIR(AttrGroup.MENTAL), LEADERSHIP(AttrGroup.MENTAL),
    OFF_THE_BALL(AttrGroup.MENTAL), POSITIONING(AttrGroup.MENTAL), TEAMWORK(AttrGroup.MENTAL),
    VISION(AttrGroup.MENTAL), WORK_RATE(AttrGroup.MENTAL),
    // Fiziksel
    ACCELERATION(AttrGroup.PHYSICAL), AGILITY(AttrGroup.PHYSICAL), BALANCE(AttrGroup.PHYSICAL),
    JUMPING(AttrGroup.PHYSICAL), NATURAL_FITNESS(AttrGroup.PHYSICAL), PACE(AttrGroup.PHYSICAL),
    STAMINA(AttrGroup.PHYSICAL), STRENGTH(AttrGroup.PHYSICAL),
    // Kaleci
    AERIAL_REACH(AttrGroup.GOALKEEPING), COMMAND_OF_AREA(AttrGroup.GOALKEEPING), HANDLING(AttrGroup.GOALKEEPING),
    KICKING(AttrGroup.GOALKEEPING), ONE_ON_ONES(AttrGroup.GOALKEEPING), REFLEXES(AttrGroup.GOALKEEPING);

    companion object {
        val COUNT = entries.size
        val OUTFIELD = entries.filter { it.group != AttrGroup.GOALKEEPING }
    }
}

/** Gizli kisilik ozellikleri (1-20). */
enum class Hidden { CONSISTENCY, IMPORTANT_MATCHES, INJURY_PRONENESS, PROFESSIONALISM, AMBITION, LOYALTY, ADAPTABILITY, PRESSURE, VERSATILITY }

/** Mevkiler. */
enum class Pos(val line: Line) {
    GK(Line.GK), DR(Line.DEF), DC(Line.DEF), DL(Line.DEF), WBR(Line.DEF), WBL(Line.DEF),
    DM(Line.MID), MR(Line.MID), MC(Line.MID), ML(Line.MID),
    AMR(Line.ATT), AMC(Line.ATT), AML(Line.ATT), ST(Line.ATT);

    enum class Line { GK, DEF, MID, ATT }

    companion object { val COUNT = entries.size }
}

/**
 * Mevkiye gore yetenek agirliklari: "mevcut yetenek" (CA, 1..200) hesabinda ve
 * oyuncu uretilirken hangi yeteneklerin one cikacagini belirlemede kullanilir.
 */
object PosWeights {
    private val W: Map<Pos, Map<Attr, Float>>

    init {
        val m = HashMap<Pos, Map<Attr, Float>>()
        fun w(vararg p: Pair<Attr, Float>) = p.toMap()
        val gk = w(
            Attr.REFLEXES to 5f, Attr.HANDLING to 4f, Attr.ONE_ON_ONES to 4f, Attr.AERIAL_REACH to 3f, Attr.COMMAND_OF_AREA to 3f,
            Attr.KICKING to 2f, Attr.POSITIONING to 3f, Attr.CONCENTRATION to 2f, Attr.ANTICIPATION to 2f, Attr.DECISIONS to 2f,
            Attr.COMPOSURE to 1.5f, Attr.AGILITY to 3f, Attr.JUMPING to 1.5f, Attr.FIRST_TOUCH to 0.5f, Attr.PASSING to 0.8f,
        )
        val dc = w(
            Attr.MARKING to 5f, Attr.TACKLING to 5f, Attr.HEADING to 4f, Attr.POSITIONING to 4f, Attr.ANTICIPATION to 3f,
            Attr.CONCENTRATION to 3f, Attr.DECISIONS to 2f, Attr.BRAVERY to 2f, Attr.COMPOSURE to 1.5f, Attr.JUMPING to 3f,
            Attr.STRENGTH to 3f, Attr.PACE to 2f, Attr.ACCELERATION to 1.5f, Attr.PASSING to 1.5f, Attr.FIRST_TOUCH to 1f,
            Attr.TEAMWORK to 1f, Attr.AGGRESSION to 1f, Attr.STAMINA to 1f, Attr.TECHNIQUE to 0.5f,
        )
        val fb = w(
            Attr.TACKLING to 4f, Attr.MARKING to 3f, Attr.POSITIONING to 3f, Attr.CROSSING to 3f, Attr.PACE to 4f,
            Attr.ACCELERATION to 3f, Attr.STAMINA to 3f, Attr.WORK_RATE to 2f, Attr.TEAMWORK to 2f, Attr.ANTICIPATION to 2f,
            Attr.CONCENTRATION to 2f, Attr.DECISIONS to 1.5f, Attr.PASSING to 2f, Attr.DRIBBLING to 1.5f, Attr.FIRST_TOUCH to 1.5f,
            Attr.AGILITY to 1.5f, Attr.TECHNIQUE to 1f, Attr.STRENGTH to 1f, Attr.HEADING to 1f,
        )
        val wb = fb + w(Attr.CROSSING to 4f, Attr.DRIBBLING to 2.5f, Attr.OFF_THE_BALL to 2f, Attr.STAMINA to 4f, Attr.MARKING to 2f)
        val dm = w(
            Attr.TACKLING to 4f, Attr.MARKING to 3f, Attr.POSITIONING to 4f, Attr.ANTICIPATION to 3f, Attr.PASSING to 3.5f,
            Attr.DECISIONS to 3f, Attr.TEAMWORK to 3f, Attr.WORK_RATE to 3f, Attr.CONCENTRATION to 2f, Attr.COMPOSURE to 2f,
            Attr.FIRST_TOUCH to 2f, Attr.STAMINA to 3f, Attr.STRENGTH to 2f, Attr.VISION to 1.5f, Attr.TECHNIQUE to 1.5f,
            Attr.AGGRESSION to 1.5f, Attr.BRAVERY to 1f, Attr.PACE to 1f, Attr.HEADING to 1f,
        )
        val mc = w(
            Attr.PASSING to 5f, Attr.VISION to 4f, Attr.DECISIONS to 4f, Attr.FIRST_TOUCH to 3.5f, Attr.TECHNIQUE to 3.5f,
            Attr.TEAMWORK to 3f, Attr.WORK_RATE to 3f, Attr.COMPOSURE to 3f, Attr.ANTICIPATION to 2.5f, Attr.STAMINA to 3f,
            Attr.OFF_THE_BALL to 2f, Attr.TACKLING to 2f, Attr.POSITIONING to 2f, Attr.LONG_SHOTS to 1.5f, Attr.DRIBBLING to 1.5f,
            Attr.CONCENTRATION to 1.5f, Attr.BALANCE to 1f, Attr.AGILITY to 1f, Attr.ACCELERATION to 1f, Attr.PACE to 1f,
        )
        val wide = w(
            Attr.CROSSING to 4f, Attr.DRIBBLING to 4f, Attr.PACE to 4f, Attr.ACCELERATION to 4f, Attr.TECHNIQUE to 3f,
            Attr.FIRST_TOUCH to 3f, Attr.PASSING to 2.5f, Attr.OFF_THE_BALL to 3f, Attr.AGILITY to 3f, Attr.FLAIR to 2f,
            Attr.STAMINA to 3f, Attr.WORK_RATE to 2f, Attr.DECISIONS to 2f, Attr.VISION to 1.5f, Attr.BALANCE to 1.5f,
            Attr.FINISHING to 1.5f, Attr.TEAMWORK to 1.5f, Attr.COMPOSURE to 1f,
        )
        val am = w(
            Attr.PASSING to 4f, Attr.VISION to 4f, Attr.TECHNIQUE to 4f, Attr.FIRST_TOUCH to 4f, Attr.DRIBBLING to 3.5f,
            Attr.FLAIR to 3f, Attr.DECISIONS to 3f, Attr.COMPOSURE to 3f, Attr.OFF_THE_BALL to 3f, Attr.ANTICIPATION to 2f,
            Attr.LONG_SHOTS to 2.5f, Attr.FINISHING to 2f, Attr.AGILITY to 2.5f, Attr.ACCELERATION to 2f, Attr.BALANCE to 2f,
            Attr.PACE to 1.5f, Attr.TEAMWORK to 1.5f, Attr.WORK_RATE to 1f,
        )
        val winger = wide + w(Attr.FINISHING to 2.5f, Attr.CROSSING to 3f, Attr.FLAIR to 3f)
        val st = w(
            Attr.FINISHING to 5f, Attr.OFF_THE_BALL to 4f, Attr.COMPOSURE to 4f, Attr.FIRST_TOUCH to 3.5f, Attr.ANTICIPATION to 3f,
            Attr.PACE to 3.5f, Attr.ACCELERATION to 3.5f, Attr.HEADING to 2.5f, Attr.TECHNIQUE to 2.5f, Attr.DRIBBLING to 2.5f,
            Attr.STRENGTH to 2f, Attr.JUMPING to 2f, Attr.DECISIONS to 2f, Attr.AGILITY to 2f, Attr.BALANCE to 1.5f,
            Attr.LONG_SHOTS to 1f, Attr.PASSING to 1f, Attr.WORK_RATE to 1f, Attr.BRAVERY to 1f, Attr.STAMINA to 1f,
        )
        m[Pos.GK] = gk; m[Pos.DC] = dc; m[Pos.DR] = fb; m[Pos.DL] = fb; m[Pos.WBR] = wb; m[Pos.WBL] = wb
        m[Pos.DM] = dm; m[Pos.MC] = mc; m[Pos.MR] = wide; m[Pos.ML] = wide
        m[Pos.AMC] = am; m[Pos.AMR] = winger; m[Pos.AML] = winger; m[Pos.ST] = st
        W = m
    }

    fun of(p: Pos): Map<Attr, Float> = W.getValue(p)

    private val ARR: Array<FloatArray> by lazy { Array(Pos.COUNT) { pi -> val m = of(Pos.entries[pi]); FloatArray(Attr.COUNT) { m[Attr.entries[it]] ?: 0f } } }
    /** Agirliklar dizi olarak (eksik = 0) */
    fun arr(p: Pos): FloatArray = ARR[p.ordinal]

    /** Yeteneklerden mevkideki "guc" (1..20). */
    private val WSUM: FloatArray by lazy { FloatArray(Pos.COUNT) { arr(Pos.entries[it]).sum() } }
    private val CA_GK: IntArray by lazy { Attr.entries.filter { it.group == AttrGroup.GOALKEEPING || it.group == AttrGroup.MENTAL || it.group == AttrGroup.PHYSICAL }.map { it.ordinal }.toIntArray() }
    private val CA_OUT: IntArray by lazy { Attr.entries.filter { it.group != AttrGroup.GOALKEEPING }.map { it.ordinal }.toIntArray() }

    fun rating(attrs: ByteArray, p: Pos): Float {
        val w = arr(p)
        var sum = 0f
        for (i in w.indices) { val k = w[i]; if (k != 0f) sum += (attrs[i].toInt() and 0xFF) * k }
        return sum / 10f / WSUM[p.ordinal]
    }

    /**
     * Mevcut yetenek (CA, 1..200). Oyuncunun en iyi mevkisine gore; yan yetenekler de biraz katkida bulunur.
     * Ortalama 10'luk oyuncu ~100, ortalama 17'lik oyuncu ~180 civarinda olur.
     */
    fun currentAbility(attrs: ByteArray, best: Pos): Int {
        val main = rating(attrs, best)
        val all = if (best == Pos.GK) CA_GK else CA_OUT
        var avg = 0f
        for (i in all) avg += (attrs[i].toInt() and 0xFF) / 10f
        avg /= all.size
        val v = main * 0.8f + avg * 0.2f
        return (v * 10f).toInt().coerceIn(1, 200)
    }
}
