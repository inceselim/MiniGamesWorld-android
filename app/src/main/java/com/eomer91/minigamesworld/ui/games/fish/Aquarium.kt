package com.eomer91.minigamesworld.ui.games.fish

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Hunger { FULL, HUNGRY, STARVING }

/** Akvaryum susleri: kozmetik + inci uretimine yuzdesel bonus. */
enum class Decoration(@StringRes val nameRes: Int, val cost: Int, val bonus: Float) {
    PLANT(R.string.fish_deco_plant, 60, 0.05f),
    CORAL(R.string.fish_deco_coral, 150, 0.08f),
    CASTLE(R.string.fish_deco_castle, 400, 0.12f),
    TREASURE(R.string.fish_deco_treasure, 900, 0.20f),
}

/**
 * Akvaryum kurallari (saf fonksiyonlar, zaman parametre olarak gelir; unit test ile test edilir).
 * Baliklar olmez: cok ac kalan balik sadece inci uretmeyi birakir.
 */
object Aquarium {
    const val MINUTE = 60_000L
    const val HOUR = 60 * MINUTE

    const val MAX_FOOD = 10
    const val FOOD_REFILL_MS = 6 * MINUTE
    const val AD_FOOD = 10

    /** Bu sure icinde yeniden yemez. */
    const val FULL_MS = 45 * MINUTE
    /** Bu sureden fazla ac kalan inci uretmez. */
    const val STARVING_MS = 12 * HOUR
    /** Toplanmadan en fazla bu kadar saatlik inci birikir. */
    const val MAX_ACCRUAL_HOURS = 8f

    const val GROWTH_PER_MEAL = 0.1f

    const val CAPACITY_STEP = 4
    const val MAX_CAPACITY = 40

    fun hunger(f: OwnedFish, now: Long): Hunger {
        val since = now - f.lastFedAt
        return when {
            since < FULL_MS -> Hunger.FULL
            since < STARVING_MS -> Hunger.HUNGRY
            else -> Hunger.STARVING
        }
    }

    fun canEat(f: OwnedFish, now: Long) = hunger(f, now) != Hunger.FULL

    fun species(f: OwnedFish): FishSpecies = FishSpecies.entries.firstOrNull { it.name == f.species } ?: FishSpecies.SARDINE

    fun decorationBonus(p: FishProfile): Float =
        Decoration.entries.filter { it.name in p.decorations }.sumOf { it.bonus.toDouble() }.toFloat()

    /** Saatlik inci uretimi. */
    fun pearlRate(f: OwnedFish, bonus: Float): Float = species(f).value * 0.08f * (0.5f + f.growth) * (1f + bonus)

    /** Biriken (henuz toplanmamis) inci, kesirli. */
    fun pending(f: OwnedFish, now: Long, bonus: Float): Float {
        val end = minOf(now, f.lastFedAt + STARVING_MS)
        val hours = ((end - f.lastPearlAt) / HOUR.toFloat()).coerceIn(0f, MAX_ACCRUAL_HOURS)
        return hours * pearlRate(f, bonus)
    }

    fun pendingTotal(p: FishProfile, now: Long): Int {
        val bonus = decorationBonus(p)
        return p.aquarium.sumOf { floor(pending(it, now, bonus)).toInt() }
    }

    /** Tum biriken inciyi topla (tam sayi kismi; kesirler kaybolur). */
    fun collect(p: FishProfile, now: Long): Pair<FishProfile, Int> {
        val total = pendingTotal(p, now)
        if (total <= 0) return p to 0
        val fish = p.aquarium.map { it.copy(lastPearlAt = now) }
        return p.copy(pearls = p.pearls + total, aquarium = fish) to total
    }

