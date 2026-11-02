package com.eomer91.minigamesworld.ui.games.connect4

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.launch

private val BoardBlue = Color(0xFF1D4ED8)
private val HoleColor = Color(0xFF0B1020)
private val P1Color = Color(0xFFEF4444)
private val P2Color = Color(0xFFFACC15)

@Composable
fun Connect4Screen(viewModel: Connect4ViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val rematchWithAd = { showInterstitial { viewModel.rematch() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        if (state.phase == C4Phase.CHOOSING) {
            ModePicker(onPick = viewModel::start)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Header(state, onUndo = { showRewarded({ viewModel.undo() }, adNotReady) }, onModes = viewModel::backToChoosing)
                C4Board(state, onColumn = viewModel::onColumnTap, modifier = Modifier.padding(top = 14.dp))

                if (state.phase == C4Phase.FINISHED) {
                    val title = when {
                        state.draw -> stringResource(R.string.c4_draw)
                        state.vsBot && state.winner == 1 -> stringResource(R.string.c4_you_win)
                        state.vsBot -> stringResource(R.string.c4_bot_wins)
                        else -> stringResource(R.string.c4_player_wins, state.winner)
                    }
                    val good = state.draw || !state.vsBot || state.winner == 1
                    GameOverPanel(
                        score = state.finalScore,
                        isNewRecord = false,
                        onRestart = rematchWithAd,
                        title = title,
                        titleColor = if (good) Color(0xFF22C55E) else AppColors.Danger,
                        detail = if (state.vsBot) stringResource(R.string.c4_result, state.finalScore) else "",
                        goldEarned = if (state.vsBot) state.goldEarned else 0,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    TextButton(onClick = viewModel::backToChoosing, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.c4_change_mode))
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ModePicker(onPick: (BotLevel?) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.solitaire_choose), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        val options: List<Triple<BotLevel?, Int, Color>> = listOf(
            Triple(BotLevel.EASY, R.string.sudoku_easy, Color(0xFF22C55E)),
            Triple(BotLevel.NORMAL, R.string.sudoku_normal, AppColors.Primary),
            Triple(BotLevel.HARD, R.string.sudoku_hard, AppColors.Danger),
            Triple(null, R.string.c4_two_players, AppColors.Gold),
        )
        options.forEach { (level, label, accent) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.SurfaceHigh)
                    .border(2.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onPick(level) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    if (level != null) stringResource(R.string.c4_vs_bot, stringResource(label)) else stringResource(label),
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (level != null) stringResource(R.string.c4_level_info, level.winScore) else stringResource(R.string.c4_two_players_info),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(state: C4UiState, onUndo: () -> Unit, onModes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                state.level?.let { stringResource(R.string.c4_vs_bot, stringResource(levelLabel(it))) } ?: stringResource(R.string.c4_two_players),
                color = AppColors.Primary,
                fontWeight = FontWeight.ExtraBold,
            )
            if (state.vsBot) {
                Text(stringResource(R.string.c4_series, state.wins, state.losses), color = AppColors.TextSecondary, fontSize = 13.sp)
            }
        }
        if (state.vsBot && state.phase == C4Phase.PLAYING) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.canUndo,
                shape = RoundedCornerShape(12.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.height(40.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = AppColors.Gold, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Button(
            onClick = onModes,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
            modifier = Modifier.height(40.dp),
        ) {
            Text(stringResource(R.string.sudoku_new), fontWeight = FontWeight.Black)
        }
    }

    if (state.phase == C4Phase.PLAYING) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(if (state.turn == 1) P1Color else P2Color))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    state.botThinking -> stringResource(R.string.c4_bot_thinking)
                    state.vsBot && state.turn == 1 -> stringResource(R.string.c4_your_turn)
                    state.vsBot -> stringResource(R.string.c4_bot_thinking)
                    else -> stringResource(R.string.c4_player_turn, state.turn)
                },
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun levelLabel(level: BotLevel) = when (level) {
    BotLevel.EASY -> R.string.sudoku_easy
    BotLevel.NORMAL -> R.string.sudoku_normal
    BotLevel.HARD -> R.string.sudoku_hard
}

@Composable
private fun C4Board(state: C4UiState, onColumn: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cols = Connect4Engine.COLS
    val rows = Connect4Engine.ROWS

    // Son dusen tasin animasyonu: ustten yerine duser (hafif sekme).
    val drop = remember { Animatable(1f) }
    LaunchedEffect(state.lastDrop, state.board.count { it != 0 }) {
        if (state.lastDrop != null) {
            drop.snapTo(0f)
            drop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    // Kazanan dortlu yanip soner
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseValue",
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(cols / rows.toFloat())
            .clip(RoundedCornerShape(18.dp))
            .background(BoardBlue)
            .pointerInput(Unit) {
                detectTapGestures { p -> onColumn((p.x / (size.width / cols)).toInt().coerceIn(0, cols - 1)) }
            },
    ) {
        val cell = size.width / cols
        val r = cell * 0.40f
        fun center(row: Int, col: Int) = Offset(col * cell + cell / 2, row * cell + cell / 2)

        for (row in 0 until rows) for (col in 0 until cols) {
            val i = row * cols + col
            val v = state.board[i]
            val c = center(row, col)
            drawCircle(HoleColor, r, c)
            if (v == 0) continue
            val color = if (v == 1) P1Color else P2Color
            val pos = if (i == state.lastDrop) {
                // Sutunun ustunden (-1. satir) yerine
                val startY = -cell / 2
                Offset(c.x, startY + (c.y - startY) * drop.value)
            } else c
            drawPiece(pos, r, color)
            if (i in state.winningCells) drawCircle(Color.White.copy(alpha = pulse), r * 0.85f, c, style = Stroke(r * 0.18f))
        }
    }
}

private fun DrawScope.drawPiece(c: Offset, r: Float, color: Color) {
    drawCircle(color, r, c)
    drawCircle(Color.Black.copy(alpha = 0.15f), r * 0.72f, c, style = Stroke(r * 0.1f))
    drawCircle(Color.White.copy(alpha = 0.3f), r * 0.22f, c + Offset(-r * 0.35f, -r * 0.35f))
}
