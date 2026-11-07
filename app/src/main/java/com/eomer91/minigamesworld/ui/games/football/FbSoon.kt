package com.eomer91.minigamesworld.ui.games.football

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.football.engine.FbWorld
import com.eomer91.minigamesworld.ui.theme.AppColors

// Adim 6c'de dolacak ekranlar (gecici).

@Composable private fun Soon() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(stringResource(R.string.fb_soon), color = AppColors.TextSecondary, fontSize = 16.sp)
}

@Composable fun FbTransfers(world: FbWorld, ui: FbUiState, vm: FootballViewModel) = Soon()
@Composable fun FbClubScreen(world: FbWorld, ui: FbUiState, vm: FootballViewModel) = Soon()
