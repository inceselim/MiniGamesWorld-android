package com.eomer91.minigamesworld.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R

enum class GameCategory(@StringRes val labelRes: Int) {
    PUZZLE(R.string.category_puzzle),
    ARCADE(R.string.category_arcade),
    CARD(R.string.category_card),
    STRATEGY(R.string.category_strategy),
    CLASSIC(R.string.category_classic),
    CREATIVE(R.string.category_creative),
}

data class Game(
    /** Sabit kimlik: skor anahtarlari ve navigasyon bunu kullanir. Degistirme. */
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val category: GameCategory,
    @DrawableRes val imageRes: Int,
    val isFeatured: Boolean = false,
    /** Oyun native olarak tasindiginda true yapilir; false ise "Cok yakinda" ekrani acilir. */
    val isPlayable: Boolean = false,
    /** true ise oyun kodda kalir ama ana sayfa, arama, favoriler ve profilde listelenmez. */
    val isHidden: Boolean = false,
)
