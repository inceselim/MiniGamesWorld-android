package com.eomer91.minigamesworld.ui.games.blockrain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Yarim kalan Blok Yagmuru oyununun kaydi. */
@Serializable
data class BlockRainSave(
    val version: Int = CURRENT_VERSION,
    val board: List<List<Int>>,
    val piece: Int,
    val bag: List<Int>,
    val hold: Int,
    val score: Int,
    val lines: Int,
    val goldAwarded: Int = 0,
    val rescueUsed: Boolean = false,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val CURRENT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): BlockRainSave? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
                ?.takeIf { it.version == CURRENT_VERSION }
    }
}
