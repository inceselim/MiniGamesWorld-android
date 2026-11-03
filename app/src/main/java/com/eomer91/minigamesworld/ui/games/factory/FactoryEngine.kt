package com.eomer91.minigamesworld.ui.games.factory

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/*
 * Fabrika Patronu: uretim zinciri + pazar (Android'den bagimsiz; birim test).
 */

enum class Product(val emoji: String, val basePrice: Double, /** Fiyati yariya dusuren dakikalik satis */ val market: Double) {
    ORE("⛏️", 3.0, 900.0),
    WOOD("🌲", 3.0, 900.0),
    COTTON("🌱", 3.0, 900.0),
    IRON("🔩", 10.0, 400.0),
    PLANK("🪵", 8.0, 400.0),
    FABRIC("🧵", 9.0, 400.0),
    TOOLS("🔧", 30.0, 160.0),
    FURNITURE("🪑", 48.0, 110.0),
    CLOTHES("👕", 28.0, 160.0),
    CAR("🚗", 260.0, 25.0),
}

enum class Building(
    val emoji: String,
    val unlockCost: Double,
    /** Seviye basina saniyelik uretim dongusu */
    val rate: Double,
    val inputs: Map<Product, Int>,
    val output: Product,
) {
    MINE("⛰️", 0.0, 1.0, emptyMap(), Product.ORE),
    LUMBER("🪓", 120.0, 1.0, emptyMap(), Product.WOOD),
    FARM("🚜", 300.0, 1.0, emptyMap(), Product.COTTON),
    SMELTER("🔥", 700.0, 0.5, mapOf(Product.ORE to 2), Product.IRON),
    SAWMILL("🪚", 1_000.0, 0.6, mapOf(Product.WOOD to 2), Product.PLANK),
    WEAVER("🧶", 1_600.0, 0.5, mapOf(Product.COTTON to 2), Product.FABRIC),
    WORKSHOP("🛠️", 4_500.0, 0.25, mapOf(Product.IRON to 1, Product.PLANK to 1), Product.TOOLS),
    TAILOR("🧵", 7_000.0, 0.3, mapOf(Product.FABRIC to 2), Product.CLOTHES),
    CARPENTER("🪑", 11_000.0, 0.2, mapOf(Product.PLANK to 3, Product.FABRIC to 1), Product.FURNITURE),
    CAR_PLANT("🏭", 60_000.0, 0.05, mapOf(Product.IRON to 4, Product.TOOLS to 2, Product.FABRIC to 1), Product.CAR),
    ;

    /** Seviye [level] -> [level]+1 maliyeti (ilk acilis unlockCost). */
    fun upgradeCost(level: Int): Double = if (level == 0) unlockCost else max(unlockCost, 40.0) * 0.7 * 1.55.pow(level)
}

enum class BuildingStatus { IDLE, RUNNING, NO_INPUT, FULL }

@Serializable
data class FactoryOrder(val product: Product, val qty: Int, val reward: Double, val timeLeft: Double)

@Serializable
data class FactorySave(
    val version: Int = 1,
    val cash: Double = 60.0,
    val levels: Map<Building, Int> = mapOf(Building.MINE to 1),
    val stock: Map<Product, Double> = emptyMap(),
    val demand: Map<Product, Double> = emptyMap(),
    val autoSell: Set<Product> = emptySet(),
    val warehouse: Int = 0,
    val totalEarned: Double = 0.0,
    val orders: List<FactoryOrder> = emptyList(),
    val savedAt: Long = 0L,
)

/** Gecici pazar olayi: [product] fiyati [mult] carpanla [timeLeft] saniye. */
data class MarketEvent(val product: Product, val mult: Double, var timeLeft: Double)

