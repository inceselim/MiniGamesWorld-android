package com.eomer91.minigamesworld.ui.games.match3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val BoardColor = Color(0xFF020617)
private val CellA = Color(0xFF0F172A)
private val CellB = Color(0xFF111C33)

/** Her tas tipinin rengi; sekiller [drawGem] icinde (renk korlugu icin sekil de farkli). */
private val GemColors = listOf(
    Color(0xFFEF4444), // 0 daire
    Color(0xFFF97316), // 1 kare
    Color(0xFFFACC15), // 2 elmas
    Color(0xFF22C55E), // 3 altigen
    Color(0xFF3B82F6), // 4 ucgen
    Color(0xFFA855F7), // 5 yildiz
)

@Composable
fun Match3Screen(viewModel: Match3ViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

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
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ScoreHeader(score = state.score, best = state.best, onRestart = restartWithAd)

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.match3_moves, state.movesLeft),
                    color = if (state.movesLeft <= 5) AppColors.Danger else AppColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                when {
                    state.shuffling -> Text(stringResource(R.string.match3_shuffling), color = AppColors.Gold, fontWeight = FontWeight.Bold)
                    state.chain >= 2 -> Text(stringResource(R.string.match3_chain, state.chain), color = AppColors.Gold, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }

            Match3Board(
                state = state,
                boardSize = viewModel.boardSize,
                onTap = viewModel::onTap,
                onSwipe = viewModel::onSwipe,
                modifier = Modifier.padding(top = 10.dp),
            )

            if (state.gameOver) {
                if (!state.bonusUsed) {
                    Button(
                        onClick = { showRewarded({ viewModel.addBonusMoves() }, adNotReady) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
                    ) {
                        Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.match3_bonus_moves, Match3Engine.BONUS_MOVES), fontWeight = FontWeight.Black)
                    }
                }
                GameOverPanel(
                    score = state.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = restartWithAd,
                    title = stringResource(R.string.match3_out_of_moves),
                    goldEarned = state.goldEarned,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Text(
                    stringResource(R.string.match3_hint),
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp).align(Alignment.CenterHorizontally),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Match3Board(
    state: Match3UiState,
    boardSize: Int,
    onTap: (Int) -> Unit,
    onSwipe: (cell: Int, dRow: Int, dCol: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeThreshold = with(LocalDensity.current) { 18.dp.toPx() }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(BoardColor)
            .pointerInput(boardSize) {
                detectTapGestures { offset ->
                    val cell = cellAt(offset, size.width.toFloat(), boardSize)
                    if (cell != null) onTap(cell)
                }
            }
            .pointerInput(boardSize) {
                var start = Offset.Zero
                var total = Offset.Zero
                var fired = false
                detectDragGestures(
                    onDragStart = { start = it; total = Offset.Zero; fired = false },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        if (!fired && maxOf(abs(total.x), abs(total.y)) >= swipeThreshold) {
                            fired = true
                            cellAt(start, size.width.toFloat(), boardSize)?.let { cell ->
                                if (abs(total.x) > abs(total.y)) {
                                    onSwipe(cell, 0, if (total.x > 0) 1 else -1)
                                } else {
                                    onSwipe(cell, if (total.y > 0) 1 else -1, 0)
                                }
                            }
                        }
                    },
                )
            },
    ) {
        val cell: Dp = maxWidth / boardSize

        // Dama desenli zemin
        Canvas(Modifier.matchParentSize()) {
            val step = size.width / boardSize
            for (r in 0 until boardSize) for (c in 0 until boardSize) {
                drawRect(if ((r + c) % 2 == 0) CellA else CellB, Offset(c * step, r * step), Size(step, step))
            }
        }

        state.board.forEachIndexed { i, gem ->
            if (gem != null) {
                key(gem.id) {
                    GemView(
                        gem = gem,
                        row = i / boardSize,
                        col = i % boardSize,
                        cell = cell,
                        selected = state.selected == i,
                        clearing = gem.id in state.clearing,
                    )
                }
            }
        }
    }
}

private fun cellAt(offset: Offset, widthPx: Float, n: Int): Int? {
    val step = widthPx / n
    val c = (offset.x / step).toInt()
    val r = (offset.y / step).toInt()
    return if (r in 0 until n && c in 0 until n) r * n + c else null
}

@Composable
private fun GemView(gem: Gem, row: Int, col: Int, cell: Dp, selected: Boolean, clearing: Boolean) {
    // Konum animasyonu: yeni taslar tahtanin ustunden duser, takaslar yana kayar.
    val y = remember(gem.id) { Animatable(gem.spawnRow.toFloat()) }
    val x = remember(gem.id) { Animatable(col.toFloat()) }
    LaunchedEffect(row) {
        val distance = abs(row - y.value)
        y.animateTo(row.toFloat(), tween(durationMillis = (110 + distance * 45).toInt().coerceAtMost(420), easing = FastOutSlowInEasing))
    }
    LaunchedEffect(col) { x.animateTo(col.toFloat(), tween(durationMillis = 160, easing = FastOutSlowInEasing)) }

    val scale by animateFloatAsState(
        targetValue = when {
            clearing -> 0f
            selected -> 1.12f
            else -> 1f
        },
        animationSpec = tween(durationMillis = if (clearing) 200 else 120),
        label = "gemScale",
    )
    val cellPx = with(LocalDensity.current) { cell.toPx() }

    Box(
        Modifier
            .offset { IntOffset((x.value * cellPx).roundToInt(), (y.value * cellPx).roundToInt()) }
            .size(cell)
            .padding(3.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (selected) {
                drawRoundRect(Color.White.copy(alpha = 0.18f), cornerRadius = CornerRadius(size.width * 0.25f))
                drawRoundRect(Color.White, cornerRadius = CornerRadius(size.width * 0.25f), style = Stroke(width = 2.dp.toPx()))
            }
            drawGem(gem.type)
        }
    }
}

/** Tas cizimi: her tipin hem rengi hem sekli farkli. */
private fun DrawScope.drawGem(type: Int) {
    val color = GemColors[type % GemColors.size]
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.42f

    fun polygon(points: Int, radius: Float, rotation: Float = -PI.toFloat() / 2f, inner: Float? = null): Path = Path().apply {
        val total = if (inner != null) points * 2 else points
        for (k in 0 until total) {
            val rr = if (inner != null && k % 2 == 1) inner else radius
            val a = rotation + k * (2f * PI.toFloat() / total)
            val p = Offset(c.x + rr * cos(a), c.y + rr * sin(a))
            if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

    when (type) {
        0 -> drawCircle(color, r, c)
        1 -> drawRoundRect(color, Offset(c.x - r * 0.88f, c.y - r * 0.88f), Size(r * 1.76f, r * 1.76f), CornerRadius(r * 0.35f))
        2 -> drawPath(polygon(4, r * 1.08f), color)
        3 -> drawPath(polygon(6, r, rotation = 0f), color)
        4 -> drawPath(polygon(3, r * 1.12f).also { it.translate(Offset(0f, r * 0.15f)) }, color)
        else -> drawPath(polygon(5, r * 1.1f, inner = r * 0.5f), color)
    }
    // Parlama
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.22f, Offset(c.x - r * 0.35f, c.y - r * 0.38f))
}
