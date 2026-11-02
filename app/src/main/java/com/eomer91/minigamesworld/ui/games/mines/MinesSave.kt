package com.eomer91.minigamesworld.ui.games.mines

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Yarim kalan Mayin Tarlasi kaydi. Hucreler kompakt string: her hucre icin bir karakter. */
@Serializable
data class MinesSave(
    val version: Int = CURRENT_VERSION,
    val difficulty: String,
    /** Her hucre: bit0 mayin, bit1 acik, bit2 bayrak -> '0'..'7'. */
    val cells: String,
    val minesPlaced: Boolean,
    val elapsedSeconds: Int,
    val secondChanceUsed: Boolean,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    fun toBoard(engine: MinesEngine): MineBoard? {
        val d = MinesDifficulty.entries.firstOrNull { it.name == difficulty } ?: return null
        if (cells.length != d.cols * d.rows) return null
        val empty = engine.empty(d)
        val mines = cells.indices.filter { (cells[it] - '0') and 1 != 0 }.toSet()
        val restored = cells.mapIndexed { i, ch ->
            val bits = ch - '0'
            MineCell(
                mine = bits and 1 != 0,
                adjacent = engine.neighbors(empty, i).count { it in mines },
                revealed = bits and 2 != 0,
                flagged = bits and 4 != 0,
            )
        }
        return empty.copy(cells = restored, minesPlaced = minesPlaced)
    }

    companion object {
        const val CURRENT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): MinesSave? =
            runCatching { json.decodeFromString(serializer(), text) }.getOrNull()?.takeIf { it.version == CURRENT_VERSION }

        fun from(d: MinesDifficulty, b: MineBoard, elapsed: Int, secondChanceUsed: Boolean) = MinesSave(
            difficulty = d.name,
            cells = b.cells.joinToString("") { c ->
                ((if (c.mine) 1 else 0) or (if (c.revealed) 2 else 0) or (if (c.flagged) 4 else 0)).toString()
            },
            minesPlaced = b.minesPlaced,
            elapsedSeconds = elapsed,
            secondChanceUsed = secondChanceUsed,
        )
    }
}
