package com.eomer91.minigamesworld.ui.games.countries

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Serializable
data class Country(
    val id: Int,
    val name: String,
    val isBot: Boolean,
    val alive: Boolean = true,
    val polity: String = Polity.DEMOCRACY.name,
    /** Premium bitince donulecek yonetim bicimi. */
    val basePolity: String = polity,
    val premiumTurns: Int = 0,
    val polityChangedTurn: Int = -100,
    val focus: String = Focus.POPULATION.name,
    val taxRate: Int = 10,
    val population: Int = 3500,
    val gold: Int = 1800,
    val wood: Int = 500,
    val clay: Int = 400,
    val iron: Int = 300,
    val gems: Int = 0,
    val buildings: Map<String, Int> = emptyMap(),
    val army: Map<String, Int> = emptyMap(),
) {
    fun level(b: Building) = buildings[b.name] ?: 0
    fun count(u: UnitType) = army[u.name] ?: 0
    val pol: Polity get() = Polity.valueOf(polity)
    val foc: Focus get() = Focus.valueOf(focus)
    val soldiersPop: Int get() = UnitType.entries.sumOf { count(it) * it.pop }
    val barracksCapacity: Int get() = level(Building.BARRACKS) * CountriesEngine.BARRACKS_CAPACITY
    val farmCapacity: Int get() = level(Building.FARM) * CountriesEngine.FARM_CAPACITY
    fun has(r: Res) = gold >= r.gold && wood >= r.wood && clay >= r.clay && iron >= r.iron && gems >= r.gems
    fun pay(r: Res) = copy(gold = gold - r.gold, wood = wood - r.wood, clay = clay - r.clay, iron = iron - r.iron, gems = gems - r.gems)
    fun withArmy(u: UnitType, n: Int) = copy(army = army + (u.name to max(0, n)))
    fun withBuilding(b: Building, n: Int) = copy(buildings = buildings + (b.name to n))
}

enum class LogType { BOT_ATTACK_WON, BOT_ATTACK_LOST, YOU_WON, YOU_LOST, SHORTAGE, PREMIUM_ENDED, PEACE_OVER, BOT_BUILT_CASTLE }

@Serializable
data class LogEntry(val turn: Int, val type: String, val who: String = "", val a: Int = 0, val b: Int = 0) {
    val logType: LogType get() = LogType.valueOf(type)
}

@Serializable
data class BattleReport(
    val attacker: String,
    val defender: String,
    val attackPower: Int,
    val defencePower: Int,
    val won: Boolean,
    val attackerLosses: Map<String, Int>,
    val defenderLosses: Map<String, Int>,
    val goldTaken: Int,
    val popTaken: Int,
)

@Serializable
data class CwGame(
    val version: Int = 1,
    val turn: Int = 1,
    val difficulty: String,
    val player: Country,
    val bots: List<Country>,
    val log: List<LogEntry> = emptyList(),
    /** null: suruyor, true: zafer, false: yenilgi */
    val victory: Boolean? = null,
    val lastBattle: BattleReport? = null,
) {
    val diff: CwDifficulty get() = CwDifficulty.valueOf(difficulty)
    val over: Boolean get() = victory != null
    val peace: Boolean get() = turn <= CountriesEngine.PEACE_TURNS
    val defeatedBots: Int get() = bots.count { !it.alive }

    val score: Int
        get() = when (victory) {
            true -> 1000 * bots.size + max(0, 3000 - turn * 15) + player.population / 20
            else -> 300 * defeatedBots + turn * 5
        }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): CwGame? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
    }
}

sealed interface CwResult {
    data class Ok(val game: CwGame) : CwResult
    data object NotEnoughResources : CwResult
    data object NoBarracksSpace : CwResult
    data object NotEnoughPopulation : CwResult
    data object MaxLevel : CwResult
    data object PeaceTime : CwResult
    data object Cooldown : CwResult
    data object Invalid : CwResult
}

