package com.eomer91.minigamesworld.ui.games.cooking

import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random

/*
 * Pasta Dukkani kurallari (Android'den bagimsiz).
 */

enum class Flavor(val sponge: Long, val emoji: String) {
    VANILLA(0xFFF6D98B, "🍦"),
    CHOCOLATE(0xFF7B4A2A, "🍫"),
    STRAWBERRY(0xFFF7A8B8, "🍓"),
}

enum class Ingredient(val emoji: String, val flavor: Flavor? = null) {
    FLOUR("🌾"),
    SUGAR("🍬"),
    EGGS("🥚"),
    MILK("🥛"),
    VANILLA("🍦", Flavor.VANILLA),
    COCOA("🍫", Flavor.CHOCOLATE),
    STRAWBERRY("🍓", Flavor.STRAWBERRY),
    ;

    companion object {
        val BASE = listOf(FLOUR, SUGAR, EGGS, MILK)
    }
}

enum class Frosting(val color: Long) {
    WHITE(0xFFFFFBF2),
    PINK(0xFFFFA6C9),
    CHOCOLATE(0xFF5A3420),
    MINT(0xFFA7F3D0),
    LEMON(0xFFFFE680),
    BLUE(0xFF9AD0F5),
}

enum class Topping(val emoji: String) {
    STRAWBERRY("🍓"),
    CHERRY("🍒"),
    CANDLE("🕯️"),
    CHOCO("🍫"),
    STAR("⭐"),
    FLOWER("🌸"),
}

enum class CakeStep { INGREDIENTS, MIX, BAKE, FROST, DECORATE, SERVE }

enum class BakeResult { RAW, UNDER, PERFECT, OK, BURNT }

data class CakeOrder(val flavor: Flavor, val frosting: Frosting, val toppings: Set<Topping>)

data class PlacedTopping(val topping: Topping, val x: Float, val y: Float)

data class CakeWork(
    val added: List<Ingredient> = emptyList(),
    val mistakes: Int = 0,
    val mix: Float = 0f,
    val bake: BakeResult? = null,
    val frosting: Frosting? = null,
    /** Kremanin yayildigi hucreler (0..1) */
    val frostCells: List<Float> = List(Cake.FROST_CELLS) { 0f },
    val toppings: List<PlacedTopping> = emptyList(),
) {
    /** Hamurun rengi: eklenen aroma */
    val flavor: Flavor? get() = added.firstNotNullOfOrNull { it.flavor }
    val frostCoverage: Float get() = frostCells.average().toFloat()
}

object Cake {
    const val FROST_CELLS = 12
    /** Karistirma: bu kadar tur tamamlaninca hamur hazir. */
    const val MIX_TURNS = 5f
    const val BAKE_SECONDS = 5f
    const val PERFECT_FROM = 0.62f
    const val PERFECT_TO = 0.80f

    fun randomOrder(random: Random = Random.Default): CakeOrder {
        val n = 1 + random.nextInt(2)
        return CakeOrder(
            flavor = Flavor.entries.random(random),
            frosting = Frosting.entries.random(random),
            toppings = Topping.entries.shuffled(random).take(n).toSet(),
        )
    }

    /** Malzeme ekle; ayni malzemeyi tekrar eklemek ya da ikinci bir aroma eklemek hata sayilir. */
    fun add(work: CakeWork, ing: Ingredient, order: CakeOrder): CakeWork {
        val wrong = ing in work.added ||
            (ing.flavor != null && (ing.flavor != order.flavor || work.flavor != null))
        return if (wrong) work.copy(mistakes = work.mistakes + 1) else work.copy(added = work.added + ing)
    }

    fun ingredientsDone(work: CakeWork): Boolean = Ingredient.BASE.all { it in work.added } && work.flavor != null

    /** Karistirma: [dAngle] radyan. 0..1 ilerleme */
    fun stir(work: CakeWork, dAngle: Float): CakeWork =
        work.copy(mix = (work.mix + abs(dAngle) / (2f * PI.toFloat() * MIX_TURNS)).coerceAtMost(1f))

    /** Firindan cikarma anindaki ilerleme (0..1) -> sonuc */
    fun bakeResult(progress: Float): BakeResult = when {
        progress < 0.4f -> BakeResult.RAW
        progress < PERFECT_FROM -> BakeResult.UNDER
        progress <= PERFECT_TO -> BakeResult.PERFECT
        progress <= 0.9f -> BakeResult.OK
        else -> BakeResult.BURNT
    }

    /** Krema surme: hucre indeksi ve yol uzunlugu. */
    fun spread(work: CakeWork, cell: Int, dist: Float): CakeWork {
        if (cell !in 0 until FROST_CELLS || work.frosting == null) return work
        val cells = work.frostCells.toMutableList()
        cells[cell] = (cells[cell] + dist * 0.02f).coerceAtMost(1f)
        return work.copy(frostCells = cells)
    }

    fun place(work: CakeWork, t: Topping, x: Float, y: Float): CakeWork =
        if (work.toppings.size >= 14) work else work.copy(toppings = work.toppings + PlacedTopping(t, x, y))

    /** 0..1 puan */
    fun rate(order: CakeOrder, work: CakeWork): Float {
        var s = 0f
        val total = 11f
        if (work.flavor == order.flavor) s += 2f
        s += when (work.mistakes) {
            0 -> 1f
            1 -> 0.5f
            else -> 0f
        }
        s += work.mix
        s += when (work.bake) {
            BakeResult.PERFECT -> 2f
            BakeResult.OK, BakeResult.UNDER -> 1f
            else -> 0f
        }
        if (work.frosting == order.frosting) s += 2f
        s += (work.frostCoverage / 0.8f).coerceAtMost(1f)
        // Istenen susler: her biri en az 2 tane
        val counts = work.toppings.groupingBy { it.topping }.eachCount()
        val wanted = order.toppings.sumOf { t -> ((counts[t] ?: 0).coerceAtMost(2)) } / (order.toppings.size * 2f)
        s += 2f * wanted
        val extra = work.toppings.count { it.topping !in order.toppings }
        if (extra > 2) s -= 0.5f
        return (s / total).coerceIn(0f, 1f)
    }

    fun stars(rating: Float): Int = when {
        rating >= 0.88f -> 3
        rating >= 0.7f -> 2
        rating >= 0.45f -> 1
        else -> 0
    }
}
