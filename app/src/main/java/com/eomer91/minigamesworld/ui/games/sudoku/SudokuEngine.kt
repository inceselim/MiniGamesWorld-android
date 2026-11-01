package com.eomer91.minigamesworld.ui.games.sudoku

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlin.random.Random

/**
 * Zorluk: kac ipucu (dolu hucre) birakilacagi, taban puan ve "hedef sure".
 * Hedef sureyi asan her saniye 1 puan dusurur.
 */
enum class SudokuDifficulty(
    @StringRes val labelRes: Int,
    val clues: Int,
    val baseScore: Int,
    val parSeconds: Int,
) {
    EASY(R.string.sudoku_easy, clues = 40, baseScore = 600, parSeconds = 360),
    NORMAL(R.string.sudoku_normal, clues = 32, baseScore = 1200, parSeconds = 720),
    HARD(R.string.sudoku_hard, clues = 26, baseScore = 2400, parSeconds = 1200),
}

/** 81 elemanli, satir satir; 0 = bos. */
data class SudokuPuzzle(val givens: List<Int>, val solution: List<Int>)

object SudokuRules {
    const val SIZE = 9
    const val CELLS = 81

    fun row(i: Int) = i / SIZE
    fun col(i: Int) = i % SIZE
    fun box(i: Int) = (row(i) / 3) * 3 + col(i) / 3

    /** Ayni satir, sutun ya da 3x3 kutudaki hucreler (kendisi haric). */
    fun peers(i: Int): List<Int> = (0 until CELLS).filter {
        it != i && (row(it) == row(i) || col(it) == col(i) || box(it) == box(i))
    }

    fun rowCells(r: Int) = (0 until SIZE).map { r * SIZE + it }
    fun colCells(c: Int) = (0 until SIZE).map { it * SIZE + c }
    fun boxCells(b: Int): List<Int> {
        val r0 = (b / 3) * 3
        val c0 = (b % 3) * 3
        return (0 until 9).map { (r0 + it / 3) * SIZE + c0 + it % 3 }
    }

    /** Tamamen dolu ve kurallara uygun mu? */
    fun isValidSolution(grid: List<Int>): Boolean {
        if (grid.size != CELLS || grid.any { it !in 1..9 }) return false
        val units = (0 until SIZE).flatMap { listOf(rowCells(it), colCells(it), boxCells(it)) }
        return units.all { unit -> unit.map { grid[it] }.toSet().size == SIZE }
    }
}

/**
 * Rastgele, TEK cozumlu Sudoku uretir.
 * 1) Bos tahtayi rastgele siralarla geri izleme (backtracking) ile doldurur.
 * 2) Hucreleri rastgele sirayla silmeye calisir; silince cozum sayisi 1'den fazla oluyorsa geri koyar.
 * Cozucu bit maskeleri ve "en az aday" (MRV) sezgisi kullanir, telefonda milisaniyeler surer.
 */
class SudokuGenerator(private val random: Random = Random.Default) {

    fun generate(difficulty: SudokuDifficulty): SudokuPuzzle {
        val solution = IntArray(SudokuRules.CELLS)
        fill(solution)

        val puzzle = solution.copyOf()
        var filled = SudokuRules.CELLS
        for (i in (0 until SudokuRules.CELLS).shuffled(random)) {
            if (filled <= difficulty.clues) break
            val backup = puzzle[i]
            puzzle[i] = 0
            if (countSolutions(puzzle.copyOf(), limit = 2) != 1) {
                puzzle[i] = backup
            } else {
                filled--
            }
        }
        return SudokuPuzzle(puzzle.toList(), solution.toList())
    }

    /** Cozum sayisini [limit]'e kadar sayar (tekillik kontrolu icin limit = 2 yeter). */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val masks = Masks.from(grid) ?: return 0
        return countRec(grid, masks, limit)
    }

    private fun fill(grid: IntArray): Boolean {
        val masks = Masks.from(grid) ?: return false
        return fillRec(grid, masks)
    }

    private fun fillRec(grid: IntArray, m: Masks): Boolean {
        val cell = m.bestEmptyCell(grid) ?: return true
        val candidates = m.candidates(cell).shuffled(random)
        for (v in candidates) {
            m.set(grid, cell, v)
            if (fillRec(grid, m)) return true
            m.clear(grid, cell, v)
        }
        return false
    }

    private fun countRec(grid: IntArray, m: Masks, limit: Int): Int {
        val cell = m.bestEmptyCell(grid) ?: return 1
        var count = 0
        for (v in m.candidates(cell)) {
            m.set(grid, cell, v)
            count += countRec(grid, m, limit - count)
            m.clear(grid, cell, v)
            if (count >= limit) break
        }
        return count
    }

    /** Satir/sutun/kutu icin kullanilan rakamlarin bit maskeleri. */
    private class Masks(val rows: IntArray, val cols: IntArray, val boxes: IntArray) {
        fun used(i: Int) = rows[SudokuRules.row(i)] or cols[SudokuRules.col(i)] or boxes[SudokuRules.box(i)]

        fun candidates(i: Int): List<Int> {
            val u = used(i)
            return (1..9).filter { u and (1 shl it) == 0 }
        }

        fun set(grid: IntArray, i: Int, v: Int) {
            grid[i] = v
            val bit = 1 shl v
            rows[SudokuRules.row(i)] = rows[SudokuRules.row(i)] or bit
            cols[SudokuRules.col(i)] = cols[SudokuRules.col(i)] or bit
            boxes[SudokuRules.box(i)] = boxes[SudokuRules.box(i)] or bit
        }

        fun clear(grid: IntArray, i: Int, v: Int) {
            grid[i] = 0
            val bit = (1 shl v).inv()
            rows[SudokuRules.row(i)] = rows[SudokuRules.row(i)] and bit
            cols[SudokuRules.col(i)] = cols[SudokuRules.col(i)] and bit
            boxes[SudokuRules.box(i)] = boxes[SudokuRules.box(i)] and bit
        }

        /** Bos hucreler arasinda en az adayi olan; tahta doluysa null. */
        fun bestEmptyCell(grid: IntArray): Int? {
            var best: Int? = null
            var bestCount = 10
            for (i in grid.indices) {
                if (grid[i] != 0) continue
                val n = 9 - Integer.bitCount(used(i) shr 1)
                if (n < bestCount) {
                    best = i
                    bestCount = n
                    if (n <= 1) break
                }
            }
            return best
        }

        companion object {
            /** Verilen tahtanin maskelerini kurar; tahta zaten celiskiliyse null. */
            fun from(grid: IntArray): Masks? {
                val m = Masks(IntArray(9), IntArray(9), IntArray(9))
                for (i in grid.indices) {
                    val v = grid[i]
                    if (v == 0) continue
                    if (m.used(i) and (1 shl v) != 0) return null
                    m.set(grid, i, v)
                }
                return m
            }
        }
    }
}

/** Puan: taban - hata/ipucu cezasi - hedef sureyi asan saniyeler; en az tabanin 1/5'i. */
fun sudokuScore(difficulty: SudokuDifficulty, mistakes: Int, hints: Int, elapsedSeconds: Int): Int {
    val penalty = mistakes * 100 + hints * 150 + (elapsedSeconds - difficulty.parSeconds).coerceAtLeast(0)
    return (difficulty.baseScore - penalty).coerceAtLeast(difficulty.baseScore / 5)
}