/** Ulkeler Savasi kurallari. Android'den bagimsiz, unit test ile test edilir. */
class CountriesEngine(private val random: Random = Random.Default) {

    // ------------------------------------------------------------ Kurulum

    private val startBuildings = mapOf(
        Building.PARLIAMENT to 3, Building.CASTLE to 1, Building.BARRACKS to 3, Building.FARM to 4,
        Building.MINE to 2, Building.WOODCUTTER to 2, Building.BRICKHOUSE to 2, Building.TRADE_CENTER to 5, Building.AVM to 1,
    ).mapKeys { it.key.name }

    private val startArmy = mapOf(
        UnitType.SPEARMAN to 10, UnitType.BOWMAN to 10, UnitType.SWORDMAN to 5,
        UnitType.AXEMAN to 5, UnitType.KNIGHT to 44, UnitType.CATAPULT to 2,
    )

    fun newGame(difficulty: CwDifficulty, name: String, polity: Polity): CwGame {
        val player = Country(
            id = 0,
            name = name.ifBlank { "?" },
            isBot = false,
            polity = polity.name,
            basePolity = polity.name,
            buildings = startBuildings,
            army = startArmy.mapKeys { it.key.name },
        )
        val names = BotNames.shuffled(random)
        val bots = List(difficulty.bots) { i ->
            val k = difficulty.botPower * (0.85f + random.nextFloat() * 0.3f)
            val focus = Focus.entries.random(random)
            val pol = listOf(Polity.MONARCHY, Polity.ARISTOCRACY, Polity.THEOCRACY, Polity.DICTATORSHIP, Polity.DEMOCRACY).random(random)
            Country(
                id = i + 1,
                name = names[i % names.size],
                isBot = true,
                polity = pol.name,
                basePolity = pol.name,
                focus = focus.name,
                population = 3000,
                gold = (1200 * k).toInt(),
                wood = (400 * k).toInt(),
                clay = (400 * k).toInt(),
                iron = (400 * k).toInt(),
                buildings = startBuildings + (Building.CASTLE.name to (1 + (k * 1.2f).toInt())),
                army = startArmy.mapKeys { it.key.name }.mapValues { (it.value * k * (0.7f + random.nextFloat() * 0.6f)).toInt() },
            )
        }
        return CwGame(difficulty = difficulty.name, player = player, bots = bots)
    }

    // ------------------------------------------------------------ Hesaplar

    private fun focusBonus(c: Country, f: Focus) = if (c.foc == f) FOCUS_BONUS else 0f

    fun attackMultiplier(c: Country) = 1f + c.pol.attack + focusBonus(c, Focus.ATTACK)
    fun defenceMultiplier(c: Country) = (1f + c.pol.defence + focusBonus(c, Focus.DEFENCE)) * (1f + CASTLE_BONUS * c.level(Building.CASTLE))

    fun attackPower(c: Country, units: Map<String, Int> = c.army): Int =
        (UnitType.entries.sumOf { (units[it.name] ?: 0) * it.attack } * attackMultiplier(c)).toInt()

    fun defencePower(c: Country): Int {
        val units = UnitType.entries.sumOf { c.count(it) * it.defence }
        val walls = c.level(Building.CASTLE) * CASTLE_DEFENCE + c.level(Building.TOWER) * TOWER_DEFENCE
        return ((units + walls) * defenceMultiplier(c)).toInt()
    }

    /** Meclis verimi: nufus meclis kapasitesini asarsa vergi geliri duser. */
    fun parliamentEfficiency(c: Country): Float {
        val over = c.population - c.level(Building.PARLIAMENT) * PARLIAMENT_CAPACITY
        return if (over <= 0) 1f else max(0.5f, 1f - 0.05f * over / 100f)
    }

    fun buildCost(c: Country, b: Building): Res =
        if (b.cost.gems > 0) b.cost else b.cost * (1f - c.pol.buildPrice)

