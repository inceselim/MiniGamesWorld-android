package com.eomer91.minigamesworld.ui.games.connect4

import kotlin.random.Random

/** 0 bos, 1 birinci oyuncu (sen), 2 ikinci oyuncu (bilgisayar / arkadas). Satir 0 en ust. */
typealias C4Board = List<Int>

enum class BotLevel(val depth: Int, val winScore: Int) {
    EASY(0, 100),
    NORMAL(4, 250),
    HARD(7, 600),
}

/** Dort Bagla kurallari ve yapay zeka. Android'den bagimsiz, unit test ile test edilir. */
class Connect4Engine(private val random: Random = Random.Default) {

    fun empty(): C4Board = List(COLS * ROWS) { 0 }

    fun index(r: Int, c: Int) = r * COLS + c

    /** Taşın düşeceği satır; sütun doluysa -1. */
    fun dropRow(b: C4Board, col: Int): Int {
        for (r in ROWS - 1 downTo 0) if (b[index(r, col)] == 0) return r
        return -1
    }

    fun canPlay(b: C4Board, col: Int) = col in 0 until COLS && b[index(0, col)] == 0

    fun play(b: C4Board, col: Int, player: Int): C4Board {
        val r = dropRow(b, col)
        require(r >= 0) { "Column $col is full" }
        return b.toMutableList().also { it[index(r, col)] = player }
    }

    fun isFull(b: C4Board) = (0 until COLS).none { canPlay(b, it) }

    /** Kazanan dortlunun hucreleri; yoksa null. */
    fun winningLine(b: C4Board): List<Int>? {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = b[index(r, c)]
            if (p == 0) continue
            for ((dr, dc) in DIRECTIONS) {
                val cells = (0 until 4).map { k -> r + dr * k to c + dc * k }
                if (cells.all { (rr, cc) -> rr in 0 until ROWS && cc in 0 until COLS && b[index(rr, cc)] == p }) {
                    return cells.map { (rr, cc) -> index(rr, cc) }
                }
            }
        }
        return null
    }

    fun winner(b: C4Board): Int = winningLine(b)?.let { b[it.first()] } ?: 0

    /** Bilgisayarin hamlesi. */
    fun botMove(b: C4Board, level: BotLevel, bot: Int = 2): Int {
        val human = 3 - bot
        val legal = (0 until COLS).filter { canPlay(b, it) }
        // Her seviyede: kazanabiliyorsa kazan.
        legal.firstOrNull { winner(play(b, it, bot)) == bot }?.let { return it }

        if (level == BotLevel.EASY) {
            // Rakibin kazanacagi yeri cogu zaman engelle.
            legal.firstOrNull { winner(play(b, it, human)) == human }?.let { if (random.nextFloat() < 0.75f) return it }
            // Ortaya yakin rastgele
            val weighted = legal.flatMap { c -> List(4 - kotlin.math.abs(c - 3)) { c } }
            return weighted[random.nextInt(weighted.size)]
        }

        // Arama icin degistirilebilir dizi: her dugumde liste kopyalamak yerine oyna/geri al.
        val grid = b.toIntArray()
        var bestScore = Int.MIN_VALUE
        var best = ORDER.first { it in legal }
        for (c in ORDER) {
            if (c !in legal) continue
            val r = drop(grid, c, bot)
            val score = -negamax(grid, level.depth - 1, -WIN, WIN, human, lastRow = r, lastCol = c)
            grid[index(r, c)] = 0
            if (score > bestScore) {
                bestScore = score
                best = c
            }
        }
        return best
    }

    private fun drop(g: IntArray, col: Int, player: Int): Int {
        for (r in ROWS - 1 downTo 0) {
            if (g[index(r, col)] == 0) {
                g[index(r, col)] = player
                return r
            }
        }
        return -1
    }

    /** Son oynanan tasin 4'lu yapip yapmadigi (sadece o tasin cevresine bakar, hizli). */
    private fun makesFour(g: IntArray, r: Int, c: Int): Boolean {
        val p = g[index(r, c)]
        for ((dr, dc) in DIRECTIONS) {
            var count = 1
            var rr = r + dr
            var cc = c + dc
            while (rr in 0 until ROWS && cc in 0 until COLS && g[index(rr, cc)] == p) { count++; rr += dr; cc += dc }
            rr = r - dr
            cc = c - dc
            while (rr in 0 until ROWS && cc in 0 until COLS && g[index(rr, cc)] == p) { count++; rr -= dr; cc -= dc }
            if (count >= 4) return true
        }
        return false
    }

    /**
     * Negamax + alfa-beta budama. [player]: siradaki oyuncu. Son hamle rakibindi;
     * o hamle 4'lu yaptiysa siradaki oyuncu kaybetmistir.
     */
    private fun negamax(g: IntArray, depth: Int, alphaIn: Int, beta: Int, player: Int, lastRow: Int, lastCol: Int): Int {
        if (makesFour(g, lastRow, lastCol)) return -(WIN + depth)
        if ((0 until COLS).none { g[index(0, it)] == 0 }) return 0
        if (depth == 0) return evaluate(g.asList(), player)

        var alpha = alphaIn
        var best = -WIN * 2
        for (c in ORDER) {
            if (g[index(0, c)] != 0) continue
            val r = drop(g, c, player)
            val score = -negamax(g, depth - 1, -beta, -alpha, 3 - player, r, c)
            g[index(r, c)] = 0
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    /** Konum degerlendirmesi: [player] acisindan acik 2'li/3'luler ve orta sutun. */
    fun evaluate(b: C4Board, player: Int): Int {
        val opponent = 3 - player
        var score = 0
        for (r in 0 until ROWS) if (b[index(r, 3)] == player) score += 3 else if (b[index(r, 3)] == opponent) score -= 3
        for (r in 0 until ROWS) for (c in 0 until COLS) for ((dr, dc) in DIRECTIONS) {
            val er = r + dr * 3
            val ec = c + dc * 3
            if (er !in 0 until ROWS || ec !in 0 until COLS) continue
            var mine = 0
            var theirs = 0
            for (k in 0 until 4) {
                when (b[index(r + dr * k, c + dc * k)]) {
                    player -> mine++
                    opponent -> theirs++
                }
            }
            score += windowScore(mine, theirs) - windowScore(theirs, mine)
        }
        return score
    }

    private fun windowScore(mine: Int, theirs: Int): Int = when {
        theirs > 0 -> 0
        mine == 3 -> 50
        mine == 2 -> 10
        mine == 1 -> 1
        else -> 0
    }

    companion object {
        const val COLS = 7
        const val ROWS = 6
        private const val WIN = 1_000_000
        private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        /** Ortadan disa: alfa-beta budamayi hizlandirir. */
        private val ORDER = listOf(3, 2, 4, 1, 5, 0, 6)
    }
}