class FactoryEngine(save: FactorySave = FactorySave(), private val random: Random = Random.Default) {
    var cash = save.cash
        private set
    val levels: MutableMap<Building, Int> = Building.entries.associateWith { save.levels[it] ?: 0 }.toMutableMap()
    val stock: MutableMap<Product, Double> = Product.entries.associateWith { save.stock[it] ?: 0.0 }.toMutableMap()
    /** Talep carpani (1 = normal) */
    val demand: MutableMap<Product, Double> = Product.entries.associateWith { save.demand[it] ?: 1.0 }.toMutableMap()
    val autoSell: MutableSet<Product> = save.autoSell.toMutableSet()
    var warehouse = save.warehouse
        private set
    var totalEarned = save.totalEarned
        private set
    val orders: MutableList<FactoryOrder> = save.orders.toMutableList()
    val status: MutableMap<Building, BuildingStatus> = Building.entries.associateWith { BuildingStatus.IDLE }.toMutableMap()
    private val progress: MutableMap<Building, Double> = Building.entries.associateWith { 0.0 }.toMutableMap()
    var event: MarketEvent? = null
        private set
    private var eventTimer = 60.0
    private var orderTimer = 20.0
    private var sellTimer = 0.0
    /** Son 10 saniyelik gelir (gelir/dk gostergesi) */
    private val incomeWindow = ArrayDeque<Pair<Double, Double>>()
    private var clock = 0.0

    /** Olaylar (ses vb.) */
    var soldEvents = 0
        private set
    var orderDone = 0
        private set

    val capacity: Double get() = 100.0 * 2.0.pow(warehouse)
    val warehouseCost: Double get() = 400.0 * 2.6.pow(warehouse)

    fun price(p: Product): Double = p.basePrice * (demand[p] ?: 1.0) * (event?.takeIf { it.product == p }?.mult ?: 1.0)

    fun level(b: Building) = levels[b] ?: 0

    /** Saniyelik net urun akisi (girdi eksikse dusuk olabilir; tahmini). */
    fun outputPerSec(b: Building): Double = b.rate * level(b)

    val incomePerMinute: Double
        get() {
            val total = incomeWindow.sumOf { it.second }
            val span = (clock - (incomeWindow.firstOrNull()?.first ?: clock)).coerceAtLeast(1.0)
            return total / span * 60.0
        }

    // ================================================================ Islemler

    fun canAfford(cost: Double) = cash >= cost - 1e-9

    fun upgrade(b: Building): Boolean {
        val cost = b.upgradeCost(level(b))
        if (!canAfford(cost)) return false
        cash -= cost
        levels[b] = level(b) + 1
        return true
    }

    fun upgradeWarehouse(): Boolean {
        val cost = warehouseCost
        if (!canAfford(cost)) return false
        cash -= cost
        warehouse++
        return true
    }

    fun toggleAutoSell(p: Product) {
        if (!autoSell.remove(p)) autoSell += p
    }

    /** Urunun tamamini sat; kazanci dondurur. */
    fun sell(p: Product, qty: Double = stock[p] ?: 0.0): Double {
        val q = min(qty, stock[p] ?: 0.0)
        if (q < 1.0) return 0.0
        val whole = kotlin.math.floor(q)
        var earned = 0.0
        // Satis fiyati duser: ortalama fiyatla hesapla
        val before = demand[p] ?: 1.0
        val after = (before * 0.5.pow(whole / p.market)).coerceAtLeast(MIN_DEMAND)
        val avg = (before + after) / 2
        earned = whole * p.basePrice * avg * (event?.takeIf { it.product == p }?.mult ?: 1.0)
        demand[p] = after
        stock[p] = (stock[p] ?: 0.0) - whole
        earn(earned)
        soldEvents++
        return earned
    }

    fun deliver(i: Int): Boolean {
        val o = orders.getOrNull(i) ?: return false
        if ((stock[o.product] ?: 0.0) < o.qty) return false
        stock[o.product] = (stock[o.product] ?: 0.0) - o.qty
        orders.removeAt(i)
        earn(o.reward)
        orderDone++
        return true
    }

    private fun earn(x: Double) {
        cash += x
        totalEarned += x
        incomeWindow.addLast(clock to x)
    }

    // ================================================================ Zaman

