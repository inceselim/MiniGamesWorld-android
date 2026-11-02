package com.eomer91.minigamesworld.ui.games.stack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Ekranda gorunen kat sayisi; kule bundan yukselince kamera yukari kayar. */
private const val VISIBLE_LEVELS = 14f

/** Blok yuksekligi, tahta genisligine oranla. */
private const val BLOCK_HEIGHT_RATIO = 0.075f

@Composable
fun StackScreen(viewModel: StackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    // Kare dongusu: sadece ekran on plandayken (reklamda ve arka planda durur).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last != 0L) viewModel.onFrame((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }
    }

    // Kamera: kule yukseldikce yumusakca yukari kayar.
    val cameraTarget = (state.game.height - (VISIBLE_LEVELS - 6f)).coerceAtLeast(0f)
    val camera by animateFloatAsState(cameraTarget, label = "camera")

    // "PERFECT" yazisi kisa sure gorunur.
    var showPerfect by remember { mutableStateOf(false) }
    LaunchedEffect(state.perfectEvent) {
        if (state.perfectEvent > 0) {
            showPerfect = true
            delay(700)
            showPerfect = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .padding(16.dp),
    ) {
        ScoreHeader(score = state.game.score, best = state.best, onRestart = restartWithAd)

        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(Unit) { detectTapGestures { viewModel.onTap() } },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawTower(state.game, camera)
            }

            PerfectBanner(
                visible = showPerfect && !state.game.gameOver,
                combo = state.game.combo,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            )

            if (!state.started) {
                Text(
                    stringResource(R.string.stack_tap_to_start),
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }

            if (state.game.gameOver) {
                GameOverPanel(
                    score = state.game.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = restartWithAd,
                    goldEarned = state.goldEarned,
                    detail = stringResource(R.string.stack_result, state.game.height, state.game.score),
                    modifier = Modifier.align(Alignment.Center).padding(20.dp),
                )
            }
        }
    }
}

/**
 * "KUSURSUZ" yazisi. Ayri fonksiyonda: Column icindeki Box'ta dogrudan cagrilinca
 * Kotlin ColumnScope.AnimatedVisibility'yi secip derleme hatasi veriyor.
 */
@Composable
private fun PerfectBanner(visible: Boolean, combo: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Text(
            if (combo > 1) stringResource(R.string.stack_perfect_combo, combo) else stringResource(R.string.stack_perfect),
            color = AppColors.Gold,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/** Kat numarasina gore renk: kule yukseldikce ton yavasca degisir. */
private fun blockColor(level: Int): Color = Color.hsv(((200f + level * 9f) % 360f), 0.55f, 0.95f)

private fun DrawScope.drawTower(game: StackGame, camera: Float) {
    // Arka plan: kule yukseldikce koyulasan gradyan
    val t = (game.height / 60f).coerceAtMost(1f)
    drawRect(
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E3A8A).copy(alpha = 1f - t * 0.6f),
                Color(0xFF020617),
            ),
        ),
    )

    val unit = size.width / StackEngine.BOARD_WIDTH
    val blockH = size.width * BLOCK_HEIGHT_RATIO
    val baseY = size.height - blockH * 1.5f
    fun yOf(level: Int) = baseY - (level - camera) * blockH

    fun block(x: Float, width: Float, level: Int, color: Color, yOffset: Float = 0f, alpha: Float = 1f) {
        val top = yOf(level) + yOffset
        if (top > size.height || top + blockH < 0f) return
        val left = x * unit
        val w = width * unit
        drawRect(color.copy(alpha = alpha), Offset(left, top), Size(w, blockH))
        // Ust kenarda ince acik serit: hafif derinlik hissi
        drawRect(Color.White.copy(alpha = 0.25f * alpha), Offset(left, top), Size(w, blockH * 0.18f))
        drawRect(Color.Black.copy(alpha = 0.18f * alpha), Offset(left, top + blockH * 0.82f), Size(w, blockH * 0.18f))
    }

    game.blocks.forEach { block(it.x, it.width, it.level, blockColor(it.level)) }

    // Dusen parcalar: asagi duser ve soner
    game.debris.forEach { d ->
        val fall = 0.5f * 2600f * d.age * d.age
        block(d.x, d.width, d.level, blockColor(d.level), yOffset = fall, alpha = (1f - d.age / StackEngine.DEBRIS_LIFETIME).coerceIn(0f, 1f))
    }

    if (!game.gameOver) {
        block(game.movingX, game.movingWidth, game.movingLevel, blockColor(game.movingLevel))
    }
}
