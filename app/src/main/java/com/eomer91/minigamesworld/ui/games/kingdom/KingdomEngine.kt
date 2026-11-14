package com.eomer91.minigamesworld.ui.games.kingdom

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/*
 * Idle Kingdom Wars – oyun mantigi (UI'dan bagimsiz).
 * Kaynaklar dakika bazinda uretilir; oyun kapaliyken en fazla [KingdomEngine.offlineCapHours] saat birikir.
 * Kahramanlar sandik/sans ile degil, belirli bolgeleri fethederek acilir.
 */

enum class Res(val emoji: String) { GOLD("🪙"), WOOD("🪵"), STONE("🪨"), FOOD("🌾") }

enum class KBuilding(val emoji: String, val produces: Res?, val basePerMin: Double) {
    CASTLE("🏰", null, 0.0),
    GOLD_MINE("⛏️", Res.GOLD, 10.0),
    LUMBER_CAMP("🪓", Res.WOOD, 12.0),
    QUARRY("🪨", Res.STONE, 8.0),
    FARM("🌾", Res.FOOD, 15.0),
    WAREHOUSE("📦", null, 0.0),
    BARRACKS("⚔️", null, 0.0),
    ARCHERY("🏹", null, 0.0),
    STABLE("🐎", null, 0.0),
}

enum class UnitType(val emoji: String, val atk: Double, val def: Double, val hp: Double) {
    SWORDSMAN("🗡️", 10.0, 8.0, 50.0),
    SPEARMAN("🔱", 8.0, 10.0, 55.0),
    ARCHER("🏹", 12.0, 4.0, 35.0),
    CAVALRY("🐎", 16.0, 9.0, 70.0),
}

enum class Hero(val emoji: String) { ARTHUR("👑"), ROBIN("🏹"), LEONIDAS("🛡️") }

enum class Tech(val emoji: String) {
    ECON_GOLD("🪙"), ECON_WOOD("🪵"), ECON_STONE("🪨"), ECON_FOOD("🌾"),
    MIL_INFANTRY("🗡️"), MIL_ARCHERS("🏹"), MIL_CAVALRY("🐎"),
    DEF_ARMOR("🛡️"), LOG_CAPACITY("🎒"), LOG_OFFLINE("⏳"),
}

/** Bolge: dusman ordusu, fetih odulu, kalici bonus ve vergi geliri. */
data class Region(
    val index: Int,
    val emoji: String,
    val enemy: Map<UnitType, Int>,
    val reward: Map<Res, Int>,
    /** Fethedilince kalici uretim bonusu (null = yok) */
    val bonusRes: Res?,
    val bonusPct: Int,
    /** Fethedilince dakikada eklenen altin vergisi */
    val taxPerMin: Double,
    val unlocksHero: Hero? = null,
    val isBoss: Boolean = false,
)

@Serializable
data class KingdomSave(
    val version: Int = 1,
    val res: Map<Res, Double> = mapOf(Res.GOLD to 100.0, Res.WOOD to 100.0, Res.STONE to 50.0, Res.FOOD to 50.0),
    val levels: Map<KBuilding, Int> = mapOf(KBuilding.CASTLE to 1),
    val army: Map<UnitType, Int> = mapOf(UnitType.SWORDSMAN to 10),
    val heroes: Map<Hero, Int> = mapOf(Hero.ARTHUR to 1),
    val activeHero: Hero = Hero.ARTHUR,
    val conquered: Int = 0,
    val techs: Set<Tech> = emptySet(),
    val questIndex: Int = 0,
    val battlesWon: Int = 0,
    val unitsTrained: Int = 0,
    val savedAt: Long = 0,
)

data class BattleReport(
    val region: Region,
    val won: Boolean,
    val myPower: Double,
    val enemyPower: Double,
    val myLosses: Map<UnitType, Int>,
    val enemyLosses: Map<UnitType, Int>,
    val reward: Map<Res, Int>,
    val heroUnlocked: Hero?,
)