    /** Baligi besle. Cok ac kalmis balikta birikmis sure sifirlanir (ac kaldigi zaman uretmez). */
    fun feed(p: FishProfile, fishId: Long, now: Long): FishProfile = p.copy(
        aquarium = p.aquarium.map { f ->
            if (f.id != fishId || !canEat(f, now)) f
            else {
                // Ac kaldigi donemin uretimini koru, sonra saati yeniden baslat
                val starvingSince = f.lastFedAt + STARVING_MS
                val pearlAt = if (f.lastPearlAt < starvingSince && now > starvingSince) {
                    // Uretimin durdugu andan bugune "bos" gecen sureyi atla
                    now - (starvingSince - f.lastPearlAt).coerceAtLeast(0L)
                } else f.lastPearlAt
                f.copy(growth = (f.growth + GROWTH_PER_MEAL).coerceAtMost(1f), lastFedAt = now, lastPearlAt = pearlAt)
            }
        },
    )

    fun sellPrice(f: OwnedFish): Int = (species(f).value * (2f + f.growth * 4f)).roundToInt()

    fun sell(p: FishProfile, fishId: Long): Pair<FishProfile, Int> {
        val f = p.aquarium.firstOrNull { it.id == fishId } ?: return p to 0
        val price = sellPrice(f)
        return p.copy(pearls = p.pearls + price, aquarium = p.aquarium - f) to price
    }

    /** Yem kabini gecen sureye gore doldur. */
    fun refillFood(p: FishProfile, now: Long): FishProfile {
        if (p.food >= MAX_FOOD) return if (p.foodUpdatedAt == now) p else p.copy(foodUpdatedAt = now)
        if (p.foodUpdatedAt <= 0L || p.foodUpdatedAt > now) return p.copy(foodUpdatedAt = now)
        val gained = ((now - p.foodUpdatedAt) / FOOD_REFILL_MS).toInt()
        if (gained <= 0) return p
        val food = (p.food + gained).coerceAtMost(MAX_FOOD)
        val updated = if (food >= MAX_FOOD) now else p.foodUpdatedAt + gained * FOOD_REFILL_MS
        return p.copy(food = food, foodUpdatedAt = updated)
    }

    /** Bir sonraki yeme kalan ms (kap doluysa null). */
    fun nextFoodIn(p: FishProfile, now: Long): Long? =
        if (p.food >= MAX_FOOD) null else (FOOD_REFILL_MS - (now - p.foodUpdatedAt)).coerceAtLeast(0L)

    fun useFood(p: FishProfile, now: Long): FishProfile? {
        val r = refillFood(p, now)
        if (r.food <= 0) return null
        // Kap doluyken ilk yem kullanilinca dolum saati simdi baslar
        return r.copy(food = r.food - 1, foodUpdatedAt = if (r.food >= MAX_FOOD) now else r.foodUpdatedAt)
    }

    fun addFood(p: FishProfile, amount: Int, now: Long): FishProfile {
        val r = refillFood(p, now)
        // Reklamla kap kapasitesini asabilir (en fazla 2 kat)
        return r.copy(food = (r.food + amount).coerceAtMost(MAX_FOOD * 2))
    }

    fun capacityLevel(p: FishProfile) = (p.aquariumCapacity - FishProfile.START_CAPACITY) / CAPACITY_STEP
    fun capacityCost(p: FishProfile): Int? =
        if (p.aquariumCapacity >= MAX_CAPACITY) null else (120 * 1.7.pow(capacityLevel(p))).roundToInt()
}

// ---------------------------------------------------------------- Tank simulasyonu (sadece gorsel)

data class TankFish(
    val id: Long,
    val species: FishSpecies,
    val growth: Float,
    val x: Float,
    val y: Float,
    val targetX: Float,
    val targetY: Float,
    val dir: Int,
    val hungry: Boolean,
)

data class Pellet(val id: Long, val x: Float, val y: Float, val age: Float = 0f)

data class TankState(val fish: List<TankFish> = emptyList(), val pellets: List<Pellet> = emptyList(), val time: Float = 0f)

/** Akvaryumdaki yuzme/yem kovalama. Kalici veri tutmaz; yeme olaylarini dondurur. */
class TankSim(private val random: Random = Random.Default) {
    private var nextPellet = 1L

