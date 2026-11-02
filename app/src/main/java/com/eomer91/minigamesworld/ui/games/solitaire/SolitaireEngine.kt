package com.eomer91.minigamesworld.ui.games.solitaire

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** [suit]: 0 maca, 1 kupa, 2 karo, 3 sinek. [rank]: 1 (As) .. 13 (Papaz). */
@Serializable
data class Card(val id: Int, val suit: Int, val rank: Int, val faceUp: Boolean = false) {
    val isRed: Boolean get() = suit == 1 || suit == 2
}

@Serializable
sealed interface Pile {
    @Serializable data object Stock : Pile
    @Serializable data object Waste : Pile
    @Serializable data class Foundation(val index: Int) : Pile
    @Serializable data class Tableau(val index: Int) : Pile
}

/** [fromIndex]: tableau'da tasinan dizinin ilk kartinin indeksi; diger yiginlarda ust kart. */
data class Move(val from: Pile, val fromIndex: Int, val to: Pile)

@Serializable
data class SolitaireGame(
    val stock: List<Card>,
    val waste: List<Card>,
    val foundations: List<List<Card>>,
    val tableau: List<List<Card>>,
    val drawCount: Int,
    val score: Int = 0,
    val moves: Int = 0,
) {
    fun pile(p: Pile): List<Card> = when (p) {
        Pile.Stock -> stock
        Pile.Waste -> waste
        is Pile.Foundation -> foundations[p.index]
        is Pile.Tableau -> tableau[p.index]
    }

    val isWon: Boolean get() = foundations.all { it.size == 13 }
}

/** Klondike kurallari. Android'den bagimsiz, unit test ile test edilir. */
class SolitaireEngine(private val random: Random = Random.Default) {

    fun deal(drawCount: Int): SolitaireGame {
        val deck = (0 until 52).map { Card(it, it / 13, it % 13 + 1) }.shuffled(random).toMutableList()
        val tableau = (0 until 7).map { col ->
            List(col + 1) { k -> deck.removeAt(deck.lastIndex).copy(faceUp = k == col) }
        }
        return SolitaireGame(
            stock = deck,
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = tableau,
            drawCount = drawCount,
        )
    }

    /** Desteden kart cek; deste bossa acik kartlari geri cevir. */
    fun draw(g: SolitaireGame): SolitaireGame {
        if (g.stock.isEmpty()) {
            if (g.waste.isEmpty()) return g
            val penalty = if (g.drawCount == 1) RECYCLE_PENALTY else 0
            return g.copy(
                stock = g.waste.reversed().map { it.copy(faceUp = false) },
                waste = emptyList(),
                score = (g.score - penalty).coerceAtLeast(0),
                moves = g.moves + 1,
            )
        }
        val n = minOf(g.drawCount, g.stock.size)
        val drawn = g.stock.takeLast(n).reversed().map { it.copy(faceUp = true) }
        return g.copy(stock = g.stock.dropLast(n), waste = g.waste + drawn, moves = g.moves + 1)
    }

    fun canPlaceOnFoundation(card: Card, foundation: List<Card>): Boolean {
        val top = foundation.lastOrNull()
        return if (top == null) card.rank == 1 else top.suit == card.suit && card.rank == top.rank + 1
    }

    fun canPlaceOnTableau(card: Card, column: List<Card>): Boolean {
        val top = column.lastOrNull()
        return if (top == null) card.rank == 13 else top.faceUp && top.isRed != card.isRed && card.rank == top.rank - 1
    }

    fun isValid(g: SolitaireGame, m: Move): Boolean {
        if (m.from == m.to) return false
        val source = g.pile(m.from)
        if (m.fromIndex !in source.indices) return false
        val moving = source.subList(m.fromIndex, source.size)
        if (moving.any { !it.faceUp }) return false
        // Tableau disindan (deste/acik/hedef) sadece ust kart tasinir
        if (m.from !is Pile.Tableau && moving.size != 1) return false
        return when (val to = m.to) {
            is Pile.Foundation -> moving.size == 1 && canPlaceOnFoundation(moving.first(), g.foundations[to.index])
            is Pile.Tableau -> canPlaceOnTableau(moving.first(), g.tableau[to.index])
            else -> false
        }
    }

