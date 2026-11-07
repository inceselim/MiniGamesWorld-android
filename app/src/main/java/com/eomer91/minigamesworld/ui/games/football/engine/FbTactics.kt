package com.eomer91.minigamesworld.ui.games.football.engine

/** Dizilis yerlesimi: x = kendi kalesinden rakip kaleye (0..1), y = soldan saga (0..1). */
class Slot(val pos: Pos, val x: Float, val y: Float)

enum class Formation(val label: String, val slots: List<Slot>) {
    F442("4-4-2", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DR, 0.2f, 0.88f), Slot(Pos.DC, 0.17f, 0.62f), Slot(Pos.DC, 0.17f, 0.38f), Slot(Pos.DL, 0.2f, 0.12f),
        Slot(Pos.MR, 0.45f, 0.86f), Slot(Pos.MC, 0.42f, 0.6f), Slot(Pos.MC, 0.42f, 0.4f), Slot(Pos.ML, 0.45f, 0.14f),
        Slot(Pos.ST, 0.7f, 0.6f), Slot(Pos.ST, 0.7f, 0.4f),
    )),
    F433("4-3-3", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DR, 0.2f, 0.88f), Slot(Pos.DC, 0.17f, 0.62f), Slot(Pos.DC, 0.17f, 0.38f), Slot(Pos.DL, 0.2f, 0.12f),
        Slot(Pos.DM, 0.33f, 0.5f), Slot(Pos.MC, 0.45f, 0.68f), Slot(Pos.MC, 0.45f, 0.32f),
        Slot(Pos.AMR, 0.68f, 0.84f), Slot(Pos.ST, 0.74f, 0.5f), Slot(Pos.AML, 0.68f, 0.16f),
    )),
    F4231("4-2-3-1", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DR, 0.2f, 0.88f), Slot(Pos.DC, 0.17f, 0.62f), Slot(Pos.DC, 0.17f, 0.38f), Slot(Pos.DL, 0.2f, 0.12f),
        Slot(Pos.DM, 0.35f, 0.62f), Slot(Pos.DM, 0.35f, 0.38f),
        Slot(Pos.AMR, 0.6f, 0.84f), Slot(Pos.AMC, 0.58f, 0.5f), Slot(Pos.AML, 0.6f, 0.16f), Slot(Pos.ST, 0.76f, 0.5f),
    )),
    F4141("4-1-4-1", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DR, 0.2f, 0.88f), Slot(Pos.DC, 0.17f, 0.62f), Slot(Pos.DC, 0.17f, 0.38f), Slot(Pos.DL, 0.2f, 0.12f),
        Slot(Pos.DM, 0.32f, 0.5f), Slot(Pos.MR, 0.5f, 0.86f), Slot(Pos.MC, 0.48f, 0.62f), Slot(Pos.MC, 0.48f, 0.38f), Slot(Pos.ML, 0.5f, 0.14f),
        Slot(Pos.ST, 0.74f, 0.5f),
    )),
    F451("4-5-1", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DR, 0.2f, 0.88f), Slot(Pos.DC, 0.17f, 0.62f), Slot(Pos.DC, 0.17f, 0.38f), Slot(Pos.DL, 0.2f, 0.12f),
        Slot(Pos.MR, 0.44f, 0.86f), Slot(Pos.MC, 0.4f, 0.66f), Slot(Pos.MC, 0.38f, 0.5f), Slot(Pos.MC, 0.4f, 0.34f), Slot(Pos.ML, 0.44f, 0.14f),
        Slot(Pos.ST, 0.7f, 0.5f),
    )),
    F352("3-5-2", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DC, 0.17f, 0.72f), Slot(Pos.DC, 0.15f, 0.5f), Slot(Pos.DC, 0.17f, 0.28f),
        Slot(Pos.WBR, 0.4f, 0.9f), Slot(Pos.DM, 0.34f, 0.5f), Slot(Pos.MC, 0.47f, 0.66f), Slot(Pos.MC, 0.47f, 0.34f), Slot(Pos.WBL, 0.4f, 0.1f),
        Slot(Pos.ST, 0.72f, 0.6f), Slot(Pos.ST, 0.72f, 0.4f),
    )),
    F343("3-4-3", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DC, 0.17f, 0.72f), Slot(Pos.DC, 0.15f, 0.5f), Slot(Pos.DC, 0.17f, 0.28f),
        Slot(Pos.MR, 0.43f, 0.88f), Slot(Pos.MC, 0.4f, 0.62f), Slot(Pos.MC, 0.4f, 0.38f), Slot(Pos.ML, 0.43f, 0.12f),
        Slot(Pos.AMR, 0.68f, 0.8f), Slot(Pos.ST, 0.74f, 0.5f), Slot(Pos.AML, 0.68f, 0.2f),
    )),
    F532("5-3-2", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.WBR, 0.26f, 0.9f), Slot(Pos.DC, 0.16f, 0.7f), Slot(Pos.DC, 0.14f, 0.5f), Slot(Pos.DC, 0.16f, 0.3f), Slot(Pos.WBL, 0.26f, 0.1f),
        Slot(Pos.MC, 0.42f, 0.7f), Slot(Pos.MC, 0.4f, 0.5f), Slot(Pos.MC, 0.42f, 0.3f),
        Slot(Pos.ST, 0.7f, 0.6f), Slot(Pos.ST, 0.7f, 0.4f),
    )),
    F4312("4-3-1-2", listOf(
        Slot(Pos.GK, 0.03f, 0.5f), Slot(Pos.DR, 0.2f, 0.88f), Slot(Pos.DC, 0.17f, 0.62f), Slot(Pos.DC, 0.17f, 0.38f), Slot(Pos.DL, 0.2f, 0.12f),
        Slot(Pos.MC, 0.42f, 0.7f), Slot(Pos.DM, 0.36f, 0.5f), Slot(Pos.MC, 0.42f, 0.3f), Slot(Pos.AMC, 0.58f, 0.5f),
        Slot(Pos.ST, 0.74f, 0.6f), Slot(Pos.ST, 0.74f, 0.4f),
    )),
}

