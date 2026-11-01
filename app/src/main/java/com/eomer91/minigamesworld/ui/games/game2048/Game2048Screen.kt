package com.eomer91.minigamesworld.ui.games.game2048

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.abs

private val CellGap = 10.dp

@Composable
fun Game2048Screen(viewModel: Game2048ViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    if (state.isLoading) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.Surface),
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScoreHeader(score = state.score, best = state.best, onRestart = restartWithAd)

        GameBoard(
            board = state.board,
            onSwipe = viewModel::move,
            modifier = Modifier.padding(top = 20.dp),
        )

        when {
            state.gameOver -> GameOverPanel(
                score = state.score,
                isNewRecord = state.isNewRecord,
                goldEarned = state.goldEarned,
                onRestart = restartWithAd,
                modifier = Modifier.padding(top = 20.dp),
            )
            state.reachedWinTile -> Hint(stringResource(R.string.g2048_win), AppColors.Gold)
            else -> Hint(stringResource(R.string.g2048_hint), AppColors.TextSecondary)
        }
    }
}

@Composable
private fun Hint(text: String, color: Color) {
    Text(
        text,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
    )
}

@Composable
private fun GameBoard(board: Board, onSwipe: (Direction) -> Unit, modifier: Modifier = Modifier) {
    val swipeThreshold = with(LocalDensity.current) { 30.dp.toPx() }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF020617))
            .pointerInput(Unit) {
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                    },
                    onDragEnd = {
                        val (dx, dy) = total
                        if (maxOf(abs(dx), abs(dy)) >= swipeThreshold) {
                            onSwipe(
                                if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (dy > 0) Direction.DOWN else Direction.UP
                                },
                            )
                        }
                    },
                )
            }
            .padding(CellGap),
    ) {
        val n = board.size
        val cellSize: Dp = (maxWidth - CellGap * (n - 1)) / n

        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            board.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    row.forEach { value -> Tile(value, cellSize) }
                }
            }
        }
    }
}

@Composable
private fun Tile(value: Int, size: Dp) {
    val background by animateColorAsState(tileColor(value), label = "tileColor")
    val fontScale = when {
        value < 100 -> 0.42f
        value < 1000 -> 0.34f
        else -> 0.27f
    }
    val fontSize = with(LocalDensity.current) { (size * fontScale).toSp() }

    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                value.toString(),
                color = if (value <= 4) Color(0xFF020617) else Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun tileColor(value: Int): Color = when (value) {
    0 -> Color(0xFF1E293B)
    2 -> Color(0xFFE0F2FE)
    4 -> Color(0xFFBAE6FD)
    8 -> Color(0xFF7DD3FC)
    16 -> Color(0xFF38BDF8)
    32 -> Color(0xFF0EA5E9)
    64 -> Color(0xFF0284C7)
    128 -> Color(0xFFFACC15)
    256 -> Color(0xFFF59E0B)
    512 -> Color(0xFFFB7185)
    1024 -> Color(0xFFA78BFA)
    2048 -> Color(0xFF22C55E)
    else -> Color(0xFF14B8A6)
}
