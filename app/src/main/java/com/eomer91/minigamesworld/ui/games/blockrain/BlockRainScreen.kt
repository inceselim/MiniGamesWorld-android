package com.eomer91.minigamesworld.ui.games.blockrain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private val BoardBg = Color(0xFF0B1222)
private val GridLine = Color(0xFF1A2540)

@Composable
fun BlockRainScreen(viewModel: BlockRainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) viewModel.pause() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            viewModel.pause()
        }
    }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last != 0L) viewModel.onFrame((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.Surface)
                .padding(12.dp),
        ) {
            ScoreHeader(score = state.score, best = state.best, onRestart = restartWithAd)

            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.fillMaxHeight().aspectRatio(0.5f, matchHeightConstraintsFirst = true)) {
                    Board(viewModel)
                    when (state.phase) {
                        BlockRainPhase.READY -> BoardMessage(stringResource(R.string.br_tap_start), stringResource(R.string.br_how), onClick = viewModel::start)
                        BlockRainPhase.PAUSED -> BoardMessage(
                            stringResource(R.string.br_paused),
                            stringResource(R.string.br_tap_resume),
                            onClick = viewModel::start,
                        )
                        BlockRainPhase.OVER -> Column(
                            Modifier.align(Alignment.Center).padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            GameOverPanel(
                                score = state.score,
                                isNewRecord = state.isNewRecord,
                                goldEarned = state.goldEarned,
                                onRestart = restartWithAd,
                            )
                            if (!state.rescueUsed) {
                                Button(
                                    onClick = {
                                        showRewarded({ viewModel.rescue() }) {
                                            scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.br_rescue), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                        else -> Unit
                    }
                }
                Spacer(Modifier.width(10.dp))
                SidePanel(viewModel, state)
            }

            Controls(viewModel, Modifier.padding(top = 10.dp))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ============================================================ Tahta

@Composable
private fun Board(viewModel: BlockRainViewModel) {
    Canvas(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(BoardBg)
            .pointerInput(Unit) {
                // Dokun: dondur · Yana surukle: kaydir · Asagi surukle: yumusak dusus
                // Hizla asagi: birak · Hizla yukari: tut
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val cell = size.width / BlockRainEngine.COLS.toFloat()
                    var accX = 0f
                    var accY = 0f
                    var totalX = 0f
                    var totalY = 0f
                    var acted = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) {
                            val ms = ch.uptimeMillis - down.uptimeMillis
                            val fastVertical = ms < 260 && abs(totalY) > cell * 2.5f && abs(totalY) > abs(totalX) * 2f
                            when {
                                fastVertical && totalY > 0 -> viewModel.hardDrop()
                                fastVertical && totalY < 0 -> viewModel.hold()
                                !acted && abs(totalX) < cell * 0.6f && abs(totalY) < cell * 0.6f -> viewModel.rotate(1)
                            }
                            break
                        }
                        val d = ch.positionChange()
                        ch.consume()
                        accX += d.x
                        accY += d.y
                        totalX += d.x
                        totalY += d.y
                        while (accX >= cell) { viewModel.move(1); accX -= cell; acted = true }
                        while (accX <= -cell) { viewModel.move(-1); accX += cell; acted = true }
                        if (accY < 0f) accY = 0f
                        val ms = ch.uptimeMillis - down.uptimeMillis
                        if (ms > 120) {
                            while (accY >= cell) {
                                viewModel.softDrop(true)
                                viewModel.softDrop(false)
                                accY -= cell
                                acted = true
                            }
                        }
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val f = viewModel.frame
        val e = viewModel.engine
        val cell = size.width / BlockRainEngine.COLS
        // Izgara
        for (c in 1 until BlockRainEngine.COLS) drawLine(GridLine, Offset(c * cell, 0f), Offset(c * cell, size.height), 1f)
        for (r in 1 until BlockRainEngine.ROWS) drawLine(GridLine, Offset(0f, r * cell), Offset(size.width, r * cell), 1f)
        // Yerlesmis bloklar
        for (r in 0 until BlockRainEngine.ROWS) for (c in 0 until BlockRainEngine.COLS) {
            val v = e.board[r][c]
            if (v != 0) block(Color(Tetromino.entries[v - 1].color), c * cell, r * cell, cell)
        }
        if (!e.gameOver) {
            // Hayalet
            val color = Color(e.piece.color)
            val g = e.ghostRow()
            for ((r, c) in e.cells(top = g)) {
                if (r < 0) continue
                drawRoundRect(color.copy(alpha = 0.18f), Offset(c * cell + 2f, r * cell + 2f), Size(cell - 4f, cell - 4f), CornerRadius(cell * 0.15f))
                drawRoundRect(color.copy(alpha = 0.5f), Offset(c * cell + 2f, r * cell + 2f), Size(cell - 4f, cell - 4f), CornerRadius(cell * 0.15f), style = Stroke(2f))
            }
            // Aktif parca
            for ((r, c) in e.cells()) if (r >= 0) block(color, c * cell, r * cell, cell)
        }
        // Temizlenen satir parlamasi
        if (e.flashTimer > 0f) {
            val a = (e.flashTimer / 0.25f).coerceIn(0f, 1f) * 0.7f
            e.flashRows.forEach { r -> drawRect(Color.White.copy(alpha = a), Offset(0f, r * cell), Size(size.width, cell)) }
        }
    }
}

/** Golgeli kare blok. */
private fun DrawScope.block(color: Color, x: Float, y: Float, s: Float) {
    val p = s * 0.06f
    val r = CornerRadius(s * 0.15f)
    drawRoundRect(color, Offset(x + p, y + p), Size(s - 2 * p, s - 2 * p), r)
    // Ust-sol isik, alt-sag golge
    drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(x + p * 2, y + p * 2), Size(s - 4 * p, s * 0.22f), CornerRadius(s * 0.1f))
    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(x + p * 2, y + s * 0.7f), Size(s - 4 * p, s * 0.2f), CornerRadius(s * 0.1f))
}

@Composable
private fun BoardMessage(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(subtitle, color = AppColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}

// ============================================================ Yan panel

@Composable
private fun SidePanel(viewModel: BlockRainViewModel, state: BlockRainUiState) {
    @Suppress("UNUSED_VARIABLE")
    val f = viewModel.frame
    val e = viewModel.engine
    Column(Modifier.width(78.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Label(stringResource(R.string.br_hold))
        PiecePreview(e.hold, Modifier.clickable { viewModel.hold() })
        Label(stringResource(R.string.br_next), Modifier.padding(top = 10.dp))
        e.next.forEachIndexed { i, t -> PiecePreview(t, alpha = if (i == 0) 1f else 0.6f) }
        Spacer(Modifier.weight(1f))
        Stat(stringResource(R.string.br_level), state.level)
        Stat(stringResource(R.string.br_lines), state.lines)
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceHigh)
                .clickable(enabled = state.phase == BlockRainPhase.PLAYING) { viewModel.pause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.br_paused), tint = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, color = AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = modifier.padding(bottom = 2.dp))
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.SurfaceHigh)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = AppColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value.toString(), color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PiecePreview(t: Tetromino?, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Canvas(
        modifier
            .padding(vertical = 2.dp)
            .size(width = 70.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BoardBg),
    ) {
        if (t == null) return@Canvas
        val cells = t.cells[0]
        val minR = cells.minOf { it.first }
        val maxR = cells.maxOf { it.first }
        val minC = cells.minOf { it.second }
        val maxC = cells.maxOf { it.second }
        val s = minOf(size.width / 5f, size.height / 2.6f)
        val ox = (size.width - (maxC - minC + 1) * s) / 2f
        val oy = (size.height - (maxR - minR + 1) * s) / 2f
        val color = Color(t.color).copy(alpha = alpha)
        cells.forEach { (r, c) -> block(color, ox + (c - minC) * s, oy + (r - minR) * s, s) }
    }
}

// ============================================================ Kontroller

@Composable
private fun Controls(viewModel: BlockRainViewModel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RepeatButton("◀", Modifier.weight(1f)) { viewModel.move(-1) }
        RepeatButton("▶", Modifier.weight(1f)) { viewModel.move(1) }
        ControlButton("⟳", Modifier.weight(1f), Color(0xFF7C3AED)) { viewModel.rotate(1) }
        HoldButton("▼", Modifier.weight(1f), onDown = { viewModel.softDrop(true) }, onUp = { viewModel.softDrop(false) })
        ControlButton("⤓", Modifier.weight(1f), Color(0xFFF59E0B)) { viewModel.hardDrop() }
    }
}

@Composable
private fun ButtonFace(label: String, modifier: Modifier, color: Color) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

/** Basili tutulunca tekrarlar (ilk gecikme 170 ms, sonra 50 ms). */
@Composable
private fun RepeatButton(label: String, modifier: Modifier, onPress: () -> Unit) {
    ButtonFace(
        label,
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown().consume()
                onPress()
                var wait = 170L
                while (true) {
                    var released = false
                    withTimeoutOrNull(wait) {
                        waitForUpOrCancellation()
                        released = true
                    }
                    if (released) break
                    onPress()
                    wait = 50L
                }
            }
        },
        AppColors.SurfaceHigh,
    )
}

@Composable
private fun HoldButton(label: String, modifier: Modifier, onDown: () -> Unit, onUp: () -> Unit) {
    ButtonFace(
        label,
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown().consume()
                onDown()
                waitForUpOrCancellation()
                onUp()
            }
        },
        AppColors.SurfaceHigh,
    )
}

@Composable
private fun ControlButton(label: String, modifier: Modifier, color: Color, onPress: () -> Unit) {
    ButtonFace(
        label,
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown().consume()
                onPress()
                waitForUpOrCancellation()
            }
        },
        color,
    )
}
