package com.eomer91.minigamesworld.ui.games.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.games.common.SwipeDirection
import com.eomer91.minigamesworld.ui.games.common.swipeGestures
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val BoardColor = Color(0xFF020617)
private val EmptyCellColor = Color(0xFF1E293B)
private val HeadColor = Color(0xFF22C55E)
private val BodyColor = Color(0xFF16A34A)
private val FoodColor = Color(0xFFF43F5E)

@Composable
fun SnakeScreen(viewModel: SnakeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val running = state.started && !state.game.gameOver

    // Oyun dongusu sadece ekran on plandayken calisir: uygulama arka plana gecince
    // ya da gecis reklami acilinca yilan kendiliginden durur.
    LaunchedEffect(running) {
        if (running) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(viewModel.tickMillis())
                    viewModel.tick()
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScoreHeader(score = state.game.score, best = state.best, onRestart = restartWithAd)

        SnakeBoard(
            game = state.game,
            gridSize = viewModel.gridSize,
            onSwipe = { viewModel.onSwipe(it.toSnakeDirection()) },
            modifier = Modifier.padding(top = 20.dp),
        )

        if (state.game.gameOver) {
            GameOverPanel(
                score = state.game.score,
                isNewRecord = state.isNewRecord,
                goldEarned = state.goldEarned,
                onRestart = restartWithAd,
                modifier = Modifier.padding(top = 20.dp),
            )
        } else {
            Text(
                stringResource(if (state.started) R.string.snake_hint else R.string.snake_hint_start),
                color = if (state.started) AppColors.TextSecondary else AppColors.Primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun SnakeBoard(
    game: SnakeGame,
    gridSize: Int,
    onSwipe: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(BoardColor)
            .swipeGestures(threshold = 20.dp, continuous = true, onSwipe = onSwipe)
            .padding(6.dp),
    ) {
        val gap = 2.dp.toPx()
        val cell = (size.width - gap * (gridSize - 1)) / gridSize
        val cellSize = Size(cell, cell)
        fun topLeft(c: Cell) = Offset(c.x * (cell + gap), c.y * (cell + gap))

        val emptyRadius = CornerRadius(cell * 0.25f)
        for (y in 0 until gridSize) for (x in 0 until gridSize) {
            drawRoundRect(EmptyCellColor, topLeft(Cell(x, y)), cellSize, emptyRadius)
        }

        drawCircle(
            color = FoodColor,
            radius = cell * 0.45f,
            center = topLeft(game.food) + Offset(cell / 2, cell / 2),
        )

        val headRadius = CornerRadius(cell * 0.35f)
        game.snake.forEachIndexed { index, part ->
            drawRoundRect(
                color = if (index == 0) HeadColor else BodyColor,
                topLeft = topLeft(part),
                size = cellSize,
                cornerRadius = if (index == 0) headRadius else emptyRadius,
            )
        }
    }
}

private fun SwipeDirection.toSnakeDirection(): SnakeDirection = when (this) {
    SwipeDirection.UP -> SnakeDirection.UP
    SwipeDirection.DOWN -> SnakeDirection.DOWN
    SwipeDirection.LEFT -> SnakeDirection.LEFT
    SwipeDirection.RIGHT -> SnakeDirection.RIGHT
}
