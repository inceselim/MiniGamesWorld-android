package com.eomer91.minigamesworld.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.eomer91.minigamesworld.ads.AdBanner
import com.eomer91.minigamesworld.ui.detail.GameDetailScreen
import com.eomer91.minigamesworld.ui.favorites.FavoritesScreen
import com.eomer91.minigamesworld.ui.home.HomeScreen
import com.eomer91.minigamesworld.ui.play.PlayScreen
import com.eomer91.minigamesworld.ui.profile.ProfileScreen
import com.eomer91.minigamesworld.ui.search.SearchScreen
import com.eomer91.minigamesworld.ui.settings.SettingsScreen
import com.eomer91.minigamesworld.ui.theme.AppColors

@Composable
fun MiniGamesAppRoot(
    privacyOptionsRequired: Boolean,
    onShowPrivacyOptions: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelRoutes.any { top ->
        currentDestination?.hierarchy?.any { it.hasRoute(top.route::class) } == true
    }

    // Banner reklam ana sayfada (Oyunlar) gosterilmez; diger sekmelerde gosterilir.
    val onHome = currentDestination?.hierarchy?.any { it.hasRoute(HomeRoute::class) } == true

    val openGame: (String) -> Unit = { id -> navController.navigate(GameDetailRoute(id)) }

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    if (!onHome) AdBanner()
                    NavigationBar(containerColor = AppColors.Surface) {
                        topLevelRoutes.forEach { top ->
                            val selected = currentDestination?.hierarchy
                                ?.any { it.hasRoute(top.route::class) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(top.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(top.icon, contentDescription = null) },
                                label = { Text(stringResource(top.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Primary,
                                    selectedTextColor = AppColors.Primary,
                                    indicatorColor = AppColors.SurfaceHigh,
                                    unselectedIconColor = AppColors.TextSecondary,
                                    unselectedTextColor = AppColors.TextSecondary,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> { HomeScreen(onGameClick = openGame) }
            composable<SearchRoute> { SearchScreen(onGameClick = openGame) }
            composable<FavoritesRoute> { FavoritesScreen(onGameClick = openGame) }
            composable<ProfileRoute> { ProfileScreen() }
            composable<SettingsRoute> {
                SettingsScreen(
                    privacyOptionsRequired = privacyOptionsRequired,
                    onShowPrivacyOptions = onShowPrivacyOptions,
                )
            }
            composable<GameDetailRoute> {
                GameDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlay = { id -> navController.navigate(PlayRoute(id)) },
                )
            }
            composable<PlayRoute> { entry ->
                PlayScreen(
                    gameId = entry.toRoute<PlayRoute>().gameId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
