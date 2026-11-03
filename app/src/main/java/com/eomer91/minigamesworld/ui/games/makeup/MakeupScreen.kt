package com.eomer91.minigamesworld.ui.games.makeup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.VectorArt
import com.eomer91.minigamesworld.ui.games.common.drawVector
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.min
import kotlin.math.sqrt

private val Pink = Color(0xFFFF5C9E)
private const val FACE_W = 300f
private const val FACE_H = 360f

private data class Tool(val tool: MakeupTool, val emoji: String, val label: Int)

private val TOOLS = listOf(
    Tool(MakeupTool.SHADOW, "👁", R.string.mk_tool_shadow),
    Tool(MakeupTool.BLUSH, "😊", R.string.mk_tool_blush),
    Tool(MakeupTool.LIPS, "💄", R.string.mk_tool_lips),
    Tool(MakeupTool.LASHES, "〰", R.string.mk_tool_lashes),
    Tool(MakeupTool.STICKER, "✨", R.string.mk_tool_sticker),
    Tool(MakeupTool.WIPE, "🧽", R.string.mk_tool_wipe),
)

@Composable
fun MakeupScreen(viewModel: MakeupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .padding(12.dp),
    ) {
        RequestCard(state, onDone = viewModel::finish)

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE3F1), Color(0xFFFFC8E1)))),
        ) {
            FaceCanvas(state, viewModel)
            if (state.phase == MakeupPhase.RESULT) {
                ResultPanel(state, onNext = { showInterstitial { viewModel.nextCustomer() } }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        if (state.phase == MakeupPhase.WORKING) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TOOLS.forEach { t ->
                    val sel = t.tool == state.tool
                    Text(
                        t.emoji + " " + stringResource(t.label),
                        color = if (sel) Color.White else AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Pink else AppColors.SurfaceHigh)
                            .clickable { viewModel.selectTool(t.tool) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (state.tool) {
                    MakeupTool.SHADOW -> MakeupPalettes.shadow.forEach { c -> Swatch(c, c == state.shadowColor) { viewModel.pickColor(c) } }
                    MakeupTool.BLUSH -> MakeupPalettes.blush.forEach { c -> Swatch(c, c == state.blushColor) { viewModel.pickColor(c) } }
                    MakeupTool.LIPS -> MakeupPalettes.lips.forEach { c -> Swatch(c, c == state.lipsColor) { viewModel.pickColor(c) } }
                    MakeupTool.LASHES -> listOf(R.string.mk_lashes_0, R.string.mk_lashes_1, R.string.mk_lashes_2).forEachIndexed { i, label ->
                        Chip(stringResource(label), state.face.lashes == i) { viewModel.pickLashes(i) }
                    }
                    MakeupTool.STICKER -> MakeupPalettes.stickers.forEachIndexed { i, e ->
                        Chip(e, state.sticker == i) { viewModel.pickSticker(i) }
                    }
                    MakeupTool.WIPE -> Text(stringResource(R.string.mk_wipe_hint), color = AppColors.TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RequestCard(state: MakeupUiState, onDone: () -> Unit) {
    val r = state.request
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SurfaceHigh)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(stringResource(R.string.mk_request), color = AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                RequestItem("👁", r.shadow)
                RequestItem("😊", r.blush)
                RequestItem("💄", r.lips)
                Text(
                    "〰 " + stringResource(listOf(R.string.mk_lashes_0, R.string.mk_lashes_1, R.string.mk_lashes_2)[r.lashes]),
                    color = AppColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onDone,
                enabled = state.phase == MakeupPhase.WORKING,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                modifier = Modifier.height(48.dp),
            ) { Text(stringResource(R.string.mk_done), fontWeight = FontWeight.Black, fontSize = 16.sp) }
            Text("★ ${state.totalStars}", color = AppColors.Gold, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RequestItem(emoji: String, color: Long?) {
    Text(emoji, fontSize = 15.sp)
    Box(
        Modifier
            .padding(start = 2.dp, end = 8.dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(if (color != null) Color(color) else Color.Transparent)
            .border(1.dp, AppColors.TextSecondary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (color == null) Text("✕", color = AppColors.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun Swatch(c: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(c))
            .border(if (selected) 3.dp else 1.dp, if (selected) Pink else Color(0x33000000), CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else AppColors.TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Pink else AppColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ============================================================ Yuz

/** Ekran -> yuz koordinati donusumu. */
private class FaceFit(val scale: Float, val left: Float, val top: Float) {
    fun toFace(p: Offset) = Offset((p.x - left) / scale, (p.y - top) / scale)
}

private fun fit(w: Float, h: Float): FaceFit {
    val s = min(w / FACE_W, h / FACE_H)
    return FaceFit(s, (w - FACE_W * s) / 2f, (h - FACE_H * s))
}

@Composable
private fun FaceCanvas(state: MakeupUiState, viewModel: MakeupViewModel) {
    val emojiPaint = remember { android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.CENTER } }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val f = fit(size.width.toFloat(), size.height.toFloat())
                    val q = f.toFace(p)
                    viewModel.tap(q.x, q.y)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val f = fit(size.width.toFloat(), size.height.toFloat())
                    val q = f.toFace(change.position)
                    val d = sqrt(drag.x * drag.x + drag.y * drag.y) / f.scale
                    viewModel.rub(q.x, q.y, d)
                }
            },
    ) {
        val f = fit(size.width, size.height)
        drawFace(state.customer, state.face, f.left, f.top, f.scale)
        emojiPaint.textSize = 26f * f.scale
        drawIntoCanvas { c ->
            state.face.stickers.forEach { st ->
                c.nativeCanvas.drawText(MakeupPalettes.stickers[st.kind], f.left + st.x * f.scale, f.top + (st.y + 9f) * f.scale, emojiPaint)
            }
        }
    }
}

fun DrawScope.drawFace(cu: Customer, face: FaceMakeup, left: Float, top: Float, scale: Float) {
    val slot: (String) -> Color? = { name ->
        when (name) {
            "skin" -> Color(cu.skin)
            "hair" -> Color(cu.hairColor)
            "eye" -> Color(cu.eyes)
            "main" -> Color(cu.top)
            "lips" -> Color(cu.lips)
            else -> null
        }
    }
    fun layer(parts: List<com.eomer91.minigamesworld.ui.games.common.VP>?) {
        if (parts != null) drawVector(parts, left, top, scale, slot = slot)
    }
    layer(MakeupArt.hairBack[cu.hair])
    layer(MakeupArt.body)
    withTransform({
        translate(left, top)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        // Far: yumusak kenarli elips
        for (r in listOf(FaceRegion.SHADOW_L, FaceRegion.SHADOW_R)) {
            val p = face.paint(r)
            if (p.coverage > 0f) {
                val c = Color(p.color)
                drawOval(
                    Brush.radialGradient(
                        listOf(c.copy(alpha = 0.85f * p.coverage), c.copy(alpha = 0.6f * p.coverage), Color.Transparent),
                        center = Offset(r.cx, r.cy),
                        radius = r.rx,
                    ),
                    Offset(r.cx - r.rx, r.cy - r.ry),
                    Size(r.rx * 2, r.ry * 2),
                )
            }
        }
    }
    layer(MakeupArt.eyes)
    layer(MakeupArt.lashes.getOrNull(face.lashes))
    layer(MakeupArt.brows)
    withTransform({
        translate(left, top)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        for (r in listOf(FaceRegion.BLUSH_L, FaceRegion.BLUSH_R)) {
            val p = face.paint(r)
            if (p.coverage > 0f) {
                val c = Color(p.color)
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.6f * p.coverage), Color.Transparent), center = Offset(r.cx, r.cy), radius = r.rx * 0.9f),
                    r.rx * 0.9f,
                    Offset(r.cx, r.cy),
                )
            }
        }
    }
    layer(MakeupArt.nose)
    layer(MakeupArt.lipsBase)
    val lp = face.paint(FaceRegion.LIPS)
    if (lp.coverage > 0f) {
        withTransform({
            translate(left, top)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(VectorArt.path(MakeupArt.LIPS_SHAPE), Color(lp.color), alpha = 0.92f * lp.coverage)
        }
    }
    layer(MakeupArt.lipsTop)
    layer(MakeupArt.hairFront[cu.hair])
}

@Composable
private fun ResultPanel(state: MakeupUiState, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("★".repeat(state.stars) + "☆".repeat(3 - state.stars), color = AppColors.Gold, fontSize = 32.sp)
        Text(
            stringResource(
                when (state.stars) {
                    3 -> R.string.mk_result_3
                    2 -> R.string.mk_result_2
                    1 -> R.string.mk_result_1
                    else -> R.string.mk_result_0
                },
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
        if (state.goldEarned > 0) {
            Text(stringResource(R.string.game_gold_earned, state.goldEarned), color = AppColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
            modifier = Modifier.padding(top = 6.dp),
        ) { Text(stringResource(R.string.mk_next), fontWeight = FontWeight.Black) }
    }
}
