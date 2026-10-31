package com.eomer91.minigamesworld.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.GameCatalog
import com.eomer91.minigamesworld.ui.components.EmptyState
import com.eomer91.minigamesworld.ui.components.GameCard
import com.eomer91.minigamesworld.ui.components.ScreenTitle
import com.eomer91.minigamesworld.ui.home.CatalogViewModel

@Composable
fun FavoritesScreen(
    onGameClick: (String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val games = GameCatalog.games.filter { it.id in favorites }

    Column(Modifier.padding(top = 16.dp)) {
        ScreenTitle(stringResource(R.string.tab_favorites), modifier = Modifier.padding(horizontal = 16.dp))
        if (games.isEmpty()) {
            EmptyState(stringResource(R.string.favorites_empty_title), stringResource(R.string.favorites_empty_subtitle))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(games, key = { it.id }) { game ->
                    GameCard(game, onClick = { onGameClick(game.id) })
                }
            }
        }
    }
}
