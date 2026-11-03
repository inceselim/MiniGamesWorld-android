package com.eomer91.minigamesworld.ui.games.checkers

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
import androidx.compose.ui.graphics.Path
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
import kotlin.math.abs

private val LightSq = Color(0xFFE9C891)
private val DarkSq = Color(0xFFB9834E)
private val WhiteTop = Color(0xFFFFFBF0)
private val WhiteSide = Color(0xFFC9BFA8)
private val BlackTop = Color(0xFF3A3A40)
private val BlackSide = Color(0xFF111114)
private val KingGold = Color(0xFFF7C948)
private val Highlight = Color(0xFF4ADE80)

@Composable
fun CheckersScreen(viewModel: CheckersViewModel = hiltViewModel()) {
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
            CheckersPhase.LOADING -> Unit
            CheckersPhase.CHOOSING -> ModePicker(onPick = { level -> showInterstitial { viewModel.start(level) } })
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Header(state, onUndo = { showRewarded({ viewModel.undo() }, adNotReady) }, onModes = viewModel::backToChoosing)
                CheckersBoard(state, onSquare = viewModel::onSquare, modifier = Modifier.padding(top = 12.dp))
                PieceCounts(state, Modifier.padding(top = 10.dp))

                if (state.phase == CheckersPhase.FINISHED) {
                    val w = state.winner ?: 0
                    val title = when {
                        w == 0 -> stringResource(R.string.c4_draw)
                        state.vsBot && w == CheckersRules.WHITE -> stringResource(R.string.c4_you_win)
                        state.vsBot -> stringResource(R.string.c4_bot_wins)
                        w == CheckersRules.WHITE -> stringResource(R.string.ck_white_wins)
                        else -> stringResource(R.string.ck_black_wins)
                    }
                    val good = w == 0 || !state.vsBot || w == CheckersRules.WHITE
                    GameOverPanel(
                        score = state.finalScore,
                        isNewRecord = false,
                        onRestart = { showInterstitial { viewModel.rematch() } },
                        title = title,
                        titleColor = if (good) Color(0xFF22C55E) else AppColors.Danger,
                        detail = if (state.vsBot) stringResource(R.string.c4_result, state.finalScore) else "",
                        goldEarned = if (state.vsBot) state.goldEarned else 0,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextButton(onClick = viewModel::backToChoosing, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.c4_change_mode))
                    }
                } else {
                    Text(stringResource(R.string.ck_rules), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ModePicker(onPick: (CheckersLevel?) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.solitaire_choose), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        val options: List<Triple<CheckersLevel?, Int, Color>> = listOf(
            Triple(CheckersLevel.EASY, R.string.sudoku_easy, Color(0xFF22C55E)),
            Triple(CheckersLevel.NORMAL, R.string.sudoku_normal, AppColors.Primary),
            Triple(CheckersLevel.HARD, R.string.sudoku_hard, AppColors.Danger),
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
        Text(stringResource(R.string.ck_rules), color = AppColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun levelLabel(level: CheckersLevel) = when (level) {
    CheckersLevel.EASY -> R.string.sudoku_easy
    CheckersLevel.NORMAL -> R.string.sudoku_normal
    CheckersLevel.HARD -> R.string.sudoku_hard
}

@Composable
private fun Header(state: CheckersUiState, onUndo: () -> Unit, onModes: () -> Unit) {
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
        if (state.vsBot && state.phase == CheckersPhase.PLAYING) {
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

    if (state.phase == CheckersPhase.PLAYING) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (state.turn == CheckersRules.WHITE) WhiteTop else BlackTop)
                    .border(1.dp, AppColors.TextSecondary, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    state.botThinking || (state.vsBot && state.turn == CheckersRules.BLACK) -> stringResource(R.string.c4_bot_thinking)
                    state.vsBot -> stringResource(R.string.c4_your_turn)
                    state.turn == CheckersRules.WHITE -> stringResource(R.string.ck_white_turn)
                    else -> stringResource(R.string.ck_black_turn)
                },
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.mustCapture && !state.botThinking) {
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.ck_must_capture), color = AppColors.Danger, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PieceCounts(state: CheckersUiState, modifier: Modifier = Modifier) {
    val b = state.board
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        listOf(CheckersRules.WHITE to WhiteTop, CheckersRules.BLACK to BlackTop).forEach { (s, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(color).border(1.dp, AppColors.TextSecondary, CircleShape))
                Text(
                    "  ${b.count { it * s > 0 }}  ·  ♛ ${b.count { it * s == 2 }}",
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CheckersBoard(state: CheckersUiState, onSquare: (Int) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF5B3A1E))
            .padding(6.dp)
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
        fun topLeft(i: Int) = Offset((i % 8) * cell, (i / 8) * cell)
        fun center(i: Int) = topLeft(i) + Offset(cell / 2, cell / 2)

        for (i in 0 until 64) {
            val dark = ((i / 8) + (i % 8)) % 2 == 1
            drawRect(if (dark) DarkSq else LightSq, topLeft(i), Size(cell + 0.5f, cell + 0.5f))
        }
        // Son hamle
        state.lastMove?.let { m ->
            m.path.forEach { drawRect(Color(0x55FFE066), topLeft(it), Size(cell, cell)) }
        }
        // Secilen yol
        state.partial.forEach { drawRect(Highlight.copy(alpha = 0.35f), topLeft(it), Size(cell, cell)) }
        // Zorunlu alma: oynayabilecek taslara halka
        if (state.mustCapture && state.partial.isEmpty() && !state.botThinking) {
            state.movable.forEach { drawCircle(AppColors.Danger, cell * 0.47f, center(it), style = Stroke(cell * 0.06f)) }
        }

        // Taslar (secili tas yolun sonunda cizilir)
        val moving = state.partial.firstOrNull()
        val movingTo = state.partial.lastOrNull()
        for (i in 0 until 64) {
            val v = state.board[i]
            if (v == 0 || i == moving && state.partial.size > 1) continue
            val faded = i in state.partialCaptured
            piece(center(i), cell, v, if (faded) 0.3f else 1f, selected = i == moving)
        }
        if (moving != null && movingTo != null && state.partial.size > 1) {
            piece(center(movingTo), cell, state.board[moving], 1f, selected = true)
        }
        // Hedefler
        state.targets.forEach { t ->
            drawCircle(Highlight.copy(alpha = 0.85f), cell * 0.16f, center(t))
            drawCircle(Color.White.copy(alpha = 0.7f), cell * 0.16f, center(t), style = Stroke(cell * 0.03f))
        }
    }
}

/** Golgeli tas; dama ise altin taç isareti. */
private fun DrawScope.piece(c: Offset, cell: Float, v: Int, alpha: Float, selected: Boolean) {
    val white = v > 0
    val r = cell * 0.4f
    val thick = cell * 0.07f
    if (selected) drawCircle(Highlight, r + cell * 0.06f, c, alpha = alpha)
    drawCircle(Color.Black.copy(alpha = 0.35f * alpha), r, c + Offset(0f, thick * 1.3f))
    drawCircle(if (white) WhiteSide else BlackSide, r, c + Offset(0f, thick), alpha = alpha)
    drawCircle(
        Brush.radialGradient(
            listOf(if (white) Color.White else Color(0xFF5A5A62), if (white) WhiteTop else BlackTop),
            center = c - Offset(r * 0.3f, r * 0.3f),
            radius = r * 1.3f,
        ),
        r,
        c,
        alpha = alpha,
    )
    drawCircle((if (white) WhiteSide else Color.Black).copy(alpha = 0.6f * alpha), r * 0.68f, c, style = Stroke(cell * 0.025f))
    if (abs(v) == 2) {
        // Tac
        val w = r * 0.95f
        val h = r * 0.6f
        val left = c.x - w / 2
        val bottom = c.y + h * 0.45f
        val top = bottom - h
        val path = Path().apply {
            moveTo(left, bottom)
            lineTo(left, top + h * 0.25f)
            lineTo(left + w * 0.25f, top + h * 0.6f)
            lineTo(left + w * 0.5f, top)
            lineTo(left + w * 0.75f, top + h * 0.6f)
            lineTo(left + w, top + h * 0.25f)
            lineTo(left + w, bottom)
            close()
        }
        drawPath(path, KingGold, alpha = alpha)
        drawPath(path, Color(0xFF8A6100), alpha = alpha, style = Stroke(cell * 0.02f))
    }
}