    /** Tur basina net gelir (bakim dusulmus). */
    fun turnIncome(c: Country): Res {
        val incomeBonus = c.pol.income + focusBonus(c, Focus.INCOME)
        var gold = c.population / 1000f * 30f * (c.taxRate / 100f) * parliamentEfficiency(c)
        var wood = 0
        var clay = 0
        var iron = 0
        Building.entries.forEach { b ->
            val l = c.level(b)
            gold += b.income.gold * l
            wood += b.income.wood * l - b.upkeep.wood * l
            clay += b.income.clay * l - b.upkeep.clay * l
            iron += b.income.iron * l - b.upkeep.iron * l
        }
        val upkeepGold = Building.entries.sumOf { it.upkeep.gold * c.level(it) }
        UnitType.entries.forEach { u ->
            val n = c.count(u)
            wood -= u.upkeep.wood * n
            clay -= u.upkeep.clay * n
            iron -= u.upkeep.iron * n
        }
        return Res((gold * (1f + incomeBonus)).toInt() - upkeepGold, wood, clay, iron)
    }

    fun nextPopulation(c: Country): Int {
        val cap = c.farmCapacity
        if (c.population >= cap) return cap
        if (c.population < MIN_POP) return MIN_POP
        val bonus = c.pol.population + focusBonus(c, Focus.POPULATION)
        val taxFactor = 0.006f - ((c.taxRate - 10) / 80f) * 0.005f
        val growth = c.population * taxFactor + c.population * 0.004f * bonus
        return min(cap, ceil(c.population + growth).toInt())
    }

    // ------------------------------------------------------------ Tur

    fun tick(g: CwGame): CwGame {
        if (g.over) return g
        val turn = g.turn + 1
        val log = ArrayList<LogEntry>()
        var battle: BattleReport? = null

        var player = economy(g.player, turn, log)
        val bots = g.bots.map { bot ->
            if (!bot.alive) return@map bot
            var b = economy(bot, turn, null)
            b = botAct(b)
            b
        }.toMutableList()

        if (turn == PEACE_TURNS + 1) log += LogEntry(turn, LogType.PEACE_OVER.name)

        // Botlar oyuncuya saldirabilir (baris donemi bittikten sonra)
        if (turn > PEACE_TURNS) {
            for (i in bots.indices) {
                val bot = bots[i]
                if (!bot.alive || !player.alive) continue
                if (random.nextFloat() >= g.diff.aggression) continue
                val sent = bot.army.mapValues { (it.value * BOT_ATTACK_SHARE).toInt() }
                if (attackPower(bot, sent) < defencePower(player) * 0.6f) continue // umutsuz saldiri yapmaz
                val (att, def, report) = battle(bot, player, sent)
                bots[i] = att
                player = def
                battle = report
                log += LogEntry(turn, (if (report.won) LogType.BOT_ATTACK_WON else LogType.BOT_ATTACK_LOST).name, bot.name, report.attackPower, report.defencePower)
            }
        }

        val victory = when {
            !player.alive -> false
            bots.none { it.alive } -> true
            else -> null
        }
        return g.copy(
            turn = turn,
            player = player,
            bots = bots,
            log = (log.reversed() + g.log).take(MAX_LOG),
            victory = victory,
            lastBattle = battle ?: g.lastBattle,
        )
    }

    private fun economy(c0: Country, turn: Int, log: MutableList<LogEntry>?): Country {
        var c = c0
        val inc = turnIncome(c)
        val gold = c.gold + inc.gold
        val wood = c.wood + inc.wood
        val clay = c.clay + inc.clay
        val iron = c.iron + inc.iron
        val shortage = gold < 0 || wood < 0 || clay < 0 || iron < 0
        c = c.copy(gold = max(0, gold), wood = max(0, wood), clay = max(0, clay), iron = max(0, iron), population = nextPopulation(c))
        if (shortage) {
            // Kitlik: her birlikten %3 kacar
            var lost = 0
            UnitType.entries.forEach { u ->
                val n = c.count(u)
                val d = if (n > 0) max(1, (n * 0.03f).toInt()) else 0
                lost += d
                c = c.withArmy(u, n - d)
            }
            if (lost > 0) log?.add(LogEntry(turn, LogType.SHORTAGE.name, a = lost))
        }
        if (c.premiumTurns > 0) {
            val left = c.premiumTurns - 1
            c = if (left == 0) {
                log?.add(LogEntry(turn, LogType.PREMIUM_ENDED.name))
                c.copy(premiumTurns = 0, polity = c.basePolity)
            } else c.copy(premiumTurns = left)
        }
        return c
    }

