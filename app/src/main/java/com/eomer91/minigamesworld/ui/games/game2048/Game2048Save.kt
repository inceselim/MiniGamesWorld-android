package com.eomer91.minigamesworld.ui.games.game2048

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Yarim kalan 2048 oyununun kaydi (DataStore'da JSON olarak saklanir). */
@Serializable
data class Game2048Save(
    val version: Int = CURRENT_VERSION,
    val board: List<List<Int>>,
    val score: Int,
    val reachedWinTile: Boolean,
    val goldAwarded: Int,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val CURRENT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        /** Bozuk ya da uyumsuz kayitta null doner (yeni oyun baslar). */
        fun fromJson(text: String): Game2048Save? =
            runCatching { json.decodeFromString(serializer(), text) }
                .getOrNull()
                ?.takeIf { it.version == CURRENT_VERSION }

        fun from(state: Game2048State, goldAwarded: Int) = Game2048Save(
            board = state.board,
            score = state.score,
            reachedWinTile = state.reachedWinTile,
            goldAwarded = goldAwarded,
        )
    }
}