/** Gorev: kosul + odul. Sirayla acilir. */
data class Quest(val kind: QuestKind, val target: Int, val reward: Map<Res, Int>, val building: KBuilding? = null)

enum class QuestKind { BUILD, TRAIN, CONQUER, RESEARCH, HERO_LEVEL, WIN_BATTLES }

class KingdomEngine(save: KingdomSave = KingdomSave()) {

    val res: MutableMap<Res, Double> = Res.entries.associateWith { save.res[it] ?: 0.0 }.toMutableMap()
    val levels: MutableMap<KBuilding, Int> = KBuilding.entries.associateWith { save.levels[it] ?: 0 }.toMutableMap()
    val army: MutableMap<UnitType, Int> = UnitType.entries.associateWith { save.army[it] ?: 0 }.toMutableMap()
    val heroes: MutableMap<Hero, Int> = save.heroes.toMutableMap()
    var activeHero: Hero = if (save.activeHero in save.heroes) save.activeHero else Hero.ARTHUR
        private set
    var conquered: Int = save.conquered.coerceIn(0, REGIONS.size)
        private set
    val techs: MutableSet<Tech> = save.techs.toMutableSet()
    var questIndex: Int = save.questIndex
        private set
    var battlesWon: Int = save.battlesWon
        private set
    var unitsTrained: Int = save.unitsTrained
        private set

    init {
        if (levels[KBuilding.CASTLE]!! < 1) levels[KBuilding.CASTLE] = 1
        if (heroes.isEmpty()) heroes[Hero.ARTHUR] = 1
    }

    fun save(now: Long) = KingdomSave(
        res = res.toMap(), levels = levels.toMap(), army = army.toMap(), heroes = heroes.toMap(),
        activeHero = activeHero, conquered = conquered, techs = techs.toSet(), questIndex = questIndex,
        battlesWon = battlesWon, unitsTrained = unitsTrained, savedAt = now,
    )

    // ---------- Ekonomi ----------

    fun level(b: KBuilding) = levels[b] ?: 0

    val castleLevel get() = level(KBuilding.CASTLE)

    /** Cag (1..3) kale seviyesine gore: 1-3 baslangic, 4-7 krallik, 8-10 imparatorluk. */
    val age get() = when { castleLevel >= 8 -> 3; castleLevel >= 4 -> 2; else -> 1 }

    fun capacity(): Double = 1000.0 * 2.2.pow(level(KBuilding.WAREHOUSE).toDouble())

    fun perMinute(r: Res): Double {
        val b = KBuilding.entries.first { it.produces == r }
        val lv = level(b)
        var base = if (lv == 0) 0.0 else b.basePerMin * lv * (1 + 0.1 * (lv - 1))
        var pct = 0
        for (i in 0 until conquered) if (REGIONS[i].bonusRes == r) pct += REGIONS[i].bonusPct
        if (techFor(r) in techs) pct += 15
        base *= 1 + pct / 100.0
        if (r == Res.GOLD) base += taxPerMin()
        return base
    }

    fun taxPerMin(): Double = (0 until conquered).sumOf { REGIONS[it].taxPerMin }

    private fun techFor(r: Res) = when (r) {
        Res.GOLD -> Tech.ECON_GOLD
        Res.WOOD -> Tech.ECON_WOOD
        Res.STONE -> Tech.ECON_STONE
        Res.FOOD -> Tech.ECON_FOOD
    }

    /** [seconds] kadar uretim; depo kapasitesini asmaz. Gercekten eklenen miktarlari dondurur. */
    fun produce(seconds: Double): Map<Res, Double> {
        val cap = capacity()
        val added = HashMap<Res, Double>()
        for (r in Res.entries) {
            val before = res[r]!!
            val after = if (before >= cap) before else min(cap, before + perMinute(r) * seconds / 60.0)
            res[r] = after
            added[r] = after - before
        }
        return added
    }

    val offlineCapHours: Int get() = if (Tech.LOG_OFFLINE in techs) 12 else 8

