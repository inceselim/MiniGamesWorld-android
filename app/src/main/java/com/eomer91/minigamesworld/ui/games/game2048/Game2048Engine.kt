package com.eomer91.minigamesworld.ui.games.game2048

import kotlin.random.Random

typealias Board = List<List<Int>>

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class MoveResult(val board: Board, val moved: Boolean, val gainedScore: Int)

/** 2048 kurallari. Android'e bagimli degil, unit test ile test edilir. */
class Game2048Engine(
    private val random: Random = Random.Default,
    val size: Int = 4,
) {
    fun emptyBoard(): Board = List(size) { List(size) { 0 } }

    fun initialBoard(): Board = (1..INITIAL_TILES).fold(emptyBoard()) { board, _ -> addRandomTile(board) }

    fun emptyCells(board: Board): List<Pair<Int, Int>> {
        // Not: buildList { } icinde "size" listenin kendi boyutunu (0) gosterir; bu yuzden disarida tutuyoruz.
        val cells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) for (c in 0 until size) if (board[r][c] == 0) cells += r to c
        return cells
    }

    fun addRandomTile(board: Board): Board {
        val empty = emptyCells(board)
        if (empty.isEmpty()) return board
        val (row, col) = empty[random.nextInt(empty.size)]
        val value = if (random.nextFloat() < 0.9f) 2 else 4
        return board.mapIndexed { r, line ->
            if (r != row) line else line.mapIndexed { c, v -> if (c == col) value else v }
        }
    }

    /** Taslari kaydirir ve birlestirir; yeni tas eklemez. */
    fun slide(board: Board, direction: Direction): MoveResult {
        val result = Array(size) { IntArray(size) }
        var gained = 0

        for (i in 0 until size) {
            val line = (0 until size).map { j -> board.cell(direction, i, j) }
            val (merged, score) = mergeLine(line, size)
            gained += score
            merged.forEachIndexed { j, value ->
                when (direction) {
                    Direction.LEFT -> result[i][j] = value
                    Direction.RIGHT -> result[i][size - 1 - j] = value
                    Direction.UP -> result[j][i] = value
                    Direction.DOWN -> result[size - 1 - j][i] = value
                }
            }
        }

        val newBoard = result.map { it.toList() }
        return MoveResult(newBoard, moved = newBoard != board, gainedScore = gained)
    }

    /** Kaydirir; hareket olduysa rastgele yeni tas ekler. */
    fun move(board: Board, direction: Direction): MoveResult {
        val slid = slide(board, direction)
        return if (slid.moved) slid.copy(board = addRandomTile(slid.board)) else slid
    }

    fun canMove(board: Board): Boolean =
        emptyCells(board).isNotEmpty() || Direction.entries.any { slide(board, it).moved }

    fun hasTile(board: Board, value: Int): Boolean = board.any { row -> row.any { it >= value } }

    private fun Board.cell(direction: Direction, i: Int, j: Int): Int = when (direction) {
        Direction.LEFT -> this[i][j]
        Direction.RIGHT -> this[i][size - 1 - j]
        Direction.UP -> this[j][i]
        Direction.DOWN -> this[size - 1 - j][i]
    }

    companion object {
        const val INITIAL_TILES = 2
        const val WIN_TILE = 2048

        /** Bir satiri sola dogru birlestirir. Ornek: [2,2,4,0] -> [4,4,0,0], skor 4. */
        fun mergeLine(line: List<Int>, size: Int = line.size): Pair<List<Int>, Int> {
            val tiles = line.filter { it != 0 }
            val out = ArrayList<Int>(size)
            var score = 0
            var i = 0
            while (i < tiles.size) {
                if (i + 1 < tiles.size && tiles[i] == tiles[i + 1]) {
                    val merged = tiles[i] * 2
                    out += merged
                    score += merged
                    i += 2
                } else {
                    out += tiles[i]
                    i += 1
                }
            }
            while (out.size < size) out += 0
            return out to score
        }
    }
}