    fun apply(g: SolitaireGame, m: Move): SolitaireGame {
        if (!isValid(g, m)) return g
        val source = g.pile(m.from)
        val moving = source.subList(m.fromIndex, source.size)
        var remaining = source.subList(0, m.fromIndex)
        var points = 0

        // Tableau'da alttaki kapali kart acilir
        if (m.from is Pile.Tableau && remaining.isNotEmpty() && !remaining.last().faceUp) {
            remaining = remaining.dropLast(1) + remaining.last().copy(faceUp = true)
            points += 5
        }
        points += when {
            m.to is Pile.Foundation && m.from !is Pile.Foundation -> 10
            m.from is Pile.Waste && m.to is Pile.Tableau -> 5
            m.from is Pile.Foundation && m.to is Pile.Tableau -> -15
            else -> 0
        }

        var next = g.withPile(m.from, remaining)
        next = next.withPile(m.to, next.pile(m.to) + moving)
        return next.copy(score = (g.score + points).coerceAtLeast(0), moves = g.moves + 1)
    }

    /** Dokunarak oynama: karti/diziyi gidebilecegi en iyi yere goturur (yoksa null). */
    fun bestMove(g: SolitaireGame, from: Pile, fromIndex: Int): Move? {
        val source = g.pile(from)
        val isTop = fromIndex == source.lastIndex
        if (isTop && from !is Pile.Foundation) {
            (0 until 4).map { Move(from, fromIndex, Pile.Foundation(it)) }.firstOrNull { isValid(g, it) }?.let { return it }
        }
        val tableauTargets = (0 until 7).map { Move(from, fromIndex, Pile.Tableau(it)) }.filter { isValid(g, it) }
        // Dolu bir sutun once; bos sutuna sadece anlamliysa (sutunun en altinda degilse)
        tableauTargets.firstOrNull { g.tableau[(it.to as Pile.Tableau).index].isNotEmpty() }?.let { return it }
        val alreadyAtBottom = from is Pile.Tableau && fromIndex == 0
        if (!alreadyAtBottom) tableauTargets.firstOrNull()?.let { return it }
        return null
    }

    /** Ipucu: yapilabilecek faydali bir hamle; yoksa null (o zaman desteden cekmek onerilir). */
    fun hint(g: SolitaireGame): Move? {
        val candidates = ArrayList<Move>()
        // 1) Hedefe gidebilenler
        g.tableau.forEachIndexed { i, col -> if (col.isNotEmpty()) candidates += Move(Pile.Tableau(i), col.lastIndex, Pile.Foundation(0)) }
        if (g.waste.isNotEmpty()) candidates += Move(Pile.Waste, g.waste.lastIndex, Pile.Foundation(0))
        for (c in candidates) {
            bestMove(g, c.from, c.fromIndex)?.takeIf { it.to is Pile.Foundation }?.let { return it }
        }
        // 2) Kapali kart acan tableau hamleleri
        g.tableau.forEachIndexed { i, col ->
            val first = col.indexOfFirst { it.faceUp }
            if (first > 0) bestMove(g, Pile.Tableau(i), first)?.takeIf { it.to is Pile.Tableau }?.let { return it }
        }
        // 3) Acik karti tableau'ya
        if (g.waste.isNotEmpty()) bestMove(g, Pile.Waste, g.waste.lastIndex)?.let { return it }
        return null
    }

    /** Tum kartlar acik ve deste/acik kart bitti: kalanlar otomatik bitirilebilir. */
    fun canAutoComplete(g: SolitaireGame) =
        !g.isWon && g.stock.isEmpty() && g.waste.isEmpty() && g.tableau.all { col -> col.all { it.faceUp } }

    /** Otomatik bitirme icin bir sonraki hedef hamlesi. */
    fun nextFoundationMove(g: SolitaireGame): Move? {
        g.tableau.forEachIndexed { i, col ->
            if (col.isNotEmpty()) {
                (0 until 4).map { Move(Pile.Tableau(i), col.lastIndex, Pile.Foundation(it)) }.firstOrNull { isValid(g, it) }?.let { return it }
            }
        }
        return null
    }

    /** Kazaninca sure bonusu. */
    fun winBonus(elapsedSeconds: Int) = WIN_BONUS + (TIME_BONUS_SECONDS - elapsedSeconds).coerceAtLeast(0)

    private fun SolitaireGame.withPile(p: Pile, cards: List<Card>): SolitaireGame = when (p) {
        Pile.Stock -> copy(stock = cards)
        Pile.Waste -> copy(waste = cards)
        is Pile.Foundation -> copy(foundations = foundations.mapIndexed { i, f -> if (i == p.index) cards else f })
        is Pile.Tableau -> copy(tableau = tableau.mapIndexed { i, t -> if (i == p.index) cards else t })
    }

    companion object {
        const val RECYCLE_PENALTY = 100
        const val WIN_BONUS = 500
        const val TIME_BONUS_SECONDS = 600
    }
}