enum class Mentality(val risk: Float) { VERY_DEFENSIVE(-2f), DEFENSIVE(-1f), BALANCED(0f), ATTACKING(1f), VERY_ATTACKING(2f) }
enum class PassingStyle { SHORT, MIXED, DIRECT }
enum class Tempo { SLOW, NORMAL, FAST }
enum class Width { NARROW, NORMAL, WIDE }
enum class Pressing { LOW, MEDIUM, HIGH }
enum class DefLine { DEEP, NORMAL, HIGH }
enum class Marking { ZONAL, MAN }
enum class AttackFocus { CENTER, BALANCED, FLANKS }

data class Tactics(
    val formation: Formation = Formation.F442,
    val mentality: Mentality = Mentality.BALANCED,
    val passing: PassingStyle = PassingStyle.MIXED,
    val tempo: Tempo = Tempo.NORMAL,
    val width: Width = Width.NORMAL,
    val pressing: Pressing = Pressing.MEDIUM,
    val defLine: DefLine = DefLine.NORMAL,
    val marking: Marking = Marking.ZONAL,
    val focus: AttackFocus = AttackFocus.BALANCED,
    /** Topu kazaninca hizli hucuma cik */
    val counter: Boolean = false,
    val offsideTrap: Boolean = false,
    /** Sure gecir (ondeyken oyunu yavaslat) */
    val timeWasting: Boolean = false,
    /** Sert mudahale */
    val hardTackling: Boolean = false,
) {
    companion object {
        /** Yapay zeka kulupleri icin kadroya uygun dengeli taktik */
        val DEFAULT = Tactics()
    }
}