    fun offline(seconds: Long): Map<Res, Double> {
        val s = min(seconds.toDouble(), offlineCapHours * 3600.0)
        if (s < 60) return emptyMap()
        return produce(s).filterValues { it >= 1 }
    }

    /** Odullu reklam: cevrimdisi kazanci bir kez daha ekler (depo siniri asilabilir, israf olmasin). */
    fun bonus(amounts: Map<Res, Double>) {
        for ((r, v) in amounts) res[r] = res[r]!! + v
    }

    fun canAfford(cost: Map<Res, Int>) = cost.all { (r, v) -> res[r]!! >= v }

    private fun pay(cost: Map<Res, Int>): Boolean {
        if (!canAfford(cost)) return false
        for ((r, v) in cost) res[r] = res[r]!! - v
        return true
    }

    private fun give(r: Map<Res, Int>) {
        for ((k, v) in r) res[k] = res[k]!! + v
    }

    // ---------- Binalar ----------

    fun isUnlocked(b: KBuilding): Boolean = when (b) {
        KBuilding.ARCHERY -> castleLevel >= 2
        KBuilding.STABLE -> castleLevel >= 4
        else -> true
    }

    fun maxLevel(b: KBuilding): Int = if (b == KBuilding.CASTLE) MAX_LEVEL else min(MAX_LEVEL, castleLevel + 1)

    fun upgradeCost(b: KBuilding): Map<Res, Int> {
        val lv = level(b)
        val f = COST_GROWTH.pow(lv.toDouble())
        return when (b) {
            KBuilding.CASTLE -> {
                val c = CASTLE_GROWTH.pow(lv.toDouble())
                mapOf(Res.GOLD to (400 * c).toInt(), Res.WOOD to (400 * c).toInt(), Res.STONE to (300 * c).toInt())
            }
            KBuilding.GOLD_MINE -> mapOf(Res.WOOD to (60 * f).toInt(), Res.STONE to (30 * f).toInt())
            KBuilding.LUMBER_CAMP -> mapOf(Res.GOLD to (50 * f).toInt(), Res.STONE to (20 * f).toInt())
            KBuilding.QUARRY -> mapOf(Res.GOLD to (60 * f).toInt(), Res.WOOD to (50 * f).toInt())
            KBuilding.FARM -> mapOf(Res.GOLD to (40 * f).toInt(), Res.WOOD to (40 * f).toInt())
            KBuilding.WAREHOUSE -> mapOf(Res.WOOD to (150 * f).toInt(), Res.STONE to (100 * f).toInt())
            KBuilding.BARRACKS -> mapOf(Res.GOLD to (80 * f).toInt(), Res.WOOD to (100 * f).toInt())
            KBuilding.ARCHERY -> mapOf(Res.GOLD to (120 * f).toInt(), Res.WOOD to (150 * f).toInt())
            KBuilding.STABLE -> mapOf(Res.GOLD to (200 * f).toInt(), Res.WOOD to (150 * f).toInt(), Res.STONE to (100 * f).toInt())
        }
    }

    fun canUpgrade(b: KBuilding) = isUnlocked(b) && level(b) < maxLevel(b) && canAfford(upgradeCost(b))

    fun upgrade(b: KBuilding): Boolean {
        if (!isUnlocked(b) || level(b) >= maxLevel(b)) return false
        if (!pay(upgradeCost(b))) return false
        levels[b] = level(b) + 1
        return true
    }

    // ---------- Ordu ----------

    fun armyCapacity(): Int {
        val base = ARMY_CAP[(castleLevel - 1).coerceIn(0, ARMY_CAP.lastIndex)]
        return if (Tech.LOG_CAPACITY in techs) (base * 1.2).toInt() else base
    }

    fun armySize() = army.values.sum()

    fun trainBuilding(u: UnitType) = when (u) {
        UnitType.SWORDSMAN, UnitType.SPEARMAN -> KBuilding.BARRACKS
        UnitType.ARCHER -> KBuilding.ARCHERY
        UnitType.CAVALRY -> KBuilding.STABLE
    }