    /** Basit bot yapay zekasi: kapasite dolunca ciftlik, yoksa en dusuk binayi kur; eksik birlikten asker al. */
    private fun botAct(b0: Country): Country {
        var b = b0
        val inc = turnIncome(b)
        val build = when {
            // Eksi veren kaynagin uretim binasini once kur (kitliktan asker kacmasin)
            inc.iron < 0 -> Building.MINE
            inc.clay < 0 -> Building.BRICKHOUSE
            inc.wood < 0 -> Building.WOODCUTTER
            inc.gold < 0 -> Building.TRADE_CENTER
            b.population >= b.farmCapacity - 200 -> Building.FARM
            b.soldiersPop >= b.barracksCapacity - 10 -> Building.BARRACKS
            else -> listOf(
                Building.FARM, Building.MINE, Building.WOODCUTTER, Building.BRICKHOUSE, Building.TRADE_CENTER,
                Building.CASTLE, Building.TOWER, Building.PARLIAMENT,
            ).minBy { b.level(it) }
        }
        val cost = buildCost(b, build)
        if (b.level(build) < build.maxLevel && b.has(cost)) b = b.pay(cost).withBuilding(build, b.level(build) + 1)

        // Kaynak eksi veriyorsa asker almaz (bakimi karsilayamaz)
        if (inc.wood < 0 || inc.clay < 0 || inc.iron < 0) return b

        // Odak saldiriysa saldiri birliklerini, savunmaysa savunma birliklerini tercih eder
        val pool = when (b.foc) {
            Focus.ATTACK -> listOf(UnitType.KNIGHT, UnitType.AXEMAN, UnitType.CATAPULT, UnitType.SWORDMAN)
            Focus.DEFENCE -> listOf(UnitType.SWORDMAN, UnitType.SPEARMAN, UnitType.BOWMAN, UnitType.KNIGHT)
            else -> UnitType.entries
        }
        val unit = pool.minBy { b.count(it) * it.pop }
        repeat(BOT_RECRUIT) {
            if (b.has(unit.cost) && b.soldiersPop + unit.pop <= b.barracksCapacity && b.population - unit.pop >= MIN_POP) {
                b = b.pay(unit.cost).withArmy(unit, b.count(unit) + 1).copy(population = b.population - unit.pop)
            }
        }
        return b
    }

    /**
     * Savas. Kazanan saldirgan, gonderdigi birliklerin bir kismini kaybeder; savunan yok olur ve
     * nufus/altininin %10-30'u saldirgana gecer. Kaybeden saldirgan gonderdigi tum birlikleri kaybeder.
     */
    fun battle(attacker: Country, defender: Country, sent: Map<String, Int>): Triple<Country, Country, BattleReport> {
        val a = attackPower(attacker, sent)
        val d = defencePower(defender)
        val won = a > d
        val attackerLosses = HashMap<String, Int>()
        val defenderLosses: Map<String, Int>
        var att = attacker
        var def = defender
        var goldTaken = 0
        var popTaken = 0
        if (won) {
            val ratio = (d.toFloat() / a).coerceIn(0f, 1f) * 0.6f
            sent.forEach { (u, n) -> attackerLosses[u] = ceil(n * ratio).toInt().coerceAtMost(n) }
            defenderLosses = defender.army.filterValues { it > 0 }
            val share = 0.1f + random.nextFloat() * 0.2f
            goldTaken = (defender.gold * share).toInt()
            popTaken = (defender.population * share).toInt()
            att = att.copy(gold = att.gold + goldTaken, population = att.population + popTaken)
            def = def.copy(alive = false, gold = def.gold - goldTaken, population = def.population - popTaken)
        } else {
            sent.forEach { (u, n) -> if (n > 0) attackerLosses[u] = n }
            defenderLosses = unitLoss(defender, (a * 0.5f).toInt())
            defenderLosses.forEach { (u, n) -> def = def.withArmy(UnitType.valueOf(u), def.count(UnitType.valueOf(u)) - n) }
        }
        attackerLosses.forEach { (u, n) -> att = att.withArmy(UnitType.valueOf(u), att.count(UnitType.valueOf(u)) - n) }
        val report = BattleReport(attacker.name, defender.name, a, d, won, attackerLosses.filterValues { it > 0 }, defenderLosses, goldTaken, popTaken)
        return Triple(att, def, report)
    }

