package com.eomer91.minigamesworld.ui.games.pool

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val BALL_COLORS = listOf(
    0xFFFFFFF5, 0xFFF7C948, 0xFF2563EB, 0xFFDC2626, 0xFF7C3AED, 0xFFF97316, 0xFF16A34A, 0xFF7F1D1D,
    0xFF111111, 0xFFF7C948, 0xFF2563EB, 0xFFDC2626, 0xFF7C3AED, 0xFFF97316, 0xFF16A34A, 0xFF7F1D1D,
)

@Composable
fun PoolScreen(viewModel: PoolViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.shooting) {
        if (state.shooting) {
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
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        if (state.phase == PoolPhase.CHOOSING) {
            ModePicker(onPick = { l -> showInterstitial { viewModel.start(l) } })
        } else {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Header(state, viewModel)
                Row(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) { TableCanvas(state, viewModel) }
                    PowerBar(state, viewModel)
                }
                if (state.phase == PoolPhase.FINISHED) {
                    val title = when {
                        state.vsBot && state.winner == 0 -> stringResource(R.string.c4_you_win)
                        state.vsBot -> stringResource(R.string.c4_bot_wins)
                        else -> stringResource(R.string.c4_player_wins, state.winner + 1)
                    }
                    GameOverPanel(
                        score = state.finalScore,
                        isNewRecord = false,
                        onRestart = { showInterstitial { viewModel.rematch() } },
                        title = title,
                        titleColor = if (!state.vsBot || state.winner == 0) Color(0xFF22C55E) else AppColors.Danger,
                        detail = if (state.vsBot) stringResource(R.string.c4_result, state.finalScore) else "",
                        goldEarned = if (state.vsBot) state.goldEarned else 0,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    TextButton(onClick = viewModel::backToChoosing, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.c4_change_mode))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePicker(onPick: (PoolLevel?) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.solitaire_choose), color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        val options: List<Triple<PoolLevel?, Int, Color>> = listOf(
            Triple(PoolLevel.EASY, R.string.sudoku_easy, Color(0xFF22C55E)),
            Triple(PoolLevel.NORMAL, R.string.sudoku_normal, AppColors.Primary),
            Triple(PoolLevel.HARD, R.string.sudoku_hard, AppColors.Danger),
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
        Text(stringResource(R.string.pl_rules), color = AppColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun Header(state: PoolUiState, viewModel: PoolViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = viewModel.frame
    val g = viewModel.game
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (p in 0..1) {
            val active = state.turn == p && state.phase == PoolPhase.PLAYING
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) AppColors.Primary.copy(alpha = 0.25f) else AppColors.SurfaceHigh)
                    .border(if (active) 2.dp else 0.dp, if (active) AppColors.Primary else Color.Transparent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    when {
                        state.vsBot && p == 0 -> stringResource(R.string.pl_you)
                        state.vsBot -> stringResource(R.string.pl_computer)
                        else -> stringResource(R.string.pl_player, p + 1)
                    },
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                )
                val group = state.groups.getOrNull(p)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group == null) {
                        Text(stringResource(R.string.pl_open), color = AppColors.TextSecondary, fontSize = 11.sp)
                    } else {
                        val ids = if (group == PoolGroup.SOLID) 1..7 else 9..15
                        ids.forEach { id ->
                            val left = !g.table.balls[id].pocketed
                            Box(
                                Modifier
                                    .padding(end = 2.dp)
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(if (left) Color(BALL_COLORS[id]) else Color(0x22FFFFFF))
                                    .border(if (group == PoolGroup.STRIPE && left) 2.dp else 0.dp, Color.White, CircleShape),
                            )
                        }
                    }
                }
            }
            if (p == 0) Spacer(Modifier.width(8.dp))
        }
    }
    val msg = when {
        state.phase != PoolPhase.PLAYING -> null
        state.botThinking -> stringResource(R.string.c4_bot_thinking)
        state.event == PoolEvent.FOUL -> stringResource(R.string.pl_foul)
        state.ballInHand && state.humanTurn -> stringResource(R.string.pl_ball_in_hand)
        state.event == PoolEvent.GROUPS_SET -> stringResource(R.string.pl_groups_set)
        state.humanTurn -> stringResource(R.string.pl_hint)
        else -> null
    }
    Text(msg ?: " ", color = if (state.event == PoolEvent.FOUL) AppColors.Danger else AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

/** Dikey masa: masa x -> ekran asagi, masa y -> ekran saga. */
private class TableGeo(w: Float, h: Float) {
    val rail = min(w, h) * 0.06f
    val scale = min((w - 2 * rail) / PoolTable.W, (h - 2 * rail) / PoolTable.L)
    val left = (w - PoolTable.W * scale) / 2f
    val top = (h - PoolTable.L * scale) / 2f
    fun toScreen(x: Float, y: Float) = Offset(left + y * scale, top + x * scale)
    fun toTable(p: Offset) = Offset((p.y - top) / scale, (p.x - left) / scale) // (x, y)
}

@Composable
private fun TableCanvas(state: PoolUiState, viewModel: PoolViewModel) {
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var movingCue = false
                detectDragGestures(
                    onDragStart = { o ->
                        val g = TableGeo(size.width.toFloat(), size.height.toFloat())
                        val t = g.toTable(o)
                        movingCue = viewModel.canMoveCue(t.x, t.y)
                        if (!movingCue) viewModel.aimAt(t.x, t.y)
                    },
                ) { change, _ ->
                    change.consume()
                    val g = TableGeo(size.width.toFloat(), size.height.toFloat())
                    val t = g.toTable(change.position)
                    if (movingCue) viewModel.moveCue(t.x, t.y) else viewModel.aimAt(t.x, t.y)
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown(requireUnconsumed = false)
                    val g = TableGeo(size.width.toFloat(), size.height.toFloat())
                    val t = g.toTable(d.position)
                    if (!viewModel.canMoveCue(t.x, t.y)) viewModel.aimAt(t.x, t.y)
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val f = viewModel.frame
        drawTable(viewModel, state, paint)
    }
}

private fun DrawScope.drawTable(vm: PoolViewModel, state: PoolUiState, paint: android.graphics.Paint) {
    val g = TableGeo(size.width, size.height)
    val t = vm.game.table
    val tw = PoolTable.W * g.scale
    val th = PoolTable.L * g.scale
    // Ahsap kenar ve kece
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFF7B4A26), Color(0xFF4A2A14))), Offset(g.left - g.rail, g.top - g.rail), Size(tw + 2 * g.rail, th + 2 * g.rail), CornerRadius(g.rail * 0.8f))
    drawRect(Color(0xFF0B6E3A), Offset(g.left - g.rail * 0.35f, g.top - g.rail * 0.35f), Size(tw + g.rail * 0.7f, th + g.rail * 0.7f))
    drawRect(Brush.radialGradient(listOf(Color(0xFF1A9E57), Color(0xFF0E7A40)), center = Offset(g.left + tw / 2, g.top + th / 2), radius = th * 0.7f), Offset(g.left, g.top), Size(tw, th))
    // Elmaslar ve cizgi
    for (k in 1..7) {
        drawCircle(Color(0xFFF2E3C6), g.rail * 0.08f, Offset(g.left - g.rail * 0.65f, g.top + th * k / 8f))
        drawCircle(Color(0xFFF2E3C6), g.rail * 0.08f, Offset(g.left + tw + g.rail * 0.65f, g.top + th * k / 8f))
    }
    drawLine(Color(0x33FFFFFF), g.toScreen(PoolTable.L * 0.25f, 0f), g.toScreen(PoolTable.L * 0.25f, PoolTable.W), 2f)
    drawCircle(Color(0x44FFFFFF), 3f, g.toScreen(PoolTable.L * 0.72f, PoolTable.W / 2))
    // Cepler
    PoolTable.POCKETS.forEachIndexed { i, (px, py) ->
        val r = (if (i == 1 || i == 4) PoolTable.SIDE_R else PoolTable.CORNER_R) * g.scale
        drawCircle(Color(0xFF0A0A0A), r, g.toScreen(px, py))
    }
    val r = PoolTable.R * g.scale

    // Nisan cizgisi + hayalet top
    val cue = t.cue
    if (state.humanTurn || state.botThinking) {
        val dx = cos(vm.aim)
        val dy = sin(vm.aim)
        var hitT = 4f
        var hit: Ball? = null
        for (b in t.balls) {
            if (b.pocketed || b.id == 0) continue
            // Isin - daire (yaricap 2R) kesisimi
            val ox = cue.x - b.x
            val oy = cue.y - b.y
            val bq = ox * dx + oy * dy
            val c = ox * ox + oy * oy - 4 * PoolTable.R * PoolTable.R
            val disc = bq * bq - c
            if (disc < 0f) continue
            val tt = -bq - sqrt(disc)
            if (tt in 0f..hitT) {
                hitT = tt
                hit = b
            }
        }
        // Bant siniri
        val wallT = listOf(
            if (dx > 0) (PoolTable.L - PoolTable.R - cue.x) / dx else if (dx < 0) (PoolTable.R - cue.x) / dx else 9f,
            if (dy > 0) (PoolTable.W - PoolTable.R - cue.y) / dy else if (dy < 0) (PoolTable.R - cue.y) / dy else 9f,
        ).min()
        val endT = min(hitT, wallT)
        val gx = cue.x + dx * endT
        val gy = cue.y + dy * endT
        drawLine(Color(0xCCFFFFFF), g.toScreen(cue.x, cue.y), g.toScreen(gx, gy), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        drawCircle(Color(0x99FFFFFF), r, g.toScreen(gx, gy), style = Stroke(2f))
        if (hit != null && hitT <= wallT) {
            val nx = (hit.x - gx) / (2 * PoolTable.R)
            val ny = (hit.y - gy) / (2 * PoolTable.R)
            drawLine(Color(0x99FFFFFF), g.toScreen(hit.x, hit.y), g.toScreen(hit.x + nx * 0.18f, hit.y + ny * 0.18f), 2f)
        }
    }

    // Toplar
    for (b in t.balls) {
        if (b.pocketed) continue
        drawBall(b.id, g.toScreen(b.x, b.y), r, paint, highlight = state.ballInHand && b.id == 0 && state.humanTurn)
    }

    // Isteka (guc cekilirken geri gider)
    if (state.humanTurn || state.botThinking) {
        val back = r * 1.6f + vm.power * r * 6f
        val dirScreen = Offset(sin(vm.aim), cos(vm.aim)) // masa (dx,dy) -> ekran (dy, dx)
        val c = g.toScreen(cue.x, cue.y)
        val start = c - dirScreen * back
        val end = start - dirScreen * (g.scale * 0.9f)
        drawLine(Color(0xFFD9A066), start, end, r * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFF3B2414), start - dirScreen * (g.scale * 0.55f), end, r * 0.65f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFF60A5FA), start, start - dirScreen * (r * 0.4f), r * 0.55f)
    }
}