    fun canTrainType(u: UnitType): Boolean {
        val b = trainBuilding(u)
        return level(b) >= 1 && (u != UnitType.SPEARMAN || level(KBuilding.BARRACKS) >= 2)
    }

    fun unitCost(u: UnitType): Map<Res, Int> = when (u) {
        UnitType.SWORDSMAN -> mapOf(Res.GOLD to 10, Res.FOOD to 8)
        UnitType.SPEARMAN -> mapOf(Res.GOLD to 10, Res.WOOD to 6, Res.FOOD to 8)
        UnitType.ARCHER -> mapOf(Res.GOLD to 14, Res.WOOD to 10, Res.FOOD to 8)
        UnitType.CAVALRY -> mapOf(Res.GOLD to 30, Res.FOOD to 20, Res.STONE to 5)
    }

    /** Kaynak ve kapasiteye gore egitilebilecek en fazla asker. */
    fun maxTrainable(u: UnitType): Int {
        if (!canTrainType(u)) return 0
        var n = armyCapacity() - armySize()
        for ((r, v) in unitCost(u)) n = min(n, (res[r]!! / v).toInt())
        return max(0, n)
    }

    fun train(u: UnitType, count: Int): Int {
        val n = min(count, maxTrainable(u))
        if (n <= 0) return 0
        pay(unitCost(u).mapValues { it.value * n })
        army[u] = army[u]!! + n
        unitsTrained += n
        return n
    }

    // ---------- Kahramanlar ----------

    fun heroLevel(h: Hero) = heroes[h] ?: 0

    fun selectHero(h: Hero) {
        if (h in heroes) activeHero = h
    }

    fun heroUpgradeCost(h: Hero): Map<Res, Int> {
        val f = 1.7.pow(heroLevel(h).toDouble())
        return mapOf(Res.GOLD to (150 * f).toInt(), Res.FOOD to (80 * f).toInt())
    }

    fun upgradeHero(h: Hero): Boolean {
        if (h !in heroes || heroLevel(h) >= HERO_MAX) return false
        if (!pay(heroUpgradeCost(h))) return false
        heroes[h] = heroLevel(h) + 1
        return true
    }

    /** Kahraman bonusu yuzde olarak: temel + seviye basina 4. */
    fun heroBonusPct(h: Hero) = 10 + 4 * heroLevel(h)

    // ---------- Arastirma ----------

    fun techCost(t: Tech): Map<Res, Int> {
        val tier = when (t) {
            Tech.ECON_GOLD, Tech.ECON_WOOD, Tech.ECON_STONE, Tech.ECON_FOOD -> 1.0
            Tech.MIL_INFANTRY, Tech.MIL_ARCHERS, Tech.MIL_CAVALRY -> 2.0
            Tech.DEF_ARMOR -> 2.5
            Tech.LOG_CAPACITY, Tech.LOG_OFFLINE -> 3.0
        }
        return mapOf(Res.GOLD to (400 * tier).toInt(), Res.STONE to (250 * tier).toInt(), Res.FOOD to (150 * tier).toInt())
    }

    /** Arastirmalar kale seviyesi 2'den sonra acilir; askeri olanlar 3, lojistik 5. */
    fun techUnlocked(t: Tech) = when (t) {
        Tech.ECON_GOLD, Tech.ECON_WOOD, Tech.ECON_STONE, Tech.ECON_FOOD -> castleLevel >= 2
        Tech.MIL_INFANTRY, Tech.MIL_ARCHERS, Tech.MIL_CAVALRY, Tech.DEF_ARMOR -> castleLevel >= 3
        Tech.LOG_CAPACITY, Tech.LOG_OFFLINE -> castleLevel >= 5
    }

    fun research(t: Tech): Boolean {
        if (t in techs || !techUnlocked(t)) return false
        if (!pay(techCost(t))) return false
        techs += t
        return true
    }

