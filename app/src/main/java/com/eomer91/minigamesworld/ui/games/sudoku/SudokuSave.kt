package com.eomer91.minigamesworld.ui.games.sudoku

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Yarim kalan Sudoku oyununun kaydi (DataStore'da JSON). */
@Serializable
data class SudokuSave(
    val version: Int = CURRENT_VERSION,
    val difficulty: String,
    val givens: List<Int>,
    val solution: List<Int>,
    val entries: List<Int>,
    val notes: List<List<Int>>,
    val mistakes: Int,
    val hintsUsed: Int,
    val elapsedSeconds: Int,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val CURRENT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): SudokuSave? =
            runCatching { json.decodeFromString(serializer(), text) }
                .getOrNull()
                ?.takeIf {
                    it.version == CURRENT_VERSION &&
                        it.givens.size == SudokuRules.CELLS &&
                        it.solution.size == SudokuRules.CELLS &&
                        it.entries.size == SudokuRules.CELLS &&
                        it.notes.size == SudokuRules.CELLS &&
                        SudokuDifficulty.entries.any { d -> d.name == it.difficulty }
                }
    }
}
