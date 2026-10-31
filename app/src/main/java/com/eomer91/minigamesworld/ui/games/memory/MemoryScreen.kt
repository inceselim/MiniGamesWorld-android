package com.eomer91.minigamesworld.ui.games.memory

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors

private val CardBackColor = Color(0xFF1E293B)
private val MatchedColor = Color(0xFF22C55E)

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScoreHeader(
            score = state.moves,
            best = state.best,
            onRestart = restartWithAd,
            scoreLabel = stringResource(R.string.game_moves),
            showBestAsMax = false,
        )

        Column(
            Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.cards.chunked(MemoryEngine.COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { card ->
                        FlipCard(
                            card = card,
                            onClick = { viewModel.onCardTap(card.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (state.completed) {
            GameOverPanel(
                score = state.finalScore,
                isNewRecord = state.isNewRecord,
                goldEarned = state.goldEarned,
                onRestart = restartWithAd,
                title = stringResource(R.string.memory_completed),
                titleColor = MatchedColor,
                detail = stringResource(R.string.memory_result, state.moves, state.finalScore),
                modifier = Modifier.padding(top = 20.dp),
            )
        } else {
            Text(
                stringResource(R.string.memory_hint),
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun FlipCard(card: MemoryCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val faceUp = card.isFaceUp || card.isMatched
    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "cardFlip",
    )
    val showFront = rotation > 90f
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(if (showFront) AppColors.Primary else CardBackColor)
            .then(if (card.isMatched && showFront) Modifier.border(2.dp, MatchedColor, shape) else Modifier)
            .clickable(enabled = !faceUp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (showFront) {
            // On yuz, karti cevirdigimiz icin aynalanir; geri ceviriyoruz.
            Text(card.symbol, fontSize = 32.sp, modifier = Modifier.graphicsLayer { rotationY = 180f })
        } else {
            Text("?", color = AppColors.TextSecondary, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}
