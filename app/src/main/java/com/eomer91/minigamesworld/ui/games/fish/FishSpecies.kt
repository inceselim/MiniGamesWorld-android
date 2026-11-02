package com.eomer91.minigamesworld.ui.games.fish

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R

/**
 * Balik turleri. Derinlik metre cinsinden; [value] inci/puan degeri, [size] cizim boyu (birim),
 * [speed] yatay hiz, [rarity] dogma agirligi (kucuk = nadir). Renkler ARGB.
 */
enum class FishSpecies(
    @StringRes val nameRes: Int,
    val minDepth: Float,
    val maxDepth: Float,
    val value: Int,
    val size: Float,
    val speed: Float,
    val rarity: Float,
    val color: Long,
    val accent: Long,
) {
    SARDINE(R.string.fish_sardine, 2f, 22f, 2, 0.55f, 2.4f, 10f, 0xFFB0BEC5, 0xFF78909C),
    CLOWNFISH(R.string.fish_clownfish, 5f, 30f, 4, 0.6f, 1.8f, 7f, 0xFFFF7043, 0xFFFFFFFF),
    BLUE_TANG(R.string.fish_blue_tang, 14f, 45f, 6, 0.7f, 1.9f, 6f, 0xFF1E88E5, 0xFFFFEB3B),
    MACKEREL(R.string.fish_mackerel, 20f, 52f, 5, 0.8f, 2.6f, 6f, 0xFF4DB6AC, 0xFF263238),
    PUFFERFISH(R.string.fish_pufferfish, 26f, 62f, 9, 0.7f, 1.2f, 4.5f, 0xFFFDD835, 0xFF8D6E63),
    ANGELFISH(R.string.fish_angelfish, 38f, 78f, 12, 0.85f, 1.5f, 4f, 0xFFAB47BC, 0xFFFFFFFF),
    SWORDFISH(R.string.fish_swordfish, 55f, 105f, 25, 1.3f, 3.0f, 2.5f, 0xFF546E7A, 0xFFB0BEC5),
    ANGLERFISH(R.string.fish_anglerfish, 75f, 130f, 40, 0.95f, 1.1f, 2f, 0xFF37474F, 0xFFFFF176),
    GOLDFISH(R.string.fish_goldfish, 90f, 130f, 100, 0.7f, 2.2f, 0.35f, 0xFFFFC107, 0xFFFFF8E1),
}