    fun build(p: FishProfile, now: Long, old: TankState? = null): TankState {
        val prev = old?.fish?.associateBy { it.id } ?: emptyMap()
        val fish = p.aquarium.map { f ->
            val hungry = Aquarium.canEat(f, now)
            prev[f.id]?.copy(growth = f.growth, hungry = hungry) ?: run {
                val x = 1f + random.nextFloat() * (WIDTH - 2f)
                val y = 0.8f + random.nextFloat() * (FLOOR - 1.6f)
                TankFish(f.id, Aquarium.species(f), f.growth, x, y, x, y, if (random.nextBoolean()) 1 else -1, hungry)
            }
        }
        return TankState(fish, old?.pellets ?: emptyList(), old?.time ?: 0f)
    }

    fun drop(s: TankState, x: Float) = s.copy(pellets = s.pellets + Pellet(nextPellet++, x.coerceIn(0.3f, WIDTH - 0.3f), 0.2f))

    /** Balik boyu (birim): buyudukce irilesir. */
    fun length(f: TankFish) = f.species.size * (0.9f + f.growth * 0.8f)

    fun fishAt(s: TankState, x: Float, y: Float): TankFish? = s.fish.lastOrNull { f ->
        val l = length(f)
        abs(f.x - x) < l * 0.6f && abs(f.y - y) < l * 0.45f
    }

    /** @return yeni durum ve yemek yiyen baliklarin id'leri */
    fun update(s: TankState, dt: Float): Pair<TankState, List<Long>> {
        val eaten = ArrayList<Long>()
        // Yemler batar, dipte bir sure kalip kaybolur
        var pellets = s.pellets.mapNotNull { pl ->
            val y = (pl.y + PELLET_SINK * dt).coerceAtMost(FLOOR - 0.1f)
            val age = pl.age + dt
            if (age > PELLET_LIFE) null else pl.copy(y = y, age = age)
        }
        val taken = HashSet<Long>()
        val fish = s.fish.map { f ->
            val target = if (f.hungry) pellets.filter { it.id !in taken }.minByOrNull { hypot(it.x - f.x, it.y - f.y) } else null
            var tx = f.targetX
            var ty = f.targetY
            var speed = WANDER_SPEED * (0.6f + f.species.speed * 0.25f)
            if (target != null) {
                tx = target.x; ty = target.y; speed *= 2.4f
            } else if (hypot(tx - f.x, ty - f.y) < 0.2f || random.nextFloat() < dt * 0.15f) {
                tx = 0.8f + random.nextFloat() * (WIDTH - 1.6f)
                ty = 0.7f + random.nextFloat() * (FLOOR - 1.4f)
            }
            val dx = tx - f.x
            val dy = ty - f.y
            val dist = hypot(dx, dy)
            val step = minOf(dist, speed * dt)
            val nx = if (dist > 0f) f.x + dx / dist * step else f.x
            val ny = if (dist > 0f) f.y + dy / dist * step * 0.7f else f.y
            val dir = if (abs(dx) > 0.05f) (if (dx > 0) 1 else -1) else f.dir
            var next = f.copy(x = nx, y = ny, targetX = tx, targetY = ty, dir = dir)
            if (target != null && hypot(target.x - nx, target.y - ny) < EAT_RADIUS + length(f) * 0.3f) {
                taken += target.id
                eaten += f.id
                next = next.copy(hungry = false, targetX = nx, targetY = ny)
            }
            next
        }
        pellets = pellets.filter { it.id !in taken }
        return s.copy(fish = fish, pellets = pellets, time = s.time + dt) to eaten
    }

    companion object {
        const val WIDTH = 10f
        const val HEIGHT = 6f
        /** Kum zemininin ust siniri. */
        const val FLOOR = 5.3f
        const val PELLET_SINK = 0.9f
        const val PELLET_LIFE = 14f
        const val WANDER_SPEED = 0.9f
        const val EAT_RADIUS = 0.25f
    }
}
