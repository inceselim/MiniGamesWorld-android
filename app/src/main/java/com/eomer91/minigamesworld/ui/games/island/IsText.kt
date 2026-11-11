package com.eomer91.minigamesworld.ui.games.island

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.Cat
import com.eomer91.minigamesworld.ui.games.island.engine.Difficulty
import com.eomer91.minigamesworld.ui.games.island.engine.Fin
import com.eomer91.minigamesworld.ui.games.island.engine.Goal
import com.eomer91.minigamesworld.ui.games.island.engine.IsCatalog
import com.eomer91.minigamesworld.ui.games.island.engine.IsNews
import com.eomer91.minigamesworld.ui.games.island.engine.IsSim
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import com.eomer91.minigamesworld.ui.games.island.engine.MapSize
import com.eomer91.minigamesworld.ui.games.island.engine.MapType
import com.eomer91.minigamesworld.ui.games.island.engine.NewsKind
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs

object IsFmt {
    fun money(v: Long): String {
        val a = abs(v.toDouble()); val s = if (v < 0) "-" else ""
        val n = when {
            a >= 1e9 -> "%.2fB".format(a / 1e9)
            a >= 1e7 -> "%.1fM".format(a / 1e6)
            a >= 1e6 -> "%.2fM".format(a / 1e6)
            a >= 1e4 -> "%.0fK".format(a / 1e3)
            a >= 1e3 -> "%.1fK".format(a / 1e3)
            else -> "%.0f".format(a)
        }
        return "$s$$n"
    }

    /** Birim fiyat (gecelik / ziyaret) */
    fun price(p: Float): String = if (p >= 100f) "$" + "%.0f".format(p) else "$" + "%.1f".format(p)

    fun date(w: IsWorld, day: Int = w.day): String {
        val m = (3 + day / 30) % 12
        val y = w.startYear + (3 + day / 30) / 12
        return "${day % 30 + 1} ${DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths[m]} $y"
    }

    fun count(v: Float): String = when {
        v >= 10_000f -> "%.1fK".format(v / 1000f)
        else -> "%.0f".format(v)
    }

    fun pct(f: Float): String = "%.0f%%".format(f * 100f)
}

@StringRes
fun bName(t: BType): Int = when (t.key) {
    "tent" -> R.string.is_b_tent; "pension" -> R.string.is_b_pension; "bungalow" -> R.string.is_b_bungalow
    "hotel3" -> R.string.is_b_hotel3; "hotel4" -> R.string.is_b_hotel4; "resort" -> R.string.is_b_resort
    "villa" -> R.string.is_b_villa; "kiosk" -> R.string.is_b_kiosk; "fastfood" -> R.string.is_b_fastfood
    "restaurant" -> R.string.is_b_restaurant; "finedining" -> R.string.is_b_finedining; "pub" -> R.string.is_b_pub
    "disco" -> R.string.is_b_disco; "arcade" -> R.string.is_b_arcade; "funfair" -> R.string.is_b_funfair
    "waterpark" -> R.string.is_b_waterpark; "casino" -> R.string.is_b_casino; "souvenir" -> R.string.is_b_souvenir
    "market" -> R.string.is_b_market; "mall" -> R.string.is_b_mall; "beachclub" -> R.string.is_b_beachclub
    "park" -> R.string.is_b_park; "palm" -> R.string.is_b_palm; "garden" -> R.string.is_b_garden
    "road" -> R.string.is_b_road; "airport" -> R.string.is_b_airport; "motel" -> R.string.is_b_motel; "bigpark" -> R.string.is_b_bigpark; else -> R.string.is_b_harbor
}

/** Insa menusunun kategorileri (sirasiyla) */
val BUILD_CATS = listOf(Cat.STAY, Cat.FOOD, Cat.NIGHT, Cat.FUN, Cat.GAMBLE, Cat.SHOP, Cat.BEACH, Cat.NATURE, Cat.ROAD)

@StringRes
fun catName(c: Cat): Int = when (c) {
    Cat.STAY -> R.string.is_cat_stay; Cat.FOOD -> R.string.is_cat_food; Cat.NIGHT -> R.string.is_cat_night
    Cat.FUN -> R.string.is_cat_fun; Cat.GAMBLE -> R.string.is_cat_gamble; Cat.SHOP -> R.string.is_cat_shop
    Cat.BEACH -> R.string.is_cat_beach; Cat.NATURE -> R.string.is_cat_nature; else -> R.string.is_cat_road
}

fun catIcon(c: Cat): String = when (c) {
    Cat.STAY -> "🏨"; Cat.FOOD -> "🍔"; Cat.NIGHT -> "🪩"; Cat.FUN -> "🎡"; Cat.GAMBLE -> "🎳"
    Cat.SHOP -> "🛍️"; Cat.BEACH -> "🏖️"; Cat.NATURE -> "🌴"; Cat.ROAD -> "🛣️"; Cat.PUBLIC -> "✈️"
}

