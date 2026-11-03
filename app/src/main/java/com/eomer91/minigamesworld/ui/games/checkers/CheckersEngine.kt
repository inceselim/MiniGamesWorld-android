package com.eomer91.minigamesworld.ui.games.checkers

import kotlin.math.abs
import kotlin.random.Random

/*
 * Turk Damasi kurallari:
 * - 8x8, tum kareler kullanilir. Her oyuncunun 16 tasi 2. ve 3. siradadir. Beyaz baslar.
 * - Tas bir kare ileri ya da yana gider (capraz ve geri yok). Ayni yonlerde komsu rakip tasin
 *   ustunden atlayarak alir.
 * - Dama (son siraya ulasan tas) kale gibi her yone istedigi kadar gider; aradaki tek rakip tasin
 *   ustunden atlayip arkasindaki herhangi bir bos kareye iner. Coklu almada 180 derece donemez.
 * - Almak zorunludur ve en cok tas alan hamle secilmelidir. Alinan taslar hemen kalkar.
 * - Hamlesi kalmayan (ya da tasi kalmayan) taraf kaybeder.
 *
 * Tahta: IntArray(64), indeks = satir*8 + sutun, satir 0 ustte. Beyaz altta baslar ve yukari gider.
 * 1 = beyaz tas, 2 = beyaz dama, -1 = siyah tas, -2 = siyah dama, 0 = bos.
 */

data class CheckersMove(val path: List<Int>, val captured: List<Int>) {
    val from: Int get() = path.first()
    val to: Int get() = path.last()
}

object CheckersRules {
    const val WHITE = 1
    const val BLACK = -1
    /** Tas ve dama hamlesi olmadan (sadece dama hareketi) bu kadar yarim hamle gecerse berabere. */
    const val DRAW_PLIES = 60

    fun initialBoard(): IntArray = IntArray(64) { i ->
        when (i / 8) {
            1, 2 -> -1
            5, 6 -> 1
            else -> 0
        }
    }

    private fun side(v: Int) = if (v > 0) WHITE else if (v < 0) BLACK else 0
    private fun isKing(v: Int) = abs(v) == 2

    /** Yonler: (satir, sutun) */
    private val ORTHO = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    private fun manDirs(s: Int) = listOf((if (s == WHITE) -1 else 1) to 0, 0 to -1, 0 to 1)

    private fun inside(r: Int, c: Int) = r in 0..7 && c in 0..7

    /** [s] tarafinin tum yasal hamleleri (zorunlu ve en cok alma kurali uygulanmis). */
    fun legalMoves(board: IntArray, s: Int): List<CheckersMove> {
        val captures = ArrayList<CheckersMove>()
        for (i in 0 until 64) if (side(board[i]) == s) collectCaptures(board, i, captures)
        if (captures.isNotEmpty()) {
            val best = captures.maxOf { it.captured.size }
            return captures.filter { it.captured.size == best }.distinctBy { it.path to it.captured }
        }
        val moves = ArrayList<CheckersMove>()
        for (i in 0 until 64) {
            val v = board[i]
            if (side(v) != s) continue
            val r = i / 8
            val c = i % 8
            if (isKing(v)) {
                for ((dr, dc) in ORTHO) {
                    var rr = r + dr
                    var cc = c + dc
                    while (inside(rr, cc) && board[rr * 8 + cc] == 0) {
                        moves += CheckersMove(listOf(i, rr * 8 + cc), emptyList())
                        rr += dr
                        cc += dc
                    }
                }
            } else {
                for ((dr, dc) in manDirs(s)) {
                    val rr = r + dr
                    val cc = c + dc
                    if (inside(rr, cc) && board[rr * 8 + cc] == 0) moves += CheckersMove(listOf(i, rr * 8 + cc), emptyList())
                }
            }
        }
        return moves
    }

    private fun collectCaptures(board: IntArray, from: Int, out: MutableList<CheckersMove>) {
        val v = board[from]
        val work = board.copyOf()
        work[from] = 0
        dfs(work, v, from, listOf(from), emptyList(), null, out)
    }

