package com.eomer91.minigamesworld.ui.games.jigsaw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Yapboz resimleri: oyun kapaklari (16:9). */
private val PICTURES = listOf(
    R.drawable.game_fox_adventure, R.drawable.game_forest_guardian, R.drawable.game_dress_up, R.drawable.game_cake_shop,
    R.drawable.game_makeup, R.drawable.game_chess, R.drawable.game_fish_world, R.drawable.game_rocket_hop,
    R.drawable.game_bubble_shooter, R.drawable.game_match_3, R.drawable.game_tower_defense, R.drawable.game_countries_war,
    R.drawable.game_memory_card, R.drawable.game_block_puzzle,
)

@Composable
fun JigsawScreen(viewModel: JigsawViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        when (state.phase) {
            JigsawPhase.PICK -> Picker(state, viewModel, onStart = { pic -> showInterstitial { viewModel.start(pic) } })
            else -> Play(state, viewModel, onAgain = { showInterstitial { viewModel.start(state.picture) } })
        }
    }
}

@Composable
private fun Picker(state: JigsawUiState, viewModel: JigsawViewModel, onStart: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text(stringResource(R.string.jg_choose), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JigsawLevel.entries.forEach { l ->
                val sel = l == state.level
                Text(
                    "${l.cols * l.rows} " + stringResource(R.string.jg_pieces),
                    color = if (sel) AppColors.OnPrimary else AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AppColors.Primary else AppColors.SurfaceHigh)
                        .clickable { viewModel.selectLevel(l) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        PICTURES.indices.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { i ->
                    val best = state.profile.best[JigsawProfile.key(i, state.level)]
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, if (best != null) Color(0xFF4ADE80) else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { onStart(i) },
                    ) {
                        Image(painterResource(PICTURES[i]), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (best != null) {
                            Text(
                                "✓ " + formatTime(best),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun formatTime(s: Int) = "%d:%02d".format(s / 60, s % 60)

/** Tahtanin ekrandaki yerlesimi (piksel). */
private class Geo(w: Float, h: Float, img: ImageBitmap, cols: Int, rows: Int, topPad: Float) {
    val boardW = w * 0.94f
    val boardH = boardW * img.height / img.width
    val left = (w - boardW) / 2f
    val top = topPad
    val unitW = boardW / cols
    val unitH = boardH / rows
    /** Tepsinin alt siniri, hucre biriminde */
    val trayBottom = (h - top) / unitH - 0.05f
    fun toUnit(p: Offset) = Offset((p.x - left) / unitW, (p.y - top) / unitH)
}

@Composable
private fun Play(state: JigsawUiState, viewModel: JigsawViewModel, onAgain: () -> Unit) {
    val img = ImageBitmap.imageResource(PICTURES[state.picture])
    val topPad = with(LocalDensity.current) { 6.dp.toPx() }
    val pathCache = remember(state.picture, state.level, viewModel.engine) { HashMap<Int, Path>() }
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← " + stringResource(R.string.jg_pictures), color = AppColors.Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.backToPick() }.padding(6.dp))
            Spacer(Modifier.weight(1f))
            Text("⏱ " + formatTime(state.seconds), color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Text("🧩 ${state.placed}/${state.total}", color = AppColors.TextPrimary, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Text(
                "👁",
                fontSize = 20.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (state.preview) AppColors.Primary else AppColors.SurfaceHigh)
                    .clickable { viewModel.togglePreview() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
            val engine = viewModel.engine
            if (engine != null) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { sz ->
                            viewModel.layout(sz.width.toFloat(), sz.height.toFloat(), topPad, img.height.toFloat() / img.width)
                        }
                        .pointerInput(engine) {
                            val g = Geo(size.width.toFloat(), size.height.toFloat(), img, engine.cols, engine.rows, topPad)
                            detectDragGestures(
                                onDragStart = { o -> val u = g.toUnit(o); viewModel.dragStart(u.x, u.y) },
                                onDragEnd = viewModel::dragEnd,
                                onDragCancel = viewModel::dragEnd,
                            ) { change, d ->
                                change.consume()
                                viewModel.dragBy(d.x / g.unitW, d.y / g.unitH, -0.3f, g.trayBottom - 1f)
                            }
                        },
                ) {
                    @Suppress("UNUSED_VARIABLE")
                    val f = viewModel.frame
                    val g = Geo(size.width, size.height, img, engine.cols, engine.rows, topPad)
                    drawBoard(engine, img, g, state.preview, pathCache)
                }
            }
            if (state.phase == JigsawPhase.DONE) {
                Column(Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
                    GameOverPanel(
                        score = state.score,
                        isNewRecord = state.newBest,
                        onRestart = onAgain,
                        title = stringResource(R.string.jg_done),
                        titleColor = Color(0xFF22C55E),
                        detail = "⏱ " + formatTime(state.seconds),
                        goldEarned = state.goldEarned,
                    )
                    Button(
                        onClick = viewModel::backToPick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHigh, contentColor = AppColors.TextPrimary),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                    ) { Text(stringResource(R.string.jg_new_puzzle), fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

private fun DrawScope.drawBoard(e: JigsawEngine, img: ImageBitmap, g: Geo, preview: Boolean, cache: HashMap<Int, Path>) {
    // Tahta zemini ve silik resim
    drawRect(Color(0xFF0F172A), Offset(g.left, g.top), androidx.compose.ui.geometry.Size(g.boardW, g.boardH))
    drawImage(
        img, IntOffset.Zero, IntSize(img.width, img.height),
        IntOffset(g.left.roundToInt(), g.top.roundToInt()), IntSize(g.boardW.roundToInt(), g.boardH.roundToInt()),
        alpha = if (preview) 0.55f else 0.12f,
    )
    // Izgara
    for (c in 1 until e.cols) drawLine(Color(0x22FFFFFF), Offset(g.left + c * g.unitW, g.top), Offset(g.left + c * g.unitW, g.top + g.boardH), 1f)
    for (r in 1 until e.rows) drawLine(Color(0x22FFFFFF), Offset(g.left, g.top + r * g.unitH), Offset(g.left + g.boardW, g.top + r * g.unitH), 1f)

    val stroke = Stroke(1.5f, join = StrokeJoin.Round)
    for (p in e.pieces) {
        val path = cache.getOrPut(p.r * 100 + p.c + g.unitW.roundToInt() * 100_000) { piecePath(g.unitW, g.unitH, e.edges(p.r, p.c)) }
        val x = g.left + p.x * g.unitW
        val y = g.top + p.y * g.unitH
        if (!p.placed) translate(x + 3f, y + 4f) { drawPath(path, Color.Black.copy(alpha = 0.35f)) }
        translate(x, y) {
            clipPath(path) {
                drawImage(
                    img, IntOffset.Zero, IntSize(img.width, img.height),
                    IntOffset((-p.c * g.unitW).roundToInt(), (-p.r * g.unitH).roundToInt()), IntSize(g.boardW.roundToInt(), g.boardH.roundToInt()),
                )
            }
            drawPath(path, if (p.placed) Color(0x33FFFFFF) else Color(0xCCFFFFFF), style = stroke)
        }
    }
}

/** Kulakcikli parca yolu; kenarlar: ust, sag, alt, sol. */
private fun piecePath(w: Float, h: Float, edges: IntArray): Path {
    val s = minOf(w, h)
    val p = Path()
    p.moveTo(0f, 0f)
    edge(p, 0f, 0f, w, 0f, edges[0], s)
    edge(p, w, 0f, w, h, edges[1], s)
    edge(p, w, h, 0f, h, edges[2], s)
    edge(p, 0f, h, 0f, 0f, edges[3], s)
    p.close()
    return p
}

private val TAB = floatArrayOf(
    // (t, yukseklik) uclu cubic noktalari
    0.40f, 0f, 0.42f, 0.08f, 0.38f, 0.14f,
    0.32f, 0.24f, 0.40f, 0.30f, 0.50f, 0.30f,
    0.60f, 0.30f, 0.68f, 0.24f, 0.62f, 0.14f,
    0.58f, 0.08f, 0.60f, 0f, 0.66f, 0f,
)

private fun edge(p: Path, ax: Float, ay: Float, bx: Float, by: Float, sign: Int, s: Float) {
    if (sign == 0) {
        p.lineTo(bx, by)
        return
    }
    val len = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
    val dx = (bx - ax) / len
    val dy = (by - ay) / len
    // Saat yonunde dolasimda dis normal
    val nx = dy
    val ny = -dx
    fun px(t: Float, k: Float) = ax + dx * len * t + nx * k * s * sign
    fun py(t: Float, k: Float) = ay + dy * len * t + ny * k * s * sign
    p.lineTo(px(0.34f, 0f), py(0.34f, 0f))
    var i = 0
    while (i < TAB.size) {
        p.cubicTo(px(TAB[i], TAB[i + 1]), py(TAB[i], TAB[i + 1]), px(TAB[i + 2], TAB[i + 3]), py(TAB[i + 2], TAB[i + 3]), px(TAB[i + 4], TAB[i + 5]), py(TAB[i + 4], TAB[i + 5]))
        i += 6
    }
    p.lineTo(bx, by)
}
