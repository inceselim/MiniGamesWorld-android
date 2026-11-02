package com.eomer91.minigamesworld.ui.games.match3

import kotlin.random.Random

/**
 * Bir tas. [id] tahta boyunca degismez (animasyonlar bunu izler).
 * [spawnRow]: yeni taslar tahtanin ustunden (negatif satir) duser.
 */
data class Gem(val id: Long, val type: Int, val spawnRow: Int = 0)

/** 8x8, satir satir; null = bos (sadece cokme sirasinda). */
typealias GemBoard = List<Gem?>

data class MatchResult(
    /** Patlayacak hucre indeksleri. */
    val cells: Set<Int>,
    /** Her dizi icin uzunluk (3, 4, 5...). L/T sekilleri iki ayri dizi sayilir. */
    val runs: List<Int>,
)

/** Match 3 kurallari. Android'e bagimli degil, unit test ile test edilir. */
class Match3Engine(
    private val random: Random = Random.Default,
    val size: Int = 8,
    val gemTypes: Int = 6,
) {
    private var nextId = 1L
    private fun newGem(type: Int, spawnRow: Int = 0) = Gem(nextId++, type, spawnRow)

    fun row(i: Int) = i / size
    fun col(i: Int) = i % size
    fun index(r: Int, c: Int) = r * size + c

    fun areAdjacent(a: Int, b: Int): Boolean =
        (row(a) == row(b) && kotlin.math.abs(col(a) - col(b)) == 1) ||
            (col(a) == col(b) && kotlin.math.abs(row(a) - row(b)) == 1)

    /** Baslangicta hazir eslesme olmayan ve en az bir hamlesi olan tahta. */
    fun newBoard(): GemBoard {
        while (true) {
            val types = IntArray(size * size)
            for (i in types.indices) {
                val r = row(i)
                val c = col(i)
                var t: Int
                do {
                    t = random.nextInt(gemTypes)
                } while (
                    (c >= 2 && types[i - 1] == t && types[i - 2] == t) ||
                    (r >= 2 && types[i - size] == t && types[i - 2 * size] == t)
                )
                types[i] = t
            }
            // Acilista taslar kendi satirlarinin bir tahta boyu ustunden duser.
            val board = types.mapIndexed { i, t -> newGem(t, spawnRow = row(i) - size) }
            if (hasPossibleMove(board)) return board
        }
    }

    fun swap(board: GemBoard, a: Int, b: Int): GemBoard =
        board.toMutableList().also { it[a] = board[b]; it[b] = board[a] }

    fun findMatches(board: GemBoard): MatchResult {
        val cells = HashSet<Int>()
        val runs = ArrayList<Int>()
        fun scan(lines: List<List<Int>>) {
            for (line in lines) {
                var start = 0
                while (start < line.size) {
                    val t = board[line[start]]?.type
                    var end = start + 1
                    while (t != null && end < line.size && board[line[end]]?.type == t) end++
                    if (t != null && end - start >= 3) {
                        runs += end - start
                        for (k in start until end) cells += line[k]
                    }
                    start = end
                }
            }
        }
        scan((0 until size).map { r -> (0 until size).map { c -> index(r, c) } })
        scan((0 until size).map { c -> (0 until size).map { r -> index(r, c) } })
        return MatchResult(cells, runs)
    }

    /**
     * Patlayan hucreleri bosaltir, ustteki taslari asagi kaydirir, bosluklari
     * yeni taslarla doldurur. Yeni taslarin [Gem.spawnRow]'u tahtanin ustundedir.
     */
    fun collapse(board: GemBoard, cleared: Set<Int>): GemBoard {
        val out = MutableList<Gem?>(size * size) { null }
        for (c in 0 until size) {
            val survivors = (size - 1 downTo 0).map { index(it, c) }.filter { it !in cleared }.mapNotNull { board[it] }
            var r = size - 1
            for (gem in survivors) {
                out[index(r, c)] = gem
                r--
            }
            val missing = r + 1
            for (k in 0..r) {
                // En alttaki yeni tas -1. satirdan, ustundekiler daha yukaridan gelir.
                out[index(k, c)] = newGem(random.nextInt(gemTypes), spawnRow = k - missing)
            }
        }
        return out
    }

    fun hasPossibleMove(board: GemBoard): Boolean = findPossibleMove(board) != null

    /** Gecerli bir takas (ipucu icin de kullanilabilir); yoksa null. */
    fun findPossibleMove(board: GemBoard): Pair<Int, Int>? {
        for (i in board.indices) {
            val right = if (col(i) < size - 1) i + 1 else -1
            val down = if (row(i) < size - 1) i + size else -1
            for (j in listOf(right, down)) {
                if (j < 0) continue
                if (findMatches(swap(board, i, j)).cells.isNotEmpty()) return i to j
            }
        }
        return null
    }

    /** Hamle kalmadiysa: ayni taslari, eslesme olmadan ve hamle olacak sekilde yeniden dizer. */
    fun shuffle(board: GemBoard): GemBoard {
        val gems = board.filterNotNull()
        repeat(200) {
            val candidate: GemBoard = gems.shuffled(random)
            if (findMatches(candidate).cells.isEmpty() && hasPossibleMove(candidate)) return candidate
        }
        return newBoard()
    }

    /** 3'lu 15, 4'lu 40, 5'li 75 ...; zincir sirasiyla carpilir. */
    fun scoreFor(match: MatchResult, chain: Int): Int =
        match.runs.sumOf { n -> 5 * n * (n - 2) } * chain

    companion object {
        const val START_MOVES = 30
        const val BONUS_MOVES = 5
    }
}
