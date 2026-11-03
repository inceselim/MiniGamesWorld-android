package com.eomer91.minigamesworld.ui.games.forest

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/*
 * Orman Muhafizi ekipman sistemi (World of Heroes'tan uyarlandi): 10 slot, 4 nadirlik, 4 set,
 * sikkeyle seviye yukseltme, 2/4/6/10 parca set bonuslari. Saf Kotlin; unit test ile test edilir.
 */

enum class Stat(@StringRes val nameRes: Int, val percent: Boolean) {
    HP(R.string.fg_s_hp, false),
    MP(R.string.fg_s_mp, false),
    ATTACK(R.string.fg_s_attack, true),
    DEFENCE(R.string.fg_s_defence, false),
    ATTACK_SPEED(R.string.fg_s_attack_speed, true),
    CRIT_CHANCE(R.string.fg_s_crit_chance, true),
    CRIT_DAMAGE(R.string.fg_s_crit_damage, true),
    MOVE_SPEED(R.string.fg_s_move_speed, false);

    /** Sıradan, 1. seviye item'daki ana stat degeri. */
    val base: Float
        get() = when (this) {
            HP -> 25f
            MP -> 15f
            ATTACK -> 4f
            DEFENCE -> 4f
            ATTACK_SPEED -> 3f
            CRIT_CHANCE -> 1.5f
            CRIT_DAMAGE -> 8f
            MOVE_SPEED -> 0.08f
        }
}

enum class ItemSlot(@StringRes val nameRes: Int, val main: Stat, val second: Stat) {
    WEAPON(R.string.fg_slot_weapon, Stat.ATTACK, Stat.CRIT_DAMAGE),
    HELMET(R.string.fg_slot_helmet, Stat.HP, Stat.DEFENCE),
    ARMOR(R.string.fg_slot_armor, Stat.DEFENCE, Stat.HP),
    BELT(R.string.fg_slot_belt, Stat.HP, Stat.MP),
    PANTS(R.string.fg_slot_pants, Stat.DEFENCE, Stat.MOVE_SPEED),
    BOOTS(R.string.fg_slot_boots, Stat.MOVE_SPEED, Stat.DEFENCE),
    CLOAK(R.string.fg_slot_cloak, Stat.MP, Stat.ATTACK_SPEED),
    NECKLACE(R.string.fg_slot_necklace, Stat.CRIT_CHANCE, Stat.ATTACK),
    EARRING(R.string.fg_slot_earring, Stat.CRIT_DAMAGE, Stat.MP),
    RING(R.string.fg_slot_ring, Stat.ATTACK_SPEED, Stat.CRIT_CHANCE),
}

enum class Rarity(@StringRes val nameRes: Int, val color: Long, val mult: Float, val maxLevel: Int, val costMult: Int) {
    COMMON(R.string.fg_r_common, 0xFFCBD5E1, 1f, 10, 1),
    RARE(R.string.fg_r_rare, 0xFF38BDF8, 1.7f, 12, 2),
    EPIC(R.string.fg_r_epic, 0xFFA66EE0, 2.6f, 15, 4),
    LEGENDARY(R.string.fg_r_legendary, 0xFFF59E0B, 4f, 20, 8),
}

enum class ItemSet(@StringRes val nameRes: Int, val color: Long, val bonuses: Map<Int, Map<Stat, Float>>) {
    FOREST(
        R.string.fg_set_forest, 0xFF4ADE80,
        mapOf(2 to mapOf(Stat.HP to 40f), 4 to mapOf(Stat.MOVE_SPEED to 0.3f), 6 to mapOf(Stat.ATTACK_SPEED to 10f), 10 to mapOf(Stat.ATTACK to 30f)),
    ),
    STONE(
        R.string.fg_set_stone, 0xFF94A3B8,
        mapOf(2 to mapOf(Stat.DEFENCE to 15f), 4 to mapOf(Stat.HP to 80f), 6 to mapOf(Stat.DEFENCE to 30f), 10 to mapOf(Stat.HP to 250f)),
    ),
    MOONSTONE(
        R.string.fg_set_moonstone, 0xFF93C5FD,
        mapOf(2 to mapOf(Stat.MP to 40f), 4 to mapOf(Stat.CRIT_CHANCE to 5f), 6 to mapOf(Stat.CRIT_DAMAGE to 30f), 10 to mapOf(Stat.ATTACK to 40f)),
    ),
    FIRE(
        R.string.fg_set_fire, 0xFFF97316,
        mapOf(
            2 to mapOf(Stat.ATTACK to 15f), 4 to mapOf(Stat.CRIT_CHANCE to 8f), 6 to mapOf(Stat.ATTACK_SPEED to 15f),
            10 to mapOf(Stat.ATTACK to 60f, Stat.CRIT_DAMAGE to 50f),
        ),
    ),
}

@Serializable
data class OwnedItem(
    val id: Long,
    val slot: String,
    val rarity: String,
    val set: String? = null,
    val level: Int = 1,
) {
    val itemSlot: ItemSlot get() = ItemSlot.valueOf(slot)
    val itemRarity: Rarity get() = Rarity.valueOf(rarity)
    val itemSet: ItemSet? get() = set?.let { runCatching { ItemSet.valueOf(it) }.getOrNull() }
    val maxed: Boolean get() = level >= itemRarity.maxLevel
}

