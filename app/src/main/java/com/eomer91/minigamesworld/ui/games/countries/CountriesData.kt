package com.eomer91.minigamesworld.ui.games.countries

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R

/** Kaynak paketi (fiyat, bakim, uretim). Degerler eski CountriesWar oyunundan. */
data class Res(val gold: Int = 0, val wood: Int = 0, val clay: Int = 0, val iron: Int = 0, val gems: Int = 0) {
    operator fun times(k: Float) = Res((gold * k).toInt(), (wood * k).toInt(), (clay * k).toInt(), (iron * k).toInt(), gems)
}

enum class Resource(@StringRes val nameRes: Int, val icon: String) {
    GOLD(R.string.cw_res_gold, "💰"),
    WOOD(R.string.cw_res_wood, "🪵"),
    CLAY(R.string.cw_res_clay, "🧱"),
    IRON(R.string.cw_res_iron, "⛏"),
}

enum class UnitType(
    @StringRes val nameRes: Int,
    val icon: String,
    val attack: Int,
    val defence: Int,
    val cost: Res,
    /** Asker olarak alinan nufus (kisla kapasitesinden de bu kadar yer tutar). */
    val pop: Int,
    /** Tur basina bakim (altin haric: eski oyundaki gibi ordu altin tuketmez). */
    val upkeep: Res,
) {
    SPEARMAN(R.string.cw_unit_spearman, "🔱", 10, 40, Res(70, 25), 1, Res(wood = 1)),
    BOWMAN(R.string.cw_unit_bowman, "🏹", 20, 30, Res(40, clay = 30), 2, Res(clay = 1)),
    SWORDMAN(R.string.cw_unit_swordman, "⚔", 35, 75, Res(90, clay = 90, iron = 90), 1, Res(iron = 2)),
    AXEMAN(R.string.cw_unit_axeman, "🪓", 45, 10, Res(20, 20), 3, Res(wood = 2)),
    KNIGHT(R.string.cw_unit_knight, "🐎", 95, 40, Res(190, iron = 60), 4, Res(iron = 2)),
    CATAPULT(R.string.cw_unit_catapult, "🪨", 180, 10, Res(130, clay = 40), 5, Res(clay = 2)),
}

enum class Building(
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val icon: String,
    val cost: Res,
    val upkeep: Res,
    val income: Res = Res(),
    val maxLevel: Int = 99,
) {
    FARM(R.string.cw_b_farm, R.string.cw_b_farm_desc, "🌾", Res(100, 40, 45), Res(wood = 1, clay = 1)),
    PARLIAMENT(R.string.cw_b_parliament, R.string.cw_b_parliament_desc, "🏛", Res(200, 20, 25, 15), Res(wood = 3, clay = 4, iron = 1)),
    TRADE_CENTER(R.string.cw_b_trade, R.string.cw_b_trade_desc, "🏪", Res(1150, 80, 65, 75), Res(), income = Res(gold = 20)),
    WOODCUTTER(R.string.cw_b_woodcutter, R.string.cw_b_woodcutter_desc, "🪓", Res(500, 0, 25, 15), Res(), income = Res(3, wood = 20)),
    BRICKHOUSE(R.string.cw_b_brickhouse, R.string.cw_b_brickhouse_desc, "🧱", Res(500, 30, 0, 25), Res(), income = Res(3, clay = 20)),
    MINE(R.string.cw_b_mine, R.string.cw_b_mine_desc, "⛏", Res(750, 60, 55), Res(), income = Res(5, iron = 20)),
    BARRACKS(R.string.cw_b_barracks, R.string.cw_b_barracks_desc, "⛺", Res(100, 10, 15, 35), Res(15, clay = 3)),
    CASTLE(R.string.cw_b_castle, R.string.cw_b_castle_desc, "🏰", Res(2000, 130, 185, 155), Res(80, 8, 12, 9), maxLevel = 20),
    TOWER(R.string.cw_b_tower, R.string.cw_b_tower_desc, "🗼", Res(1300, 220, 125, 115), Res(50, 8, 6, 3), maxLevel = 20),
    ESPIONAGE(R.string.cw_b_espionage, R.string.cw_b_espionage_desc, "🕵", Res(gems = 5), Res(50), maxLevel = 1),
    AVM(R.string.cw_b_avm, R.string.cw_b_avm_desc, "🏬", Res(gems = 1), Res(), income = Res(80, 80, 80, 80)),
}

/** Yonetim bicimleri: olumlu/olumsuz yuzdesel etkiler. [buildPrice] pozitif = binalar ucuz. */
enum class Polity(
    @StringRes val nameRes: Int,
    val attack: Float = 0f,
    val defence: Float = 0f,
    val income: Float = 0f,
    val population: Float = 0f,
    val buildPrice: Float = 0f,
) {
    MONARCHY(R.string.cw_pol_monarchy, attack = 0.2f, income = 0.2f, population = -0.2f),
    ARISTOCRACY(R.string.cw_pol_aristocracy, attack = 0.2f, income = 0.2f, buildPrice = -0.2f),
    THEOCRACY(R.string.cw_pol_theocracy, defence = 0.2f, income = 0.2f, buildPrice = -0.2f),
    DICTATORSHIP(R.string.cw_pol_dictatorship, defence = 0.2f, population = 0.2f, attack = -0.2f),
    DEMOCRACY(R.string.cw_pol_democracy, population = 0.2f, buildPrice = 0.2f, defence = -0.1f),
    PREMIUM(R.string.cw_pol_premium, attack = 1f, defence = 1f, population = 1f),
}

enum class Focus(@StringRes val nameRes: Int) {
    ATTACK(R.string.cw_focus_attack),
    DEFENCE(R.string.cw_focus_defence),
    POPULATION(R.string.cw_focus_population),
    INCOME(R.string.cw_focus_income),
}

/** Zorluk: bot sayisi ve botlarin baslangic gucu carpani. */
enum class CwDifficulty(@StringRes val nameRes: Int, val bots: Int, val botPower: Float, val aggression: Float) {
    EASY(R.string.cw_diff_easy, 1, 0.8f, 0.03f),
    NORMAL(R.string.cw_diff_normal, 3, 1.0f, 0.04f),
    HARD(R.string.cw_diff_hard, 5, 1.2f, 0.05f),
    EXPERT(R.string.cw_diff_expert, 7, 1.5f, 0.06f),
}

val BotNames = listOf("Valoria", "Kordania", "Esteria", "Morvan", "Thalor", "Brenmark", "Zarkesh", "Orlenia", "Quarth", "Sylvar")
