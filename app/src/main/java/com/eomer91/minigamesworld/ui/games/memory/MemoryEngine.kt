package com.eomer91.minigamesworld.ui.games.memory

import kotlin.random.Random

data class MemoryCard(
    val id: Int,
    val symbol: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false,
)

/** Hafiza kartlari kurallari. Android'e bagimli degil, unit test ile test edilir. */
class MemoryEngine(private val random: Random = Random.Default) {

    val pairCount: Int get() = SYMBOLS.size

    /** Her sembolden iki kart, adil (Fisher-Yates) karistirilmis. */
    fun newDeck(): List<MemoryCard> =
        (SYMBOLS + SYMBOLS).shuffled(random).mapIndexed { index, symbol -> MemoryCard(index, symbol) }

    fun flipUp(cards: List<MemoryCard>, id: Int): List<MemoryCard> =
        cards.map { if (it.id == id) it.copy(isFaceUp = true) else it }

    fun flipDown(cards: List<MemoryCard>, ids: Collection<Int>): List<MemoryCard> =
        cards.map { if (it.id in ids) it.copy(isFaceUp = false) else it }

    fun markMatched(cards: List<MemoryCard>, ids: Collection<Int>): List<MemoryCard> =
        cards.map { if (it.id in ids) it.copy(isMatched = true) else it }

    fun isPair(cards: List<MemoryCard>, a: Int, b: Int): Boolean =
        a != b && cards.first { it.id == a }.symbol == cards.first { it.id == b }.symbol

    fun allMatched(cards: List<MemoryCard>): Boolean = cards.all { it.isMatched }

    /**
     * Hamle sayisini skora cevirir (rekor tablosunda "buyuk daha iyi" olsun diye).
     * Kusursuz oyun (her denemede eslesme) = 1000 puan; her fazla hamle 40 puan dusurur.
     */
    fun scoreFor(moves: Int): Int =
        (MAX_SCORE - (moves - pairCount).coerceAtLeast(0) * PENALTY_PER_EXTRA_MOVE).coerceAtLeast(MIN_SCORE)

    companion object {
        val SYMBOLS = listOf("🍎", "🍌", "🍇", "🍓", "🍒", "🍉", "🍍", "🥝")
        const val COLUMNS = 4
        const val MAX_SCORE = 1000
        const val MIN_SCORE = 50
        const val PENALTY_PER_EXTRA_MOVE = 40
    }
}
