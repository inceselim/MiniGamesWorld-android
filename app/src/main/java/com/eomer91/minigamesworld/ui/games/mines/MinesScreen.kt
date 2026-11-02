package com.eomer91.minigamesworld.ui.games.mines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val HiddenColor = Color(0xFF334155)
private val HiddenHighlight = Color(0xFF475569)
private val OpenColor = Color(0xFF0F172A)
private val ExplodedColor = Color(0xFF7F1D1D)
private val FlagColor = Color(0xFFEF4444)

/** Klasik sayi renkleri (koyu temaya uyarlanmis). */
private val NumberColors = listOf(
    Color(0xFF60A5FA), Color(0xFF4ADE80), Color(0xFFF87171), Color(0xFFC084FC),
    Color(0xFFFB923C), Color(0xFF2DD4BF), Color(0xFFE2E8F0), Color(0xFF94A3B8),
)

@Composable
fun MinesScreen(viewModel: MinesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val newGameWithAd = { showInterstitial { viewModel.backToChoosing() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Sure: sadece ekran on plandayken
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val playing = state.phase == MinesPhase.PLAYING
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(1_000)
                viewModel.tick()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        when (state.phase) {
            MinesPhase.LOADING -> Unit
            MinesPhase.CHOOSING -> DifficultyPicker(onPick = viewModel::start)
            else -> GameContent(
                state = state,
                viewModel = viewModel,
                onNewGame = newGameWithAd,
                onSecondChance = { showRewarded({ viewModel.secondChance() }, adNotReady) },
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DifficultyPicker(onPick: (MinesDifficulty) -> Unit) {
    val accents = listOf(Color(0xFF22C55E), AppColors.Primary, AppColors.Danger)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.sudoku_choose),
            color = AppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        MinesDifficulty.entries.forEachIndexed { index, d ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.SurfaceHigh)
                    .border(2.dp, accents[index].copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onPick(d) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(stringResource(d.labelRes), color = accents[index], fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    stringResource(R.string.mines_difficulty_info, d.cols, d.rows, d.mines),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GameContent(
    state: MinesUiState,
    viewModel: MinesViewModel,
    onNewGame: () -> Unit,
    onSecondChance: () -> Unit,
) {
    val board = state.board ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        // Ust bilgi
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(state.difficulty.labelRes), color = AppColors.Primary, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = FlagColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(board.minesLeft.toString(), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(14.dp))
                    Text("%d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60), color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onNewGame,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                modifier = Modifier.height(42.dp),
            ) {
                Text(stringResource(R.string.sudoku_new), fontWeight = FontWeight.Black)
            }
        }

        // Kaz / Bayrak modu
        if (state.phase == MinesPhase.PLAYING) {
            Row(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.SurfaceHigh)
                    .padding(4.dp),
            ) {
                ModeButton(stringResource(R.string.mines_dig), Icons.Rounded.Search, selected = !state.flagMode, modifier = Modifier.weight(1f)) {
                    if (state.flagMode) viewModel.toggleFlagMode()
                }
                ModeButton(stringResource(R.string.mines_flag), Icons.Rounded.Flag, selected = state.flagMode, modifier = Modifier.weight(1f)) {
                    if (!state.flagMode) viewModel.toggleFlagMode()
                }
            }
        }

        MinesGrid(
            board = board,
            explodedAt = state.explodedAt,
            onTap = viewModel::onTap,
            onLongPress = viewModel::onLongPress,
            modifier = Modifier.padding(top = 10.dp),
        )

        when (state.phase) {
            MinesPhase.WON -> GameOverPanel(
                score = state.finalScore,
                isNewRecord = state.isNewRecord,
                onRestart = onNewGame,
                title = stringResource(R.string.mines_won),
                titleColor = Color(0xFF22C55E),
                detail = stringResource(
                    R.string.mines_result,
                    "%d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60),
                    state.finalScore,
                ),
                goldEarned = state.goldEarned,
                modifier = Modifier.padding(top = 14.dp),
            )
            MinesPhase.LOST -> {
                if (!state.secondChanceUsed) {
                    Button(
                        onClick = onSecondChance,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.mines_second_chance), fontWeight = FontWeight.Black)
                    }
                }
                GameOverPanel(
                    score = 0,
                    isNewRecord = false,
                    onRestart = onNewGame,
                    title = stringResource(R.string.mines_lost),
                    detail = stringResource(R.string.mines_lost_body),
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            else -> Text(
                stringResource(R.string.mines_hint),
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (selected) AppColors.OnPrimary else AppColors.TextMuted
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MinesGrid(
    board: MineBoard,
    explodedAt: Int?,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell: Dp = maxWidth / board.cols
        Column(Modifier.clip(RoundedCornerShape(10.dp))) {
            for (r in 0 until board.rows) {
                Row {
                    for (c in 0 until board.cols) {
                        val i = r * board.cols + c
                        MineCellView(board.cells[i], cell, exploded = i == explodedAt, onTap = { onTap(i) }, onLongPress = { onLongPress(i) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MineCellView(cell: MineCell, size: Dp, exploded: Boolean, onTap: () -> Unit, onLongPress: () -> Unit) {
    val density = LocalDensity.current
    val background = when {
        exploded -> ExplodedColor
        cell.revealed -> OpenColor
        else -> HiddenColor
    }
    Box(
        Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(size * 0.15f))
            .background(background)
            .pointerInput(cell.revealed, cell.flagged) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            !cell.revealed && !cell.flagged -> Canvas(Modifier.fillMaxSize()) {
                // Kapali hucre: ustte hafif parlaklik, kabartma hissi
                drawRect(HiddenHighlight, size = this.size.copy(height = this.size.height * 0.18f))
            }
            cell.flagged && !cell.revealed -> Icon(Icons.Rounded.Flag, contentDescription = null, tint = FlagColor, modifier = Modifier.size(size * 0.6f))
            cell.mine -> Canvas(Modifier.fillMaxSize()) {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = this.size.minDimension * 0.24f
                for (k in 0 until 4) {
                    val a = Math.PI * k / 4
                    val dx = (kotlin.math.cos(a) * r * 1.6f).toFloat()
                    val dy = (kotlin.math.sin(a) * r * 1.6f).toFloat()
                    drawLine(Color.Black, c - Offset(dx, dy), c + Offset(dx, dy), strokeWidth = r * 0.35f)
                }
                drawCircle(Color.Black, r, c)
                drawCircle(Color.White.copy(alpha = 0.6f), r * 0.3f, c - Offset(r * 0.35f, r * 0.35f))
            }
            cell.adjacent > 0 -> Text(
                cell.adjacent.toString(),
                color = NumberColors[(cell.adjacent - 1).coerceIn(0, 7)],
                fontSize = with(density) { (size * 0.55f).toSp() },
                fontWeight = FontWeight.Black,
            )
        }
    }
}