    // ---------- Savas ----------

    val nextRegion: Region? get() = REGIONS.getOrNull(conquered)

    private fun unitAtkMult(u: UnitType): Double {
        var m = 1.0
        when (u) {
            UnitType.SWORDSMAN, UnitType.SPEARMAN -> if (Tech.MIL_INFANTRY in techs) m += 0.15
            UnitType.ARCHER -> if (Tech.MIL_ARCHERS in techs) m += 0.15
            UnitType.CAVALRY -> if (Tech.MIL_CAVALRY in techs) m += 0.15
        }
        when (activeHero) {
            Hero.ARTHUR -> if (u == UnitType.SWORDSMAN || u == UnitType.SPEARMAN) m += heroBonusPct(Hero.ARTHUR) / 100.0
            Hero.ROBIN -> if (u == UnitType.ARCHER) m += heroBonusPct(Hero.ROBIN) / 100.0
            Hero.LEONIDAS -> {}
        }
        return m
    }

    private fun myDefMult(): Double {
        var m = 1.0
        if (Tech.DEF_ARMOR in techs) m += 0.15
        if (activeHero == Hero.LEONIDAS) m += heroBonusPct(Hero.LEONIDAS) / 100.0
        return m
    }

    /**
     * Bir tarafin digerine karsi gucu. Her birim tipi, karsi ordunun bilesimine gore
     * avantajli oldugu tipe %50 fazla, dezavantajli oldugu tipe %30 az etki eder.
     */
    private fun power(side: Map<UnitType, Int>, other: Map<UnitType, Int>, atkMult: (UnitType) -> Double, defMult: Double): Double {
        val otherTotal = other.values.sum().coerceAtLeast(1)
        var p = 0.0
        for ((u, n) in side) {
            if (n <= 0) continue
            var vs = 0.0
            for ((o, m) in other) vs += m.toDouble() / otherTotal * counter(u, o)
            if (other.values.sum() == 0) vs = 1.0
            val unitValue = u.atk * atkMult(u) * vs + (u.def * defMult + u.hp / 5)
            p += n * unitValue
        }
        return p
    }

    fun myPowerVs(enemy: Map<UnitType, Int>) = power(army.filterValues { it > 0 }, enemy, ::unitAtkMult, myDefMult())

    fun enemyPower(r: Region) = power(r.enemy, army.filterValues { it > 0 }, { 1.0 }, 1.0)

    /** Siradaki bolgeye saldir. Ordu bossa saldiri yapilamaz (null). */
    fun attack(rng: Random = Random.Default): BattleReport? {
        val region = nextRegion ?: return null
        if (armySize() == 0) return null
        val mine = myPowerVs(region.enemy) * (0.9 + rng.nextDouble() * 0.2)
        val theirs = enemyPower(region) * (0.9 + rng.nextDouble() * 0.2)
        val won = mine > theirs
        val ratio = if (won) theirs / mine else mine / theirs
        // Kazanan, gucler yakin oldukca daha cok kaybeder; kaybeden ordusunun buyuk kismini kaybeder.
        val myLossFrac = if (won) (0.45 * ratio * ratio).coerceIn(0.03, 0.6) else (0.55 + 0.25 * (1 - ratio)).coerceIn(0.5, 0.8)
        val enemyLossFrac = if (won) 1.0 else (0.6 * ratio).coerceIn(0.1, 0.6)
        val myLosses = army.filterValues { it > 0 }.mapValues { (_, n) -> min(n, (n * myLossFrac + 0.5).toInt()) }
        for ((u, l) in myLosses) army[u] = army[u]!! - l
        val enemyLosses = region.enemy.mapValues { (_, n) -> min(n, (n * enemyLossFrac + 0.5).toInt()) }
        var hero: Hero? = null
        val reward: Map<Res, Int>
        if (won) {
            conquered++
            battlesWon++
            reward = region.reward
            give(reward)
            if (region.unlocksHero != null && region.unlocksHero !in heroes) {
                heroes[region.unlocksHero] = 1
                hero = region.unlocksHero
            }
        } else {
            reward = emptyMap()
        }
        return BattleReport(region, won, mine, theirs, myLosses.filterValues { it > 0 }, enemyLosses.filterValues { it > 0 }, reward, hero)
    }