    /** Savunanin kayiplari: zayif birliklerden baslayarak [lossPower] kadar savunma gucu. */
    private fun unitLoss(c: Country, lossPower: Int): Map<String, Int> {
        var left = lossPower
        val result = HashMap<String, Int>()
        for (u in UnitType.entries) {
            if (left <= 0) break
            val n = c.count(u)
            val lost = min(n, left / u.defence)
            if (lost > 0) result[u.name] = lost
            left -= lost * u.defence
            if (lost < n) break
        }
        return result
    }

    // ------------------------------------------------------------ Oyuncu eylemleri

    private fun withPlayer(g: CwGame, p: Country) = CwResult.Ok(g.copy(player = p))

    fun build(g: CwGame, b: Building): CwResult {
        val p = g.player
        if (p.level(b) >= b.maxLevel) return CwResult.MaxLevel
        val cost = buildCost(p, b)
        if (!p.has(cost)) return CwResult.NotEnoughResources
        return withPlayer(g, p.pay(cost).withBuilding(b, p.level(b) + 1))
    }

    fun recruit(g: CwGame, u: UnitType, n: Int): CwResult {
        if (n <= 0) return CwResult.Invalid
        val p = g.player
        if (p.soldiersPop + u.pop * n > p.barracksCapacity) return CwResult.NoBarracksSpace
        if (p.population - u.pop * n < MIN_POP) return CwResult.NotEnoughPopulation
        val cost = Res(u.cost.gold * n, u.cost.wood * n, u.cost.clay * n, u.cost.iron * n)
        if (!p.has(cost)) return CwResult.NotEnoughResources
        return withPlayer(g, p.pay(cost).withArmy(u, p.count(u) + n).copy(population = p.population - u.pop * n))
    }

    /** Kaynak/kisla/nufusa gore en fazla alinabilecek asker. */
    fun maxRecruit(p: Country, u: UnitType): Int {
        var n = (p.barracksCapacity - p.soldiersPop) / u.pop
        n = min(n, (p.population - MIN_POP) / u.pop)
        if (u.cost.gold > 0) n = min(n, p.gold / u.cost.gold)
        if (u.cost.wood > 0) n = min(n, p.wood / u.cost.wood)
        if (u.cost.clay > 0) n = min(n, p.clay / u.cost.clay)
        if (u.cost.iron > 0) n = min(n, p.iron / u.cost.iron)
        return max(0, n)
    }

    fun setTax(g: CwGame, rate: Int) = g.copy(player = g.player.copy(taxRate = rate.coerceIn(MIN_TAX, MAX_TAX)))
    fun setFocus(g: CwGame, f: Focus) = g.copy(player = g.player.copy(focus = f.name))

    fun setPolity(g: CwGame, pol: Polity): CwResult {
        val p = g.player
        if (pol == Polity.PREMIUM) return CwResult.Invalid
        if (p.premiumTurns > 0) return CwResult.Cooldown
        if (g.turn - p.polityChangedTurn < POLITY_COOLDOWN) return CwResult.Cooldown
        if (p.gold < POLITY_CHANGE_GOLD) return CwResult.NotEnoughResources
        return withPlayer(g, p.copy(polity = pol.name, basePolity = pol.name, polityChangedTurn = g.turn, gold = p.gold - POLITY_CHANGE_GOLD))
    }