/** Sandik turleri: fiyat (sikke) ve nadirlik agirliklari (COMMON, RARE, EPIC, LEGENDARY). */
enum class ChestType(@StringRes val nameRes: Int, val price: Int, val weights: List<Float>) {
    WOODEN(R.string.fg_chest_wooden, 150, listOf(70f, 27f, 3f, 0f)),
    ROYAL(R.string.fg_chest_royal, 600, listOf(0f, 60f, 33f, 7f)),
}

object ForestItems {
    const val INVENTORY_LIMIT = 60

    /** Item'in stat degerleri (ana stat tam, ikincil yarim). */
    fun stats(item: OwnedItem): Map<Stat, Float> {
        val k = item.itemRarity.mult * (1f + 0.15f * (item.level - 1))
        val s = item.itemSlot
        return mapOf(s.main to s.main.base * k, s.second to s.second.base * k * 0.5f)
    }

    fun upgradeCost(item: OwnedItem): Int = (20f * item.itemRarity.costMult * item.level.toFloat().pow(1.4f)).roundToInt()

    fun salvageValue(item: OwnedItem): Int = 10 * item.itemRarity.costMult * item.level

    /** Takili item'larin set parca sayilari. */
    fun setCounts(equipped: List<OwnedItem>): Map<ItemSet, Int> =
        equipped.mapNotNull { it.itemSet }.groupingBy { it }.eachCount()

    /** Aktif set bonuslari: set -> (parca esigi -> statlar). */
    fun activeSetBonuses(equipped: List<OwnedItem>): List<Triple<ItemSet, Int, Map<Stat, Float>>> =
        setCounts(equipped).flatMap { (set, n) -> set.bonuses.filterKeys { it <= n }.map { (k, v) -> Triple(set, k, v) } }

    /** Ekipman + set bonuslari -> kahraman statlari. */
    fun heroStats(equipped: List<OwnedItem>): HeroStats {
        val total = HashMap<Stat, Float>()
        equipped.forEach { item -> stats(item).forEach { (s, v) -> total[s] = (total[s] ?: 0f) + v } }
        activeSetBonuses(equipped).forEach { (_, _, bonus) -> bonus.forEach { (s, v) -> total[s] = (total[s] ?: 0f) + v } }
        val base = HeroStats()
        fun t(s: Stat) = total[s] ?: 0f
        return HeroStats(
            maxHp = base.maxHp + t(Stat.HP),
            maxMp = base.maxMp + t(Stat.MP),
            attack = base.attack + t(Stat.ATTACK),
            defence = base.defence + t(Stat.DEFENCE),
            attackSpeed = base.attackSpeed + t(Stat.ATTACK_SPEED),
            critChance = (base.critChance + t(Stat.CRIT_CHANCE)).coerceAtMost(75f),
            critDamage = base.critDamage + t(Stat.CRIT_DAMAGE),
            moveSpeed = base.moveSpeed + t(Stat.MOVE_SPEED),
        )
    }

    fun value(stats: HeroStats, s: Stat): Float = when (s) {
        Stat.HP -> stats.maxHp
        Stat.MP -> stats.maxMp
        Stat.ATTACK -> stats.attack
        Stat.DEFENCE -> stats.defence
        Stat.ATTACK_SPEED -> stats.attackSpeed
        Stat.CRIT_CHANCE -> stats.critChance
        Stat.CRIT_DAMAGE -> stats.critDamage
        Stat.MOVE_SPEED -> stats.moveSpeed
    }

    /** Agirliklara gore rastgele item uret. */
    fun roll(id: Long, weights: List<Float>, random: Random = Random.Default): OwnedItem {
        val total = weights.sum()
        var r = random.nextFloat() * total
        var rarity = Rarity.COMMON
        for ((i, w) in weights.withIndex()) {
            r -= w
            if (r <= 0f && w > 0f) {
                rarity = Rarity.entries[i]
                break
            }
        }
        val set = when (rarity) {
            Rarity.COMMON -> null
            Rarity.RARE -> listOf(ItemSet.FOREST, ItemSet.STONE).random(random)
            Rarity.EPIC -> listOf(ItemSet.STONE, ItemSet.MOONSTONE).random(random)
            Rarity.LEGENDARY -> listOf(ItemSet.MOONSTONE, ItemSet.FIRE).random(random)
        }
        return OwnedItem(id, ItemSlot.entries.random(random).name, rarity.name, set?.name)
    }

    /** Haritanin boss sandigindan dusen item nadirlik agirliklari. */
    fun bossWeights(map: ForestMap): List<Float> = when (map) {
        ForestMap.EDGE -> listOf(60f, 35f, 5f, 0f)
        ForestMap.SWAMP -> listOf(30f, 50f, 18f, 2f)
        ForestMap.MUSHROOM -> listOf(10f, 50f, 33f, 7f)
        ForestMap.VALLEY -> listOf(0f, 38f, 50f, 12f)
        ForestMap.DARK -> listOf(0f, 20f, 55f, 25f)
    }
}