    // ---------- Gorevler ----------

    val quest: Quest? get() = QUESTS.getOrNull(questIndex)

    fun questProgress(q: Quest): Int = when (q.kind) {
        QuestKind.BUILD -> level(q.building!!)
        QuestKind.TRAIN -> unitsTrained
        QuestKind.CONQUER -> conquered
        QuestKind.RESEARCH -> techs.size
        QuestKind.HERO_LEVEL -> heroes.values.maxOrNull() ?: 0
        QuestKind.WIN_BATTLES -> battlesWon
    }

    fun questDone(): Boolean = quest?.let { questProgress(it) >= it.target } == true

    fun claimQuest(): Map<Res, Int>? {
        val q = quest ?: return null
        if (questProgress(q) < q.target) return null
        give(q.reward)
        questIndex++
        return q.reward
    }

    /** Siralama/rekor icin tek sayi: fetih + kale + ordu. */
    fun score(): Int = conquered * 1000 + castleLevel * 200 + techs.size * 50 + min(armySize(), 5000)

    companion object {
        const val MAX_LEVEL = 10
        const val COST_GROWTH = 2.1
        const val CASTLE_GROWTH = 2.2
        const val HERO_MAX = 10
        val ARMY_CAP = intArrayOf(50, 100, 200, 350, 550, 800, 1100, 1500, 2000, 3000)

        /** Saldiran tip -> savunan tip carpani (tas-kagit-makas). */
        fun counter(a: UnitType, d: UnitType): Double = when {
            a == UnitType.CAVALRY && d == UnitType.ARCHER -> 1.5
            a == UnitType.SPEARMAN && d == UnitType.CAVALRY -> 1.5
            a == UnitType.ARCHER && (d == UnitType.SWORDSMAN || d == UnitType.SPEARMAN) -> 1.5
            a == UnitType.SWORDSMAN && d == UnitType.SPEARMAN -> 1.3
            a == UnitType.ARCHER && d == UnitType.CAVALRY -> 0.7
            a == UnitType.CAVALRY && d == UnitType.SPEARMAN -> 0.7
            a == UnitType.SWORDSMAN && d == UnitType.ARCHER -> 0.8
            else -> 1.0
        }

        private fun army(s: Int, sp: Int = 0, a: Int = 0, c: Int = 0) = buildMap {
            if (s > 0) put(UnitType.SWORDSMAN, s)
            if (sp > 0) put(UnitType.SPEARMAN, sp)
            if (a > 0) put(UnitType.ARCHER, a)
            if (c > 0) put(UnitType.CAVALRY, c)
        }

        /** Gec bolgelerde dusman ordusu buyur: idle ilerleme gunlere yayilsin. */
        private fun Map<UnitType, Int>.x(f: Double) = mapValues { (it.value * f).toInt() }

        private fun loot(g: Int, w: Int = 0, s: Int = 0, f: Int = 0) = buildMap {
            if (g > 0) put(Res.GOLD, g)
            if (w > 0) put(Res.WOOD, w)
            if (s > 0) put(Res.STONE, s)
            if (f > 0) put(Res.FOOD, f)
        }

        /** 15 bolgelik harita; son bolge boss (Ork Krali). */
        val REGIONS: List<Region> = listOf(
            Region(0, "⛺", army(6), loot(150, 100, 0, 100), null, 0, 2.0),
            Region(1, "🌲", army(12, a = 4), loot(250, 300), Res.WOOD, 15, 3.0),
            Region(2, "🏴", army(20, 6, 6), loot(400, 200, 150, 200), null, 0, 4.0, unlocksHero = Hero.ROBIN),
            Region(3, "⛰️", army(25, 10, 10), loot(500, 0, 400), Res.STONE, 15, 5.0),
            Region(4, "🌻", army(30, 15, 15), loot(600, 300, 200, 600), Res.FOOD, 15, 6.0),
            Region(5, "🏯", army(40, 25, 25, 5).x(1.4), loot(900, 500, 400, 400), null, 0, 8.0),
            Region(6, "⚒️", army(50, 30, 30, 10).x(1.7), loot(1200, 400, 600), Res.GOLD, 15, 10.0, unlocksHero = Hero.LEONIDAS),
            Region(7, "👹", army(70, 30, 40, 15).x(2.0), loot(1500, 800, 600, 800), null, 0, 12.0),
            Region(8, "🏰", army(80, 50, 50, 30).x(2.3), loot(2000, 1000, 1000, 1000), null, 0, 15.0),
            Region(9, "💎", army(100, 60, 60, 40).x(2.6), loot(3000, 1000, 1500), Res.GOLD, 20, 18.0),
            Region(10, "🌋", army(120, 80, 90, 60).x(2.9), loot(4000, 2000, 2000, 2000), null, 0, 22.0),
            Region(11, "🏞️", army(150, 100, 100, 80).x(3.2), loot(5000, 2500, 2500, 4000), Res.FOOD, 20, 26.0),
            Region(12, "🗼", army(180, 140, 130, 110).x(3.5), loot(7000, 3500, 3500, 3500), null, 0, 30.0),
            Region(13, "🐉", army(220, 170, 170, 150).x(3.8), loot(9000, 5000, 5000, 5000), null, 0, 36.0),
            Region(14, "👑", army(300, 220, 220, 220).x(3.9), loot(15000, 8000, 8000, 8000), null, 0, 50.0, isBoss = true),
        )

        val QUESTS: List<Quest> = listOf(
            Quest(QuestKind.BUILD, 1, loot(100, 50), KBuilding.LUMBER_CAMP),
            Quest(QuestKind.BUILD, 1, loot(0, 120, 50), KBuilding.GOLD_MINE),
            Quest(QuestKind.BUILD, 1, loot(100, 100, 0, 100), KBuilding.BARRACKS),
            Quest(QuestKind.TRAIN, 10, loot(150, 0, 0, 150)),
            Quest(QuestKind.CONQUER, 1, loot(200, 150, 100)),
            Quest(QuestKind.BUILD, 1, loot(100, 100), KBuilding.FARM),
            Quest(QuestKind.BUILD, 1, loot(150, 150), KBuilding.QUARRY),
            Quest(QuestKind.BUILD, 2, loot(300, 300, 200), KBuilding.CASTLE),
            Quest(QuestKind.BUILD, 1, loot(200, 200, 100), KBuilding.WAREHOUSE),
            Quest(QuestKind.CONQUER, 3, loot(400, 300, 300, 300)),
            Quest(QuestKind.RESEARCH, 1, loot(300, 0, 200)),
            Quest(QuestKind.HERO_LEVEL, 3, loot(500, 300, 300, 300)),
            Quest(QuestKind.TRAIN, 150, loot(600, 400, 300, 600)),
            Quest(QuestKind.BUILD, 4, loot(1000, 800, 800), KBuilding.CASTLE),
            Quest(QuestKind.CONQUER, 7, loot(1500, 1000, 1000, 1000)),
            Quest(QuestKind.RESEARCH, 5, loot(2000, 1000, 1000)),
            Quest(QuestKind.WIN_BATTLES, 10, loot(3000, 1500, 1500, 1500)),
            Quest(QuestKind.BUILD, 7, loot(4000, 2500, 2500), KBuilding.CASTLE),
            Quest(QuestKind.CONQUER, 12, loot(6000, 3000, 3000, 3000)),
            Quest(QuestKind.CONQUER, 15, loot(10000, 5000, 5000, 5000)),
        )
    }
}