    fun update(dt: Double) {
        clock += dt
        produce(dt)
        // Talep yavasca normale doner
        for (p in Product.entries) {
            val d = demand[p] ?: 1.0
            demand[p] = d + (1.0 - d) * min(1.0, dt * RECOVER)
        }
        // Otomatik satis (saniyede bir)
        sellTimer -= dt
        if (sellTimer <= 0) {
            sellTimer = 1.0
            autoSell.forEach { p -> if ((stock[p] ?: 0.0) >= 1.0) sell(p) }
        }
        // Pazar olaylari
        event?.let {
            it.timeLeft -= dt
            if (it.timeLeft <= 0) event = null
        }
        eventTimer -= dt
        if (eventTimer <= 0) {
            eventTimer = 70.0 + random.nextDouble() * 60.0
            val unlocked = unlockedProducts()
            if (unlocked.isNotEmpty()) {
                val p = unlocked.random(random)
                event = MarketEvent(p, if (random.nextDouble() < 0.7) 1.8 else 0.6, 45.0)
            }
        }
        // Siparisler
        for (i in orders.indices.reversed()) {
            val o = orders[i]
            val t = o.timeLeft - dt
            if (t <= 0) orders.removeAt(i) else orders[i] = o.copy(timeLeft = t)
        }
        orderTimer -= dt
        if (orderTimer <= 0) {
            orderTimer = 45.0 + random.nextDouble() * 30.0
            if (orders.size < MAX_ORDERS) newOrder()?.let { orders += it }
        }
        while (incomeWindow.isNotEmpty() && clock - incomeWindow.first().first > 30.0) incomeWindow.removeFirst()
    }

    private fun produce(dt: Double) {
        // Ust zincirden alta: hammadde once uretilsin
        for (b in Building.entries) {
            val lvl = level(b)
            if (lvl == 0) {
                status[b] = BuildingStatus.IDLE
                continue
            }
            var prog = (progress[b] ?: 0.0) + b.rate * lvl * dt
            var st = BuildingStatus.RUNNING
            while (prog >= 1.0) {
                if ((stock[b.output] ?: 0.0) + 1 > capacity) {
                    st = BuildingStatus.FULL
                    prog = 1.0
                    break
                }
                if (b.inputs.any { (p, n) -> (stock[p] ?: 0.0) < n }) {
                    st = BuildingStatus.NO_INPUT
                    prog = 1.0
                    break
                }
                b.inputs.forEach { (p, n) -> stock[p] = (stock[p] ?: 0.0) - n }
                stock[b.output] = (stock[b.output] ?: 0.0) + 1
                prog -= 1.0
            }
            progress[b] = prog
            status[b] = st
        }
    }

    fun unlockedProducts(): List<Product> = Building.entries.filter { level(it) > 0 }.map { it.output }.distinct()

    private fun newOrder(): FactoryOrder? {
        val candidates = Building.entries.filter { level(it) > 0 && it.inputs.isNotEmpty() }.ifEmpty { Building.entries.filter { level(it) > 0 } }
        if (candidates.isEmpty()) return null
        val b = candidates.random(random)
        val perMin = outputPerSec(b) * 60
        val qty = max(5, (perMin * (1.2 + random.nextDouble() * 1.3)).toInt())
        val reward = qty * b.output.basePrice * (1.8 + random.nextDouble() * 0.6)
        return FactoryOrder(b.output, qty, reward, 180.0 + random.nextDouble() * 120.0)
    }

    /**
     * Cevrimdisi kazanc: kapaliyken gecen [seconds] (en fazla 2 saat) yari verimle simule edilir;
     * otomatik satilan urunler para getirir. Kazanilan parayi dondurur.
     */
    fun offline(seconds: Double): Double {
        val s = min(seconds, MAX_OFFLINE)
        if (s < 30) return 0.0
        val before = totalEarned
        var t = 0.0
        val step = max(1.0, s / 600.0)
        while (t < s) {
            update(step * 0.5)
            t += step
        }
        orders.clear()
        event = null
        return totalEarned - before
    }

    /** Odullu reklam: cevrimdisi kazanci ikiye katla. */
    fun bonus(x: Double) = earn(x)

    fun save(now: Long) = FactorySave(
        cash = cash,
        levels = levels.filterValues { it > 0 },
        stock = stock.filterValues { it > 0 },
        demand = demand.filterValues { kotlin.math.abs(it - 1.0) > 0.001 },
        autoSell = autoSell.toSet(),
        warehouse = warehouse,
        totalEarned = totalEarned,
        orders = orders.toList(),
        savedAt = now,
    )

    companion object {
        const val MIN_DEMAND = 0.25
        const val RECOVER = 0.02
        const val MAX_ORDERS = 3
        const val MAX_OFFLINE = 2 * 3600.0
    }
}
