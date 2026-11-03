package com.eomer91.minigamesworld.ui.games.forest

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Kahramanin statlari. Ileride (adim 3) ekipman ve setlerden gelen degerler bunlara eklenecek.
 * [attack] ve [attackSpeed] yuzde bonus; [critChance] yuzde; [critDamage] yuzde carpani (150 = 1.5x).
 */
data class HeroStats(
    val maxHp: Float = 100f,
    val maxMp: Float = 100f,
    val attack: Float = 0f,
    val defence: Float = 0f,
    val attackSpeed: Float = 0f,
    val critChance: Float = 5f,
    val critDamage: Float = 150f,
    val moveSpeed: Float = 3.2f,
)

enum class EnemyType(
    val hp: Float,
    val speed: Float,
    val damage: Float,
    val xp: Int,
    /** Carpisma yaricapi (birim). */
    val radius: Float,
    /** Cizim boyu (birim). */
    val drawSize: Float,
    val isBoss: Boolean = false,
) {
    GOBLIN(10f, 1.6f, 10f, 1, 0.35f, 1.0f),
    BAT(5f, 2.5f, 7f, 1, 0.3f, 0.9f),
    WOLF(16f, 2.7f, 13f, 2, 0.4f, 1.15f),
    ORC(50f, 1.15f, 22f, 5, 0.55f, 1.6f),
    TREANT(4000f, 1.25f, 35f, 60, 1.1f, 3.4f, isBoss = true),
}

enum class WeaponType(@StringRes val nameRes: Int, @StringRes val descRes: Int, val color: Long) {
    BOLT(R.string.fg_w_bolt, R.string.fg_w_bolt_desc, 0xFF6FE0FF),
    BLADES(R.string.fg_w_blades, R.string.fg_w_blades_desc, 0xFFD8DEE9),
    LIGHTNING(R.string.fg_w_lightning, R.string.fg_w_lightning_desc, 0xFFF7D046),
    THORNS(R.string.fg_w_thorns, R.string.fg_w_thorns_desc, 0xFF8FE36B),
    ARROWS(R.string.fg_w_arrows, R.string.fg_w_arrows_desc, 0xFFF08A24),
    FROST(R.string.fg_w_frost, R.string.fg_w_frost_desc, 0xFFB8F0FF),
}

enum class PassiveType(@StringRes val nameRes: Int, @StringRes val descRes: Int, val color: Long) {
    MIGHT(R.string.fg_p_might, R.string.fg_p_might_desc, 0xFFE04848),
    HASTE(R.string.fg_p_haste, R.string.fg_p_haste_desc, 0xFFF7D046),
    SWIFT(R.string.fg_p_swift, R.string.fg_p_swift_desc, 0xFF6FE0FF),
    VITALITY(R.string.fg_p_vitality, R.string.fg_p_vitality_desc, 0xFF4ADE80),
    MAGNET(R.string.fg_p_magnet, R.string.fg_p_magnet_desc, 0xFFA66EE0),
    WISDOM(R.string.fg_p_wisdom, R.string.fg_p_wisdom_desc, 0xFF38BDF8),
}

/** Haritalar: renkler, dusman karisimi ve zorluk. */
enum class ForestMap(
    @StringRes val nameRes: Int,
    val grass: Long,
    val grassDark: Long,
    val hpMult: Float,
    val damageMult: Float,
    val spawnMult: Float,
    /** Dusman agirliklari: GOBLIN, BAT, WOLF, ORC */
    val weights: List<Float>,
) {
    EDGE(R.string.fg_map_1, 0xFF5E9E44, 0xFF4F8A3A, 1.0f, 1.0f, 1.0f, listOf(6f, 3f, 1.5f, 0.6f)),
    SWAMP(R.string.fg_map_2, 0xFF4E7D4A, 0xFF3F6A3E, 1.6f, 1.3f, 1.15f, listOf(4f, 5f, 2f, 1f)),
    MUSHROOM(R.string.fg_map_3, 0xFF5C7A3A, 0xFF4B6630, 2.4f, 1.6f, 1.3f, listOf(4f, 2f, 3f, 1.6f)),
    VALLEY(R.string.fg_map_4, 0xFF7A8450, 0xFF666F42, 3.4f, 2.0f, 1.45f, listOf(3f, 2f, 4f, 2.4f)),
    DARK(R.string.fg_map_5, 0xFF2F4A35, 0xFF243A2A, 4.8f, 2.6f, 1.6f, listOf(2f, 4f, 4f, 3.2f)),
}

/** Kalici profil (DataStore'da JSON). Adim 3'te ekipman da buraya eklenecek. */
@Serializable
data class ForestProfile(
    val version: Int = 1,
    /** Acilmis en yuksek harita indeksi. */
    val unlocked: Int = 0,
    val coins: Int = 0,
    val cleared: List<Int> = emptyList(),
    /** Harita indeksi -> en cok oldurme. */
    val bestKills: Map<Int, Int> = emptyMap(),
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String?): ForestProfile =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: ForestProfile()
    }
}