private fun DrawScope.drawBall(id: Int, c: Offset, r: Float, paint: android.graphics.Paint, highlight: Boolean) {
    drawCircle(Color(0x55000000), r, c + Offset(r * 0.15f, r * 0.2f))
    val color = Color(BALL_COLORS[id])
    if (id >= 9) {
        drawCircle(Color(0xFFFFFFF5), r, c)
        val band = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(band) { drawRect(color, Offset(c.x - r, c.y - r * 0.55f), Size(r * 2, r * 1.1f)) }
    } else {
        drawCircle(color, r, c)
    }
    if (id != 0) {
        drawCircle(Color(0xFFFFFFF5), r * 0.45f, c)
        paint.textSize = r * 0.62f
        paint.color = android.graphics.Color.BLACK
        drawIntoCanvas { it.nativeCanvas.drawText(id.toString(), c.x, c.y + r * 0.22f, paint) }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0x99FFFFFF), Color.Transparent), center = c - Offset(r * 0.35f, r * 0.35f), radius = r * 0.7f), r, c)
    if (highlight) drawCircle(Color(0xFFFFD166), r * 1.35f, c, style = Stroke(3f))
}

@Composable
private fun PowerBar(state: PoolUiState, viewModel: PoolViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = viewModel.frame
    Column(Modifier.width(54.dp).fillMaxHeight().padding(start = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("◀", fontSize = 18.sp, color = AppColors.TextPrimary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.SurfaceHigh).clickable { viewModel.nudge(-0.004f) }.padding(horizontal = 10.dp, vertical = 4.dp))
        Text("▶", fontSize = 18.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.SurfaceHigh).clickable { viewModel.nudge(0.004f) }.padding(horizontal = 10.dp, vertical = 4.dp))
        Text(stringResource(R.string.pl_power), color = AppColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Canvas(
            Modifier
                .weight(1f)
                .width(40.dp)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceHigh)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { viewModel.release() },
                        onDragCancel = { viewModel.setPower(0f); viewModel.release() },
                    ) { change, _ ->
                        change.consume()
                        viewModel.setPower(change.position.y / size.height)
                    }
                },
        ) {
            val p = viewModel.power
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF22C55E), Color(0xFFFACC15), Color(0xFFEF4444))),
                Offset.Zero,
                Size(size.width, size.height * p),
            )
            for (k in 1..9) drawLine(Color(0x33000000), Offset(0f, size.height * k / 10f), Offset(size.width, size.height * k / 10f), 1f)
        }
        Text("↓", color = AppColors.TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
    }
}
