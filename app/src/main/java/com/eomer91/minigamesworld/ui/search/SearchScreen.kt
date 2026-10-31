package com.eomer91.minigamesworld.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.GameCatalog
import com.eomer91.minigamesworld.data.GameCategory
import com.eomer91.minigamesworld.ui.components.CategoryChips
import com.eomer91.minigamesworld.ui.components.EmptyState
import com.eomer91.minigamesworld.ui.components.GameCard
import com.eomer91.minigamesworld.ui.components.ScreenTitle
import com.eomer91.minigamesworld.ui.components.SearchField

@Composable
fun SearchScreen(onGameClick: (String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<GameCategory?>(null) }

    // Arama, cihaz dilindeki oyun adlari uzerinden yapilir.
    val results = remember(query, category, context) {
        val q = query.trim()
        GameCatalog.games.filter { game ->
            (category == null || game.category == category) &&
                (q.isEmpty() || context.getString(game.nameRes).contains(q, ignoreCase = true))
        }
    }

    Column(Modifier.padding(top = 16.dp)) {
        ScreenTitle(stringResource(R.string.tab_search), modifier = Modifier.padding(horizontal = 16.dp))
        SearchField(query, { query = it }, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
        CategoryChips(
            selected = category,
            onSelect = { category = it },
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(top = 14.dp),
        )
        if (results.isEmpty()) {
            EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_subtitle))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(results, key = { it.id }) { game ->
                    GameCard(game, onClick = { onGameClick(game.id) })
                }
            }
        }
    }
}
