package com.eomer91.minigamesworld.ui.games.fish

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.math.roundToInt

/** Akvaryumdaki bir balik. Zaman damgalari milisaniye (System.currentTimeMillis). */
@Serializable
data class OwnedFish(
    val id: Long,
    val species: String,
    val caughtAt: Long,
    /** 0..1 buyume. */
    val growth: Float = 0f,
    val lastFedAt: Long = caughtAt,
    val lastPearlAt: Long = caughtAt,
)

/** Balik Dunyasi'nin kalici profili (DataStore'da JSON). */
@Serializable
data class FishProfile(
    val version: Int = 1,
    val pearls: Int = 0,
    val rodDepthLevel: Int = 0,
    val hookCapacityLevel: Int = 0,
    val reelLevel: Int = 0,
    val aquariumCapacity: Int = START_CAPACITY,
    val aquarium: List<OwnedFish> = emptyList(),
    /** Tur basina toplam tutulan sayisi (koleksiyon). */
    val caughtTotals: Map<String, Int> = emptyMap(),
    val nextFishId: Long = 1,
    /** Yem kabi (Aquarium.MAX_FOOD'a kadar zamanla dolar). */
    val food: Int = Aquarium.MAX_FOOD,
    val foodUpdatedAt: Long = 0L,
    /** Satin alinan susler (Decoration.name). */
    val decorations: List<String> = emptyList(),
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val START_CAPACITY = 12
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String?): FishProfile =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: FishProfile()
    }
}

/** Olta yukseltmeleri: seviye -> deger ve inci fiyati. */
enum class RodUpgrade(val labelRes: Int, val maxLevel: Int) {
    DEPTH(com.eomer91.minigamesworld.R.string.fish_up_depth, 7),
    CAPACITY(com.eomer91.minigamesworld.R.string.fish_up_capacity, 5),
    REEL(com.eomer91.minigamesworld.R.string.fish_up_reel, 5);

    fun level(p: FishProfile) = when (this) {
        DEPTH -> p.rodDepthLevel
        CAPACITY -> p.hookCapacityLevel
        REEL -> p.reelLevel
    }

    fun cost(level: Int): Int = when (this) {
        DEPTH -> (40 * 1.7.pow(level)).roundToInt()
        CAPACITY -> (60 * 1.9.pow(level)).roundToInt()
        REEL -> (35 * 1.6.pow(level)).roundToInt()
    }

    fun upgraded(p: FishProfile) = when (this) {
        DEPTH -> p.copy(rodDepthLevel = p.rodDepthLevel + 1)
        CAPACITY -> p.copy(hookCapacityLevel = p.hookCapacityLevel + 1)
        REEL -> p.copy(reelLevel = p.reelLevel + 1)
    }
}

object RodStats {
    fun maxDepth(level: Int) = 30f + level * 15f          // 30m .. 135m
    fun capacity(level: Int) = 2 + level                   // 2 .. 7 balik
    fun speedFactor(level: Int) = 1f + level * 0.15f
}