@StringRes
fun finName(f: Fin): Int = when (f) {
    Fin.STAY -> R.string.is_fin_stay; Fin.FOOD -> R.string.is_fin_food; Fin.NIGHT -> R.string.is_fin_night
    Fin.FUN -> R.string.is_fin_fun; Fin.GAMBLE -> R.string.is_fin_gamble; Fin.SHOP -> R.string.is_fin_shop
    Fin.BEACH -> R.string.is_fin_beach; Fin.SALES -> R.string.is_fin_sales; Fin.OTHER_IN -> R.string.is_fin_other_in
    Fin.BUILD -> R.string.is_fin_build; Fin.LAND -> R.string.is_fin_land; Fin.OPERATING -> R.string.is_fin_operating
    Fin.UPKEEP -> R.string.is_fin_upkeep; Fin.ADS -> R.string.is_fin_ads; Fin.INTEREST -> R.string.is_fin_interest
    Fin.DIRTY -> R.string.is_fin_dirty; Fin.FINES -> R.string.is_fin_fines; Fin.OTHER_OUT -> R.string.is_fin_other_out
}

@StringRes
fun clsName(c: Int): Int = when (c) { 0 -> R.string.is_cls_low; 1 -> R.string.is_cls_mid; else -> R.string.is_cls_high }

@StringRes
fun diffName(d: Difficulty): Int = when (d) { Difficulty.EASY -> R.string.is_diff_easy; Difficulty.NORMAL -> R.string.is_diff_normal; Difficulty.HARD -> R.string.is_diff_hard }

@StringRes
fun mapTypeName(m: MapType): Int = when (m) { MapType.ISLAND -> R.string.is_map_island; MapType.ARCHIPELAGO -> R.string.is_map_arch; MapType.COAST -> R.string.is_map_coast; MapType.RANDOM -> R.string.is_map_random }

@StringRes
fun mapSizeName(m: MapSize): Int = when (m) { MapSize.SMALL -> R.string.is_size_small; MapSize.MEDIUM -> R.string.is_size_medium; MapSize.LARGE -> R.string.is_size_large }

@StringRes
fun goalName(g: Goal): Int = when (g) { Goal.LAST_STANDING -> R.string.is_goal_last; Goal.RICHEST_20Y -> R.string.is_goal_richest; Goal.ENDLESS -> R.string.is_goal_endless }

@StringRes
fun goalDesc(g: Goal): Int = when (g) { Goal.LAST_STANDING -> R.string.is_goal_last_desc; Goal.RICHEST_20Y -> R.string.is_goal_richest_desc; Goal.ENDLESS -> R.string.is_goal_endless_desc }

@StringRes
fun seasonName(month: Int): Int {
    val f = IsSim.SEASON[month]
    return when { f >= 1.1f -> R.string.is_season_high; f <= 0.6f -> R.string.is_season_low; else -> R.string.is_season_mid }
}

fun companyName(w: IsWorld, id: Int): String = w.companies.getOrNull(id)?.name ?: "?"

/** Haber satiri; gosterilmeyecek tur icin null */
@Composable
fun newsText(w: IsWorld, n: IsNews): String? {
    val who = companyName(w, n.company)
    val type = if (n.type >= 0) stringResource(bName(IsCatalog[n.type.coerceIn(0, IsCatalog.types.size - 1)])) else ""
    return when (n.kind) {
        NewsKind.BUILT -> stringResource(R.string.is_n_built, who, type)
        NewsKind.BANKRUPT -> stringResource(R.string.is_n_bankrupt, who)
        NewsKind.AUCTION -> stringResource(R.string.is_n_auction, who, n.value.toInt())
        NewsKind.AUCTION_WON -> stringResource(R.string.is_n_auction_won, who, type, IsFmt.money(n.value))
        NewsKind.AD -> stringResource(R.string.is_n_ad, who, stringResource(clsName(n.value.toInt())))
        NewsKind.SABOTAGE -> stringResource(R.string.is_n_sabotage, who, type)
        NewsKind.CAUGHT -> stringResource(R.string.is_n_caught, who, IsFmt.money(n.value))
        NewsKind.PROPAGANDA -> stringResource(R.string.is_n_propaganda, who)
        NewsKind.STRIKE -> stringResource(R.string.is_n_strike, who, n.value.toInt())
        NewsKind.PRICE_WAR -> stringResource(R.string.is_n_price_war, who, type)
        NewsKind.WARNING -> stringResource(R.string.is_n_warning, who)
        else -> null
    }
}

/** Haber turunun simgesi */
fun newsIcon(k: NewsKind): String = when (k) {
    NewsKind.BUILT -> "🏗️"; NewsKind.BANKRUPT -> "💥"; NewsKind.AUCTION -> "🔨"; NewsKind.AUCTION_WON -> "🔨"
    NewsKind.AD -> "📣"; NewsKind.SABOTAGE -> "🧨"; NewsKind.CAUGHT -> "🚔"; NewsKind.PROPAGANDA -> "🗞️"
    NewsKind.STRIKE -> "✊"; NewsKind.PRICE_WAR -> "🏷️"; NewsKind.WARNING -> "⚠️"; else -> "•"
}
