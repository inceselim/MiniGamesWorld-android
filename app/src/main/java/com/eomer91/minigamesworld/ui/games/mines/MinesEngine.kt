package com.eomer91.minigamesworld.ui.games.mines

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlin.random.Random

enum class MinesDifficulty(
    @StringRes val labelRes: Int,
    val cols: Int,
    val rows: Int,
    val mines: Int,
    val baseScore: Int,
    val parSeconds: Int,
) {
    EASY(R.string.sudoku_easy, cols = 9, rows = 9, mines = 10, baseScore = 300, parSeconds = 60),
    NORMAL(R.string.sudoku_normal, cols = 10, rows = 14, mines = 24, baseScore = 800, parSeconds = 180),
    HARD(R.string.sudoku_hard, cols = 12, rows = 18, mines = 45, baseScore = 2000, parSeconds = 420),
}

data class MineCell(
    val mine: Boolean = false,
    val adjacent: Int = 0,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
)

data class MineBoard(
    val cols: Int,
    val rows: Int,
    val mineCount: Int,
    val cells: List<MineCell>,
    /** Mayinlar ilk dokunustan sonra yerlestirilir (ilk dokunus hep guvenli). */
    val minesPlaced: Boolean = false,
) {
    val flags: Int get() = cells.count { it.flagged }
    val minesLeft: Int get() = mineCount - flags
}

data class RevealResult(val board: MineBoard, val hitMine: Int? = null, val opened: Int = 0)

/** Mayin Tarlasi kurallari. Android'den bagimsiz, unit test ile test edilir. */
class MinesEngine(private val random: Random = Random.Default) {

    fun empty(d: MinesDifficulty) = MineBoard(d.cols, d.rows, d.mines, List(d.cols * d.rows) { MineCell() })

    fun neighbors(b: MineBoard, i: Int): List<Int> {
        val r = i / b.cols
        val c = i % b.cols
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until b.rows && nc in 0 until b.cols) out += nr * b.cols + nc
        }
        return out
    }

    /** [safe] hucresine ve komsularina mayin koymaz. */
    fun placeMines(b: MineBoard, safe: Int): MineBoard {
        val forbidden = neighbors(b, safe).toSet() + safe
        val candidates = b.cells.indices.filter { it !in forbidden }
        val mines = candidates.shuffled(random).take(b.mineCount.coerceAtMost(candidates.size)).toSet()
        val cells = b.cells.indices.map { i ->
            MineCell(mine = i in mines, adjacent = neighbors(b, i).count { it in mines })
        }
        return b.copy(cells = cells, minesPlaced = true)
    }

    /** Hucreyi acar; 0 ise bos alani zincirleme acar. Mayinsa [RevealResult.hitMine] dolar. */
    fun reveal(b: MineBoard, i: Int): RevealResult {
        var board = if (!b.minesPlaced) placeMines(b, i) else b
        val cell = board.cells[i]
        if (cell.revealed || cell.flagged) return RevealResult(board)
        if (cell.mine) {
            board = board.copy(cells = board.cells.replace(i) { it.copy(revealed = true) })
            return RevealResult(board, hitMine = i)
        }

        val cells = board.cells.toMutableList()
        val queue = ArrayDeque(listOf(i))
        var opened = 0
        while (queue.isNotEmpty()) {
            val k = queue.removeFirst()
            val c = cells[k]
            if (c.revealed || c.flagged || c.mine) continue
            cells[k] = c.copy(revealed = true)
            opened++
            if (c.adjacent == 0) neighbors(board, k).filter { !cells[it].revealed }.forEach(queue::addLast)
        }
        return RevealResult(board.copy(cells = cells), opened = opened)
    }

    /** Acik bir sayiya dokunus: cevresinde o kadar bayrak varsa kalan komsulari acar. */
    fun chord(b: MineBoard, i: Int): RevealResult {
        val cell = b.cells[i]
        if (!cell.revealed || cell.adjacent == 0) return RevealResult(b)
        val around = neighbors(b, i)
        if (around.count { b.cells[it].flagged } != cell.adjacent) return RevealResult(b)

        var board = b
        var opened = 0
        for (n in around) {
            val r = reveal(board, n)
            board = r.board
            opened += r.opened
            if (r.hitMine != null) return RevealResult(board, hitMine = r.hitMine, opened = opened)
        }
        return RevealResult(board, opened = opened)
    }

    fun toggleFlag(b: MineBoard, i: Int): MineBoard {
        val c = b.cells[i]
        if (c.revealed) return b
        return b.copy(cells = b.cells.replace(i) { it.copy(flagged = !it.flagged) })
    }

    fun isWon(b: MineBoard) = b.minesPlaced && b.cells.all { it.mine || it.revealed }

    /** Kaybedince tum mayinlari gosterir. */
    fun revealMines(b: MineBoard) = b.copy(cells = b.cells.map { if (it.mine) it.copy(revealed = true) else it })

    /** Ikinci sans: patlayan mayin bayrakla isaretlenir, diger acilan mayinlar kapanir. */
    fun secondChance(b: MineBoard, exploded: Int): MineBoard = b.copy(
        cells = b.cells.mapIndexed { i, c ->
            when {
                i == exploded -> c.copy(revealed = false, flagged = true)
                c.mine -> c.copy(revealed = false)
                else -> c
            }
        },
    )

    private inline fun List<MineCell>.replace(i: Int, f: (MineCell) -> MineCell) = mapIndexed { k, c -> if (k == i) f(c) else c }
}

fun minesScore(d: MinesDifficulty, elapsedSeconds: Int, usedSecondChance: Boolean): Int {
    val raw = (d.baseScore - (elapsedSeconds - d.parSeconds).coerceAtLeast(0)).coerceAtLeast(d.baseScore / 5)
    return if (usedSecondChance) raw * 3 / 4 else raw
}
