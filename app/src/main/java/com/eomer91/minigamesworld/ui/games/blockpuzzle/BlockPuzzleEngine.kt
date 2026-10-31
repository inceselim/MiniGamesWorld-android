package com.eomer91.minigamesworld.ui.games.blockpuzzle

import kotlin.random.Random

/** 0 = bos hucre, 1..N = dolu (renk indeksi). */
typealias BlockBoard = List<List<Int>>

/** Parcanin dolu hucreleri (satir, sutun), sol ust kose 0,0. */
data class Shape(val cells: List<Pair<Int, Int>>) {
    val rows: Int = cells.maxOf { it.first } + 1
    val cols: Int = cells.maxOf { it.second } + 1
}

data class BlockPiece(val shape: Shape, val color: Int)

data class ClearResult(val board: BlockBoard, val linesCleared: Int)

/** Block Puzzle kurallari. Android'e bagimli degil, unit test ile test edilir. */
class BlockPuzzleEngine(
    private val random: Random = Random.Default,
    val boardSize: Int = 8,
) {
    fun emptyBoard(): BlockBoard = List(boardSize) { List(boardSize) { 0 } }

    fun randomPieces(): List<BlockPiece> = List(PIECES_PER_ROUND) {
        BlockPiece(SHAPES[random.nextInt(SHAPES.size)], color = random.nextInt(1, COLOR_COUNT + 1))
    }

    fun canPlace(board: BlockBoard, shape: Shape, row: Int, col: Int): Boolean =
        shape.cells.all { (r, c) ->
            val br = row + r
            val bc = col + c
            br in 0 until boardSize && bc in 0 until boardSize && board[br][bc] == 0
        }

    fun place(board: BlockBoard, piece: BlockPiece, row: Int, col: Int): BlockBoard {
        val target = piece.shape.cells.map { (r, c) -> (row + r) to (col + c) }.toSet()
        return board.mapIndexed { r, line ->
            line.mapIndexed { c, v -> if ((r to c) in target) piece.color else v }
        }
    }

    /** Dolu satir ve sutunlari ayni anda temizler. */
    fun clearLines(board: BlockBoard): ClearResult {
        val fullRows = (0 until boardSize).filter { r -> board[r].all { it != 0 } }
        val fullCols = (0 until boardSize).filter { c -> (0 until boardSize).all { r -> board[r][c] != 0 } }
        if (fullRows.isEmpty() && fullCols.isEmpty()) return ClearResult(board, 0)

        val cleared = board.mapIndexed { r, line ->
            line.mapIndexed { c, v -> if (r in fullRows || c in fullCols) 0 else v }
        }
        return ClearResult(cleared, fullRows.size + fullCols.size)
    }

    fun canPlaceAnywhere(board: BlockBoard, shape: Shape): Boolean =
        (0..boardSize - shape.rows).any { r -> (0..boardSize - shape.cols).any { c -> canPlace(board, shape, r, c) } }

    /** Elde kalan parcalarin hicbiri yerlesemiyorsa oyun biter. */
    fun isGameOver(board: BlockBoard, pieces: List<BlockPiece?>): Boolean =
        pieces.filterNotNull().none { canPlaceAnywhere(board, it.shape) }

    fun scoreFor(linesCleared: Int): Int = POINTS_PER_PLACEMENT + linesCleared * POINTS_PER_LINE

    companion object {
        const val PIECES_PER_ROUND = 3
        const val COLOR_COUNT = 6
        const val POINTS_PER_PLACEMENT = 10
        const val POINTS_PER_LINE = 100

        private fun shape(vararg rows: String) = Shape(
            rows.flatMapIndexed { r, line -> line.mapIndexedNotNull { c, ch -> if (ch == '#') r to c else null } },
        )

        /** RN surumundeki parcalar + eksik kalan dikey 3'lu. */
        val SHAPES: List<Shape> = listOf(
            shape("#"),
            shape("##"),
            shape("#", "#"),
            shape("###"),
            shape("#", "#", "#"),
            shape("##", "##"),
            shape("#.", "##"),
            shape(".#", "##"),
        )
    }
}