    /** Tasi [cur]'dan almaya devam ettir; daha fazla alma yoksa yolu kaydet. */
    private fun dfs(
        work: IntArray,
        piece: Int,
        cur: Int,
        path: List<Int>,
        captured: List<Int>,
        lastDir: Pair<Int, Int>?,
        out: MutableList<CheckersMove>,
    ) {
        val s = side(piece)
        val r = cur / 8
        val c = cur % 8
        var extended = false
        if (isKing(piece)) {
            for (d in ORTHO) {
                val (dr, dc) = d
                if (lastDir != null && dr == -lastDir.first && dc == -lastDir.second) continue
                var rr = r + dr
                var cc = c + dc
                while (inside(rr, cc) && work[rr * 8 + cc] == 0) {
                    rr += dr
                    cc += dc
                }
                if (!inside(rr, cc) || side(work[rr * 8 + cc]) != -s) continue
                val victim = rr * 8 + cc
                var lr = rr + dr
                var lc = cc + dc
                while (inside(lr, lc) && work[lr * 8 + lc] == 0) {
                    val land = lr * 8 + lc
                    val saved = work[victim]
                    work[victim] = 0
                    extended = true
                    dfs(work, piece, land, path + land, captured + victim, d, out)
                    work[victim] = saved
                    lr += dr
                    lc += dc
                }
            }
        } else {
            for (d in manDirs(s)) {
                val (dr, dc) = d
                val mr = r + dr
                val mc = c + dc
                val lr = r + 2 * dr
                val lc = c + 2 * dc
                if (!inside(lr, lc)) continue
                val victim = mr * 8 + mc
                val land = lr * 8 + lc
                if (side(work[victim]) != -s || work[land] != 0) continue
                val saved = work[victim]
                work[victim] = 0
                extended = true
                dfs(work, piece, land, path + land, captured + victim, d, out)
                work[victim] = saved
            }
        }
        if (!extended && captured.isNotEmpty()) out += CheckersMove(path, captured)
    }

    /** Hamleyi uygula (yeni tahta doner). Son siraya ulasan tas dama olur. */
    fun apply(board: IntArray, m: CheckersMove): IntArray {
        val b = board.copyOf()
        val v = b[m.from]
        b[m.from] = 0
        m.captured.forEach { b[it] = 0 }
        val row = m.to / 8
        b[m.to] = when {
            v == 1 && row == 0 -> 2
            v == -1 && row == 7 -> -2
            else -> v
        }
        return b
    }

    fun count(board: IntArray, s: Int) = board.count { side(it) == s }

    // ================================================================ Yapay zeka

    /** Pozisyon degeri: [s] tarafinin bakis acisindan. */
    fun evaluate(board: IntArray, s: Int): Int {
        var score = 0
        for (i in 0 until 64) {
            val v = board[i]
            if (v == 0) continue
            val r = i / 8
            val c = i % 8
            val value = if (isKing(v)) {
                420
            } else {
                // Ilerleyen tas degerlenir; kenar sutunlar biraz guvenli
                val adv = if (v > 0) 6 - r else r - 1
                100 + adv * 6 + (if (c == 0 || c == 7) 3 else 0)
            }
            score += if (side(v) == s) value else -value
        }
        return score
    }

    /** Arama derinligi ve rastgelelik ile en iyi hamle. */
    fun bestMove(board: IntArray, s: Int, depth: Int, random: Random = Random.Default, noise: Int = 0, maxNodes: Int = 250_000): CheckersMove? {
        nodes = 0
        nodeLimit = maxNodes
        val moves = legalMoves(board, s)
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()
        var best: CheckersMove? = null
        var bestScore = Int.MIN_VALUE
        var alpha = -INF
        for (m in moves.shuffled(random)) {
            val score = -negamax(apply(board, m), -s, depth - 1, -INF, -alpha) + (if (noise > 0) random.nextInt(noise + 1) else 0)
            if (score > bestScore) {
                bestScore = score
                best = m
            }
            if (score > alpha) alpha = score
        }
        return best
    }

    private const val INF = 1_000_000
    // Telefonda uzun dusunmeyi onlemek icin dugum siniri (yalnizca tek is parcaciginda cagrilir)
    private var nodes = 0
    private var nodeLimit = Int.MAX_VALUE

    private fun negamax(board: IntArray, s: Int, depth: Int, alphaIn: Int, beta: Int): Int {
        if (++nodes > nodeLimit) return evaluate(board, s)
        val moves = legalMoves(board, s)
        if (moves.isEmpty()) return -INF / 2 - depth // kaybeden taraf; erken kazanci tercih et
        // Alma zinciri ortasinda kesmemek icin alma varsa biraz daha derine bak
        if (depth <= 0 && moves.first().captured.isEmpty()) return evaluate(board, s)
        if (depth <= -4) return evaluate(board, s)
        var alpha = alphaIn
        var best = -INF
        for (m in moves) {
            val score = -negamax(apply(board, m), -s, depth - 1, -beta, -alpha)
            if (score > best) best = score
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }
        return best
    }
}