    fun activatePremium(g: CwGame): CwResult {
        val p = g.player
        if (p.premiumTurns > 0) return CwResult.Cooldown
        if (p.gems < PREMIUM_GEMS) return CwResult.NotEnoughResources
        return withPlayer(g, p.copy(polity = Polity.PREMIUM.name, premiumTurns = PREMIUM_TURNS, gems = p.gems - PREMIUM_GEMS))
    }

    fun addGems(g: CwGame, n: Int) = g.copy(player = g.player.copy(gems = g.player.gems + n))

    /** Pazar: [lots] x 100 kaynak sat (70 altin) ya da 70 altina 70 kaynak al. */
    fun trade(g: CwGame, r: Resource, lots: Int, selling: Boolean): CwResult {
        if (r == Resource.GOLD || lots <= 0) return CwResult.Invalid
        val p = g.player
        val have = when (r) { Resource.WOOD -> p.wood; Resource.CLAY -> p.clay; else -> p.iron }
        val next = if (selling) {
            val amount = SELL_LOT * lots
            if (have < amount) return CwResult.NotEnoughResources
            p.copy(gold = p.gold + SELL_PRICE * lots).let { setRes(it, r, have - amount) }
        } else {
            val price = BUY_LOT * lots
            if (p.gold < price) return CwResult.NotEnoughResources
            p.copy(gold = p.gold - price).let { setRes(it, r, have + BUY_LOT * lots) }
        }
        return withPlayer(g, next)
    }

    private fun setRes(c: Country, r: Resource, v: Int) = when (r) {
        Resource.WOOD -> c.copy(wood = v)
        Resource.CLAY -> c.copy(clay = v)
        Resource.IRON -> c.copy(iron = v)
        Resource.GOLD -> c.copy(gold = v)
    }

    fun attack(g: CwGame, botId: Int, sent: Map<String, Int>): CwResult {
        if (g.over) return CwResult.Invalid
        if (g.peace) return CwResult.PeaceTime
        val idx = g.bots.indexOfFirst { it.id == botId && it.alive }
        if (idx < 0) return CwResult.Invalid
        val clean = sent.filter { (u, n) -> n > 0 && n <= g.player.count(UnitType.valueOf(u)) }
        if (clean.isEmpty()) return CwResult.Invalid
        val (att, def, report) = battle(g.player, g.bots[idx], clean)
        val bots = g.bots.toMutableList().also { it[idx] = def }
        val entry = LogEntry(g.turn, (if (report.won) LogType.YOU_WON else LogType.YOU_LOST).name, def.name, report.attackPower, report.defencePower)
        val victory = if (bots.none { it.alive }) true else null
        return CwResult.Ok(g.copy(player = att, bots = bots, lastBattle = report, log = (listOf(entry) + g.log).take(MAX_LOG), victory = victory))
    }

    companion object {
        const val PEACE_TURNS = 10
        const val TURN_SECONDS = 5f
        const val FOCUS_BONUS = 0.2f
        const val CASTLE_BONUS = 0.01f
        const val CASTLE_DEFENCE = 500
        const val TOWER_DEFENCE = 300
        const val PARLIAMENT_CAPACITY = 1000
        const val FARM_CAPACITY = 1000
        const val BARRACKS_CAPACITY = 100
        const val MIN_POP = 2500
        const val MIN_TAX = 10
        const val MAX_TAX = 90
        const val POLITY_COOLDOWN = 20
        const val POLITY_CHANGE_GOLD = 500
        const val PREMIUM_GEMS = 5
        const val PREMIUM_TURNS = 5
        const val AD_GEMS = 3
        const val SELL_LOT = 100
        const val SELL_PRICE = 70
        const val BUY_LOT = 70
        const val BOT_RECRUIT = 5
        const val BOT_ATTACK_SHARE = 0.6f
        const val MAX_LOG = 30
    }
}
