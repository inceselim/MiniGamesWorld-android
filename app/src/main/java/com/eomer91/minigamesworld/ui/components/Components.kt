package com.eomer91.minigamesworld.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.Game
import com.eomer91.minigamesworld.data.GameCategory
import com.eomer91.minigamesworld.ui.theme.AppColors

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = AppColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        if (subtitle != null) {
            Text(subtitle, color = AppColors.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.search_hint)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Surface,
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

/** null = Tumu */
@Composable
fun CategoryChips(
    selected: GameCategory?,
    onSelect: (GameCategory?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val options: List<GameCategory?> = listOf(null) + GameCategory.entries
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category?.labelRes ?: R.string.category_all)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = AppColors.Surface,
                    labelColor = AppColors.TextMuted,
                    selectedContainerColor = AppColors.Primary,
                    selectedLabelColor = AppColors.OnPrimary,
                ),
                border = null,
            )
        }
    }
}

@Composable
fun GameCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Image(
            painter = painterResource(game.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(20.dp)),
        )
        Text(
            stringResource(game.nameRes),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(stringResource(game.category.labelRes), color = AppColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
fun GameRow(title: String, games: List<Game>, onGameClick: (String) -> Unit) {
    if (games.isEmpty()) return
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            title,
            color = AppColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            items(games, key = { it.id }) { game ->
                GameCard(game, onClick = { onGameClick(game.id) }, modifier = Modifier.width(170.dp))
            }
        }
    }
}

@Composable
fun FeaturedBanner(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(game.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(stringResource(R.string.featured_label), color = AppColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(game.nameRes), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.featured_cta), color = AppColors.TextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
    }
}
