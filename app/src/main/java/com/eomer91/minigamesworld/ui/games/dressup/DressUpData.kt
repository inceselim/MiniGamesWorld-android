package com.eomer91.minigamesworld.ui.games.dressup

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.common.VP
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Bir kiyafet/aksesuar. [parts] 'main' yuvasi secilen renkle boyanir. */
class DressItem(val id: String, val defaultColor: Long, val tags: Set<String>, val parts: List<VP>)

enum class DressCategory(@StringRes val labelRes: Int, val emoji: String) {
    HAIR(R.string.du_cat_hair, "💇"),
    FACE(R.string.du_cat_face, "🙂"),
    TOP(R.string.du_cat_top, "👚"),
    BOTTOM(R.string.du_cat_bottom, "👖"),
    DRESS(R.string.du_cat_dress, "👗"),
    SHOES(R.string.du_cat_shoes, "👠"),
    HEAD(R.string.du_cat_head, "👑"),
    EXTRA(R.string.du_cat_extra, "👜"),
    BACKGROUND(R.string.du_cat_background, "🏞"),
}

enum class DressBackground(val tags: Set<String>) {
    ROOM(setOf("casual", "school")),
    BEACH(setOf("summer")),
    PARTY(setOf("party")),
    SNOW(setOf("winter")),
    GARDEN(setOf("summer", "casual")),
    CASTLE(setOf("fancy")),
}

enum class DressTheme(@StringRes val nameRes: Int, val emoji: String, val tag: String, val anti: Set<String>) {
    BEACH(R.string.du_theme_beach, "🏖", "summer", setOf("winter", "fancy")),
    PARTY(R.string.du_theme_party, "🎉", "party", setOf("winter")),
    WINTER(R.string.du_theme_winter, "❄", "winter", setOf("summer")),
    SCHOOL(R.string.du_theme_school, "🎒", "school", setOf("fancy")),
    BALL(R.string.du_theme_ball, "🏰", "fancy", setOf("summer", "casual")),
    CASUAL(R.string.du_theme_casual, "☕", "casual", setOf("fancy")),
}

object DressPalettes {
    val clothes = listOf(
        0xFFE5383B, 0xFFFF4D6D, 0xFFFF8FAB, 0xFFF7A1C4, 0xFFC77DFF, 0xFF9D4EDD, 0xFF4361EE, 0xFF5BC0EB,
        0xFF06D6A0, 0xFF2A9D8F, 0xFFFFD166, 0xFFF4A261, 0xFF7F5539, 0xFF264653, 0xFFFFFFFF, 0xFF222222,
    )
    val hair = listOf(0xFF6B3E26, 0xFF3B2416, 0xFFF2C14E, 0xFF1B1B1B, 0xFFC0392B, 0xFF8E44AD, 0xFFFF7EB6, 0xFF4CC9F0, 0xFFE8E8E8)
    val skin = listOf(0xFFFCE3D3, 0xFFF6D2B8, 0xFFE8B48F, 0xFFC68642, 0xFF8D5524, 0xFF5C3A21)
    val eyes = listOf(0xFF5B8DD6, 0xFF6B4226, 0xFF2E8B57, 0xFF8A8F98, 0xFF9B59B6)
    val lips = listOf(0xFFD9536F, 0xFFE5383B, 0xFFFF8FAB, 0xFFB5179E, 0xFFC08081, 0xFF8B1E3F)
    val hairStyles = listOf("long", "ponytail", "bob", "curly", "buns", "braids")
}

/** Kalici kayit: son kombin ve toplam yildiz. */
@Serializable
data class Outfit(
    val hair: String = "long",
    val hairColor: Long = DressPalettes.hair[0],
    val skin: Long = DressPalettes.skin[1],
    val eyes: Long = DressPalettes.eyes[0],
    val lips: Long = DressPalettes.lips[0],
    val top: String? = "tee",
    val bottom: String? = "jeans",
    val dress: String? = null,
    val shoes: String = "sneakers",
    val head: String = "none",
    val extra: String = "none",
    val background: Int = 0,
    /** kategori:itemId -> renk */
    val colors: Map<String, Long> = emptyMap(),
) {
    fun colorOf(cat: String, item: DressItem): Long = colors["$cat:${item.id}"] ?: item.defaultColor
}

@Serializable
data class DressProfile(
    val version: Int = 1,
    val outfit: Outfit = Outfit(),
    val totalStars: Int = 0,
    val shows: Int = 0,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String?): DressProfile =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: DressProfile()
    }
}

/** Jurinin puanlamasi. */
object DressJudge {
    /** Kombinin temaya uygunlugu: ham puan (yaklasik -10..20). */
    fun rawScore(theme: DressTheme, worn: List<Pair<Set<String>, Long>>, background: DressBackground): Int {
        var s = 0
        for ((tags, _) in worn) {
            when {
                theme.tag in tags -> s += 3
                tags.any { it in theme.anti } -> s -= 3
            }
        }
        when {
            theme.tag in background.tags -> s += 3
            background.tags.any { it in theme.anti } -> s -= 2
        }
        // Renk uyumu: az sayida farkli renk
        val distinct = worn.map { it.second }.distinct().size
        if (worn.size >= 3 && distinct <= 3) s += 2
        if (distinct >= 5) s -= 1
        return s
    }

    /** Uc juri puani (1..10). */
    fun judges(raw: Int, random: Random = Random.Default): List<Int> {
        val base = (raw * 10f / 18f).coerceIn(1f, 10f)
        return List(3) { (base + random.nextInt(-1, 2)).toInt().coerceIn(1, 10) }
    }

    fun stars(judges: List<Int>): Int {
        val avg = judges.average()
        return when {
            avg >= 8.0 -> 3
            avg >= 6.0 -> 2
            avg >= 4.0 -> 1
            else -> 0
        }
    }
}
