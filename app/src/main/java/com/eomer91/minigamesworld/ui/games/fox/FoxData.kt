package com.eomer91.minigamesworld.ui.games.fox

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Dunya temalari: gokyuzu renkleri ve karo seti. */
enum class FoxWorld(
    @StringRes val nameRes: Int,
    val skyTop: Long,
    val skyBottom: Long,
    val hillFar: Long,
    val hillNear: Long,
) {
    MEADOW(R.string.fx_world_meadow, 0xFF5EB7F0, 0xFFBDE8FF, 0xFF8FCB9B, 0xFF5FAE6E),
    CAVE(R.string.fx_world_cave, 0xFF120F1E, 0xFF2A2440, 0xFF231E36, 0xFF332B4C),
    CASTLE(R.string.fx_world_castle, 0xFF2A0F1A, 0xFF6E2A22, 0xFF3A1820, 0xFF4E2026),
    ;

    companion object {
        fun of(index: Int) = entries[index.coerceIn(0, entries.lastIndex)]
    }
}

/** Kalici ilerleme: acilan bolum (genel sira) ve bolum basina en iyi yildiz/sure. */
@Serializable
data class FoxProfile(
    val version: Int = 1,
    /** Acilmis en yuksek bolum (dunya*5 + bolum). */
    val unlocked: Int = 0,
    /** "d-b" -> en cok yildiz */
    val stars: Map<String, Int> = emptyMap(),
    /** "d-b" -> en iyi sure (sn) */
    val bestTime: Map<String, Int> = emptyMap(),
) {
    fun starsOf(world: Int, level: Int) = stars[key(world, level)] ?: 0
    fun timeOf(world: Int, level: Int) = bestTime[key(world, level)]
    fun isUnlocked(world: Int, level: Int) = world * FoxEngine.LEVELS_PER_WORLD + level <= unlocked
    fun isCleared(world: Int, level: Int) = key(world, level) in bestTime
    val totalStars: Int get() = stars.values.sum()

    /** Bolum bitince profili guncelle. */
    fun withResult(world: Int, level: Int, r: FoxResult): FoxProfile {
        val k = key(world, level)
        val index = world * FoxEngine.LEVELS_PER_WORLD + level
        val last = FoxEngine.WORLDS * FoxEngine.LEVELS_PER_WORLD - 1
        return copy(
            unlocked = maxOf(unlocked, (index + 1).coerceAtMost(last)),
            stars = stars + (k to maxOf(stars[k] ?: 0, r.stars)),
            bestTime = bestTime + (k to minOf(bestTime[k] ?: Int.MAX_VALUE, r.time.toInt())),
        )
    }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun key(world: Int, level: Int) = "$world-$level"
        fun fromJson(text: String?): FoxProfile =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: FoxProfile()
    }
}
