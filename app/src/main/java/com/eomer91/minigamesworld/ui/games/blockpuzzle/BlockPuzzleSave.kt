package com.eomer91.minigamesworld.ui.games.blockpuzzle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Yarim kalan Block Puzzle oyununun kaydi (DataStore'da JSON olarak saklanir). */
@Serializable
data class BlockPuzzleSave(
    val version: Int = CURRENT_VERSION,
    val board: List<List<Int>>,
    val pieces: List<SavedPiece?>,
    val score: Int,
    val goldAwarded: Int,
) {
    @Serializable
    data class SavedPiece(val cells: List<SavedCell>, val color: Int)

    @Serializable
    data class SavedCell(val r: Int, val c: Int)

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val CURRENT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        /** Bozuk ya da uyumsuz kayitta null doner (yeni oyun baslar). */
        fun fromJson(text: String): BlockPuzzleSave? =
            runCatching { json.decodeFromString(serializer(), text) }
                .getOrNull()
                ?.takeIf { it.version == CURRENT_VERSION }

        fun from(state: BlockPuzzleUiState, goldAwarded: Int) = BlockPuzzleSave(
            board = state.board,
            pieces = state.pieces.map { p ->
                p?.let { SavedPiece(it.shape.cells.map { (r, c) -> SavedCell(r, c) }, it.color) }
            },
            score = state.score,
            goldAwarded = goldAwarded,
        )
    }

    fun piecesAsDomain(): List<BlockPiece?> = pieces.map { p ->
        p?.takeIf { it.cells.isNotEmpty() }?.let { BlockPiece(Shape(it.cells.map { cell -> cell.r to cell.c }), it.color) }
    }
}
