package com.eomer91.minigamesworld.ui.games.forest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ForestItemsTest {

    @Test
    fun statsGrowWithLevelAndRarity() {
        val common = OwnedItem(1, ItemSlot.HELMET.name, Rarity.COMMON.name)
        val epic = common.copy(rarity = Rarity.EPIC.name)
        val epic5 = epic.copy(level = 5)
        val hp = { i: OwnedItem -> ForestItems.stats(i)[Stat.HP]!! }
        assertTrue(hp(epic) > hp(common))
        assertTrue(hp(epic5) > hp(epic))
        assertTrue(ForestItems.upgradeCost(epic5) > ForestItems.upgradeCost(epic))
    }

    @Test
    fun setBonusesNeedEnoughPieces() {
        val stone = ItemSlot.entries.take(4).mapIndexed { i, s -> OwnedItem(i.toLong(), s.name, Rarity.RARE.name, ItemSet.STONE.name) }
        val bonuses = ForestItems.activeSetBonuses(stone)
        assertEquals(setOf(2, 4), bonuses.map { it.second }.toSet())
        val stats = ForestItems.heroStats(stone)
        assertTrue(stats.defence > HeroStats().defence + 15f - 0.01f)
    }

    @Test
    fun rollRespectsWeightsAndSets() {
        val r = Random(9)
        repeat(200) { i ->
            val item = ForestItems.roll(i.toLong(), ChestType.ROYAL.weights, r)
            assertTrue(item.itemRarity != Rarity.COMMON)
            if (item.itemRarity == Rarity.LEGENDARY) assertTrue(item.itemSet in listOf(ItemSet.MOONSTONE, ItemSet.FIRE))
        }
        repeat(50) { i -> assertEquals(null, ForestItems.roll(i.toLong(), listOf(1f, 0f, 0f, 0f), r).itemSet) }
    }

    @Test
    fun profileEquipmentRoundTrip() {
        val item = OwnedItem(7, ItemSlot.RING.name, Rarity.LEGENDARY.name, ItemSet.FIRE.name, 3)
        val p = ForestProfile(items = listOf(item), equipped = mapOf(ItemSlot.RING.name to 7L), nextItemId = 8)
        val back = ForestProfile.fromJson(p.toJson())
        assertEquals(p, back)
        assertEquals(item, back.equippedIn(ItemSlot.RING))
    }
}
