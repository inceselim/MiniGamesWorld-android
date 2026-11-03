package com.eomer91.minigamesworld.ui.games.chess

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val LightSq = Color(0xFFF0D9B5)
private val DarkSq = Color(0xFFB58863)
private val LastMove = Color(0x66F6E05E)
private val SelectColor = Color(0x8060C060)

@Composable
fun ChessScreen(viewModel: ChessViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
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
        when (state.phase) {
            ChessPhase.LOADING -> Unit
            ChessPhase.CHOOSING -> ModePicker(onPick = { level -> showInterstitial { viewModel.start(level) } })
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Header(state, onUndo = { showRewarded({ viewModel.undo() }, adNotReady) }, onModes = viewModel::backToChoosing)
                CapturedRow(state.captured.filter { it > 0 }, Modifier.padding(top = 8.dp))
                Box(Modifier.padding(top = 4.dp)) {
                    ChessBoard(state, onSquare = viewModel::onSquare)
                    state.promotion?.let { PromotionPicker(state.side, onPick = viewModel::promote, onCancel = viewModel::cancelPromotion) }
                }
                CapturedRow(state.captured.filter { it < 0 }, Modifier.padding(top = 4.dp))

                if (state.phase == ChessPhase.FINISHED) {
                    val w = state.winner ?: 0
                    val title = when {
                        w == 0 -> stringResource(R.string.c4_draw)
                        state.vsBot && w == Chess.WHITE -> stringResource(R.string.c4_you_win)
                        state.vsBot -> stringResource(R.string.c4_bot_wins)
                        w == Chess.WHITE -> stringResource(R.string.ck_white_wins)
                        else -> stringResource(R.string.ck_black_wins)
                    }
                    val reason = when (state.end) {
                        ChessEnd.CHECKMATE -> stringResource(R.string.ch_checkmate)
                        ChessEnd.STALEMATE -> stringResource(R.string.ch_stalemate)
                        ChessEnd.INSUFFICIENT -> stringResource(R.string.ch_insufficient)
                        ChessEnd.FIFTY -> stringResource(R.string.ch_fifty)
                        ChessEnd.REPETITION -> stringResource(R.string.ch_repetition)
                        null -> ""
                    }
                    val good = w == 0 || !state.vsBot || w == Chess.WHITE
                    GameOverPanel(
                        score = state.finalScore,
                        isNewRecord = false,
                        onRestart = { showInterstitial { viewModel.rematch() } },
                        title = title,
                        titleColor = if (good) Color(0xFF22C55E) else AppColors.Danger,
                        detail = reason + if (state.vsBot) " · " + stringResource(R.string.c4_result, state.finalScore) else "",
                        goldEarned = if (state.vsBot) state.goldEarned else 0,
                        modifier = Modifier.padding(top = 12.dp),
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
private fun ModePicker(onPick: (ChessLevel?) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.solitaire_choose), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        val options: List<Triple<ChessLevel?, Int, Color>> = listOf(
            Triple(ChessLevel.EASY, R.string.sudoku_easy, Color(0xFF22C55E)),
            Triple(ChessLevel.NORMAL, R.string.sudoku_normal, AppColors.Primary),
            Triple(ChessLevel.HARD, R.string.sudoku_hard, AppColors.Danger),
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

private fun levelLabel(level: ChessLevel) = when (level) {
    ChessLevel.EASY -> R.string.sudoku_easy
    ChessLevel.NORMAL -> R.string.sudoku_normal
    ChessLevel.HARD -> R.string.sudoku_hard
}

@Composable
private fun Header(state: ChessUiState, onUndo: () -> Unit, onModes: () -> Unit) {
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
        if (state.vsBot && state.phase == ChessPhase.PLAYING) {
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

    if (state.phase == ChessPhase.PLAYING) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (state.side == Chess.WHITE) Color(0xFFFAF6EC) else Color(0xFF303036))
                    .border(1.dp, AppColors.TextSecondary, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    state.botThinking || (state.vsBot && state.side == Chess.BLACK) -> stringResource(R.string.c4_bot_thinking)
                    state.vsBot -> stringResource(R.string.c4_your_turn)
                    state.side == Chess.WHITE -> stringResource(R.string.ck_white_turn)
                    else -> stringResource(R.string.ck_black_turn)
                },
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.checkSquare >= 0) {
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.ch_check), color = AppColors.Danger, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CapturedRow(pieces: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(22.dp)) {
        val s = size.height
        pieces.forEachIndexed { i, v -> drawChessPiece(v, i * s * 0.62f, 0f, s) }
    }
}

@Composable
private fun ChessBoard(state: ChessUiState, onSquare: (Int) -> Unit) {
    val labelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val cell = size.width / 8f
                    val c = (p.x / cell).toInt().coerceIn(0, 7)
                    val r = (p.y / cell).toInt().coerceIn(0, 7)
                    onSquare(r * 8 + c)
                }
            },
    ) {
        val cell = size.width / 8f
        fun tl(i: Int) = Offset((i % 8) * cell, (i / 8) * cell)
        for (i in 0 until 64) {
            val light = ((i / 8) + (i % 8)) % 2 == 0
            drawRect(if (light) LightSq else DarkSq, tl(i), Size(cell + 0.5f, cell + 0.5f))
        }
        if (state.lastFrom >= 0) drawRect(LastMove, tl(state.lastFrom), Size(cell, cell))
        if (state.lastTo >= 0) drawRect(LastMove, tl(state.lastTo), Size(cell, cell))
        if (state.selected >= 0) drawRect(SelectColor, tl(state.selected), Size(cell, cell))
        if (state.checkSquare >= 0) {
            val c = tl(state.checkSquare) + Offset(cell / 2, cell / 2)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFF2020), Color.Transparent), center = c, radius = cell * 0.7f), cell * 0.7f, c)
        }
        // Koordinatlar
        labelPaint.textSize = cell * 0.2f
        drawIntoCanvas { canvas ->
            for (k in 0 until 8) {
                labelPaint.color = (if (k % 2 == 0) DarkSq else LightSq).toArgb()
                canvas.nativeCanvas.drawText("${8 - k}", cell * 0.05f, k * cell + cell * 0.22f, labelPaint)
                labelPaint.color = (if (k % 2 == 1) DarkSq else LightSq).toArgb()
                canvas.nativeCanvas.drawText("${'a' + k}", k * cell + cell * 0.82f, size.height - cell * 0.06f, labelPaint)
            }
        }
        for (i in 0 until 64) {
            val v = state.board[i]
            if (v != 0) drawChessPiece(v, tl(i).x, tl(i).y, cell)
        }
        state.targets.forEach { t ->
            val c = tl(t) + Offset(cell / 2, cell / 2)
            if (state.board[t] != 0) drawCircle(Color(0x99000000), cell * 0.46f, c, style = Stroke(cell * 0.07f))
            else drawCircle(Color(0x55000000), cell * 0.15f, c)
        }
    }
}

@Composable
private fun PromotionPicker(side: Int, onPick: (Int) -> Unit, onCancel: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(AppColors.Surface)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.ch_promote), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (t in intArrayOf(Chess.QUEEN, Chess.ROOK, Chess.BISHOP, Chess.KNIGHT)) {
                    Canvas(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightSq)
                            .clickable { onPick(t) },
                    ) { drawChessPiece(t * side, 0f, 0f, size.width) }
                }
            }
        }
    }
}
