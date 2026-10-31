package com.eomer91.minigamesworld.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.GameCatalog
import com.eomer91.minigamesworld.data.GameCategory
import com.eomer91.minigamesworld.ui.components.CategoryChips
import com.eomer91.minigamesworld.ui.components.FeaturedBanner
import com.eomer91.minigamesworld.ui.components.GameRow
import com.eomer91.minigamesworld.ui.components.GoldBadge
import com.eomer91.minigamesworld.ui.components.ScreenTitle

@Composable
fun HomeScreen(
    onGameClick: (String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val gold by viewModel.gold.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<GameCategory?>(null) }
    val featured = GameCatalog.featured

    LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ScreenTitle(stringResource(R.string.home_title), stringResource(R.string.home_subtitle), Modifier.weight(1f))
                GoldBadge(gold)
            }
        }
        item {
            CategoryChips(
                selected = category,
                onSelect = { category = it },
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(top = 18.dp),
            )
        }
        if (category == null && featured != null) {
            item {
                FeaturedBanner(
                    game = featured,
                    onClick = { onGameClick(featured.id) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
                )
            }
        }
        val visibleCategories = category?.let { listOf(it) } ?: GameCategory.entries
        visibleCategories.forEach { cat ->
            item(key = cat.name) {
                GameRow(
                    title = stringResource(cat.labelRes),
                    games = GameCatalog.games.filter { it.category == cat },
                    onGameClick = onGameClick,
                )
            }
        }
    }
}
