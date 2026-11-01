package com.eomer91.minigamesworld.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.data.GameEconomy
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.eomer91.minigamesworld.ui.components.EmptyState
import com.eomer91.minigamesworld.ui.components.GoldBadge
import com.eomer91.minigamesworld.ui.theme.AppColors

@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val scope = rememberCoroutineScope()
    /** Altin yetmediginde gosterilen "reklam izle" penceresi; gereken altin miktari. */
    var goldOffer by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameDetailEvent.StartGame ->
                    if (event.showAd) showInterstitial { onPlay(event.gameId) } else onPlay(event.gameId)
                is GameDetailEvent.NotEnoughGold -> goldOffer = event.required
                is GameDetailEvent.GoldRewarded ->
                    snackbar.showSnackbar(context.getString(R.string.gold_rewarded, event.amount))
            }
        }
    }

    val game = state.game
    if (game == null) {
        EmptyState(stringResource(R.string.empty_title), "")
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box {
                Image(
                    painter = painterResource(game.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    RoundIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    RoundIconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(if (state.isFavorite) R.string.favorite_remove else R.string.favorite_add),
                            tint = if (state.isFavorite) AppColors.Danger else Color.White,
                        )
                    }
                }
            }

            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(game.category.labelRes), color = AppColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    GoldBadge(state.gold)
                }
                Text(
                    stringResource(game.nameRes),
                    color = AppColors.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (state.bestScore > 0) {
                    Text(stringResource(R.string.best_score, state.bestScore), color = AppColors.Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    stringResource(game.descriptionRes),
                    color = AppColors.TextMuted,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Button(
                    onClick = viewModel::play,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play_now, game.entryCost), fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    goldOffer?.let { required ->
        AlertDialog(
            onDismissRequest = { goldOffer = null },
            icon = { Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = AppColors.Gold) },
            title = { Text(stringResource(R.string.not_enough_gold)) },
            text = {
                Text(stringResource(R.string.gold_offer_body, required, state.gold, GameEconomy.AD_REWARD_GOLD))
            },
            confirmButton = {
                Button(
                    onClick = {
                        goldOffer = null
                        showRewarded(
                            { viewModel.grantAdReward() },
                            { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } },
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                ) {
                    Text(stringResource(R.string.gold_offer_watch, GameEconomy.AD_REWARD_GOLD), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goldOffer = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
        content = content,
    )
}
