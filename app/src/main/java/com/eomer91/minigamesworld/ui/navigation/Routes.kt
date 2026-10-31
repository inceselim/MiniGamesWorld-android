package com.eomer91.minigamesworld.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.eomer91.minigamesworld.R
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object FavoritesRoute
@Serializable data object ProfileRoute
@Serializable data object SettingsRoute

@Serializable data class GameDetailRoute(val gameId: String)
@Serializable data class PlayRoute(val gameId: String)

data class TopLevelRoute<T : Any>(
    val route: T,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

val topLevelRoutes = listOf(
    TopLevelRoute(HomeRoute, R.string.tab_games, Icons.Rounded.SportsEsports),
    TopLevelRoute(SearchRoute, R.string.tab_search, Icons.Rounded.Search),
    TopLevelRoute(FavoritesRoute, R.string.tab_favorites, Icons.Rounded.Favorite),
    TopLevelRoute(ProfileRoute, R.string.tab_profile, Icons.Rounded.Person),
    TopLevelRoute(SettingsRoute, R.string.tab_settings, Icons.Rounded.Settings),
)
