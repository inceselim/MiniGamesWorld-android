package com.eomer91.minigamesworld.ui.games.dressup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.common.VP
import com.eomer91.minigamesworld.ui.games.common.drawVector
import com.eomer91.minigamesworld.ui.games.common.drawVectorFit
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.random.Random

private val Pink = Color(0xFFFF5C9E)

@Composable
fun DressUpScreen(viewModel: DressUpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    if (state.phase == DressPhase.LOADING) return

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .padding(12.dp),
    ) {
        // Tema ve "Goster" butonu
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.SurfaceHigh)
                    .clickable(enabled = state.phase == DressPhase.DRESSING) { viewModel.changeTheme() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(stringResource(R.string.du_theme_label), color = AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(state.theme.emoji + " " + stringResource(state.theme.nameRes), color = AppColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = viewModel::showOff,
                    enabled = state.phase == DressPhase.DRESSING,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                    modifier = Modifier.height(48.dp),
                ) { Text(stringResource(R.string.du_show), fontWeight = FontWeight.Black, fontSize = 16.sp) }
                Text("★ ${state.profile.totalStars}", color = AppColors.Gold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        // Sahne
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawDressBackground(DressBackground.entries[state.outfit.background.coerceIn(0, DressBackground.entries.lastIndex)])
                val scale = size.height * 0.96f / 400f
                drawDoll(state.outfit, size.width / 2f - 100f * scale, size.height * 0.02f, scale)
            }
            if (state.phase == DressPhase.JUDGING || state.phase == DressPhase.RESULT) {
                JudgePanel(state, onNext = { showInterstitial { viewModel.nextTheme() } }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        if (state.phase == DressPhase.DRESSING) {
            // Kategoriler
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DressCategory.entries.forEach { c ->
                    val sel = c == state.tab
                    Text(
                        c.emoji + " " + stringResource(c.labelRes),
                        color = if (sel) Color.White else AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Pink else AppColors.SurfaceHigh)
                            .clickable { viewModel.selectTab(c) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            TabContent(state, viewModel)
        }
    }
}

// ============================================================ Sekme icerigi

@Composable
private fun TabContent(state: DressUiState, viewModel: DressUpViewModel) {
    val o = state.outfit
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        when (state.tab) {
            DressCategory.FACE -> {
                ColorRow(DressPalettes.skin, o.skin, viewModel::pickSkin)
                ColorRow(DressPalettes.eyes, o.eyes, viewModel::pickEyes)
                ColorRow(DressPalettes.lips, o.lips, viewModel::pickLips)
            }
            DressCategory.HAIR -> {
                ColorRow(DressPalettes.hair, o.hairColor, viewModel::pickColor)
                ItemRow {
                    DressPalettes.hairStyles.forEach { h ->
                        Thumb(selected = o.hair == h, onClick = { viewModel.pickHair(h) }) { r ->
                            drawVectorFit(hairPreview(h), r, 0.08f) { dollSlot(o, it, null) }
                        }
                    }
                }
            }
            DressCategory.BACKGROUND -> ItemRow {
                DressBackground.entries.forEachIndexed { i, bg ->
                    Thumb(selected = o.background == i, onClick = { viewModel.pickBackground(i) }) { r ->
                        clipRect(r) { drawDressBackground(bg, r) }
                    }
                }
            }
            else -> {
                val cat = state.tab
                val sel = DressUpViewModel.selectedId(o, cat)
                val selItem = DressUpViewModel.item(cat, sel)
                if (selItem != null && selItem.parts.isNotEmpty()) {
                    ColorRow(DressPalettes.clothes, o.colorOf(DressUpViewModel.catKey(cat), selItem), viewModel::pickColor)
                } else {
                    Spacer(Modifier.height(34.dp))
                }
                ItemRow {
                    DressUpViewModel.itemsOf(cat).forEach { item ->
                        Thumb(selected = sel == item.id, onClick = { viewModel.pickItem(cat, item.id) }) { r ->
                            if (item.parts.isEmpty()) {
                                drawLine(AppColors.TextSecondary, r.topLeft + Offset(r.width * 0.3f, r.height * 0.3f), r.bottomRight - Offset(r.width * 0.3f, r.height * 0.3f), 4f)
                            } else {
                                val color = Color(o.colorOf(DressUpViewModel.catKey(cat), item))
                                drawVectorFit(item.parts, r, 0.1f) { dollSlot(o, it, color) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorRow(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) Pink else AppColors.TextSecondary.copy(alpha = 0.5f), CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
private fun ItemRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Thumb(selected: Boolean, onClick: () -> Unit, draw: DrawScope.(Rect) -> Unit) {
    Canvas(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF1F7))
            .border(if (selected) 3.dp else 1.dp, if (selected) Pink else Color(0x33000000), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) { draw(Rect(Offset.Zero, size)) }
}

@Composable
private fun JudgePanel(state: DressUiState, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.du_judges), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val faces = listOf("👩", "🧑", "👱‍♀️")
            state.judges.forEachIndexed { i, score ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(faces[i], fontSize = 30.sp)
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (i < state.revealed) Color.White else Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (i < state.revealed) "$score" else "?", color = if (i < state.revealed) Pink else Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                }
            }
        }
        if (state.phase == DressPhase.RESULT) {
            Text(
                "★".repeat(state.stars) + "☆".repeat(3 - state.stars),
                color = AppColors.Gold,
                fontSize = 30.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                stringResource(
                    when (state.stars) {
                        3 -> R.string.du_result_3
                        2 -> R.string.du_result_2
                        1 -> R.string.du_result_1
                        else -> R.string.du_result_0
                    },
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
            )
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                modifier = Modifier.padding(top = 6.dp),
            ) { Text(stringResource(R.string.du_next_theme), fontWeight = FontWeight.Black) }
        }
    }
}

// ============================================================ Cizim

private fun dollSlot(o: Outfit, name: String, main: Color?): Color? = when (name) {
    "main" -> main
    "skin" -> Color(o.skin)
    "hair" -> Color(o.hairColor)
    "eye" -> Color(o.eyes)
    "lips" -> Color(o.lips)
    else -> null
}

private fun hairPreview(style: String): List<VP> =
    (DressUpArt.hairBack[style] ?: emptyList()) + DressUpArt.body.takeLast(2) + DressUpArt.face + (DressUpArt.hairFront[style] ?: emptyList())

/** Bebegi katman sirasiyla ciz. */
fun DrawScope.drawDoll(o: Outfit, left: Float, top: Float, scale: Float) {
    fun layer(parts: List<VP>?, main: Color? = null) {
        if (parts != null) drawVector(parts, left, top, scale) { dollSlot(o, it, main) }
    }
    fun item(cat: DressCategory, id: String?) {
        val x = DressUpViewModel.item(cat, id) ?: return
        layer(x.parts, Color(o.colorOf(DressUpViewModel.catKey(cat), x)))
    }
    layer(DressUpArt.hairBack[o.hair])
    layer(DressUpArt.body)
    layer(DressUpArt.base)
    if (o.dress == null) {
        item(DressCategory.BOTTOM, o.bottom)
        item(DressCategory.TOP, o.top)
    } else {
        item(DressCategory.DRESS, o.dress)
    }
    item(DressCategory.SHOES, o.shoes)
    layer(DressUpArt.hands)
    layer(DressUpArt.face)
    if (o.extra != "bag") item(DressCategory.EXTRA, o.extra)
    layer(DressUpArt.hairFront[o.hair])
    item(DressCategory.HEAD, o.head)
    if (o.extra == "bag") item(DressCategory.EXTRA, o.extra)
}

private fun DrawScope.clipRect(r: Rect, block: DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(r)
    block()
    drawContext.canvas.restore()
}

/** Arka planlar; [area] verilmezse tum alan. */
fun DrawScope.drawDressBackground(bg: DressBackground, area: Rect = Rect(Offset.Zero, size)) {
    val w = area.width
    val h = area.height
    val x0 = area.left
    val y0 = area.top
    fun at(fx: Float, fy: Float) = Offset(x0 + fx * w, y0 + fy * h)
    fun sz(fw: Float, fh: Float) = Size(fw * w, fh * h)
    val rnd = Random(7)
    when (bg) {
        DressBackground.ROOM -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFD6E8), Color(0xFFFFC2DC)), y0, y0 + h), area.topLeft, area.size)
            // Duvar kagidi seritleri
            for (i in 0 until 10) drawRect(Color(0x18FFFFFF), at(i / 10f, 0f), sz(0.05f, 0.78f))
            drawRect(Color(0xFFBFE6FF), at(0.06f, 0.12f), sz(0.24f, 0.26f))
            drawRect(Color(0xFFFFFFFF), at(0.17f, 0.12f), sz(0.02f, 0.26f))
            drawRect(Color(0xFFD9A066), at(0f, 0.78f), sz(1f, 0.22f))
            drawOval(Color(0xFFFF9EC7), at(0.2f, 0.86f), sz(0.6f, 0.1f))
            drawRect(Color(0xFFFFFFFF), at(0.74f, 0.5f), sz(0.2f, 0.28f))
            drawCircle(Color(0xFF9BE15D), 0.06f * w, at(0.84f, 0.45f))
        }
        DressBackground.BEACH -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF6EC6FF), Color(0xFFC8ECFF)), y0, y0 + h * 0.6f), area.topLeft, sz(1f, 0.6f))
            drawCircle(Color(0xFFFFE066), 0.1f * w, at(0.8f, 0.14f))
            drawRect(Color(0xFF2E9BD6), at(0f, 0.55f), sz(1f, 0.15f))
            drawRect(Color(0x55FFFFFF), at(0f, 0.6f), sz(1f, 0.01f))
            drawRect(Color(0xFFF3D9A4), at(0f, 0.7f), sz(1f, 0.3f))
            // Palmiye
            drawRect(Color(0xFF8B5A2B), at(0.1f, 0.3f), sz(0.03f, 0.42f))
            for (a in 0 until 5) drawOval(Color(0xFF2E8B57), at(0.02f + a * 0.03f, 0.25f + (a % 2) * 0.03f), sz(0.14f, 0.06f))
            drawOval(Color(0xFFE63946), at(0.68f, 0.78f), sz(0.2f, 0.06f))
        }
        DressBackground.PARTY -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF240046), Color(0xFF5A189A)), y0, y0 + h), area.topLeft, area.size)
            listOf(0.15f to Color(0x40FF5C9E), 0.5f to Color(0x404CC9F0), 0.85f to Color(0x40FFD166)).forEach { (fx, c) ->
                val p = Path().apply {
                    moveTo(x0 + fx * w, y0)
                    lineTo(x0 + (fx - 0.2f) * w, y0 + h * 0.9f)
                    lineTo(x0 + (fx + 0.2f) * w, y0 + h * 0.9f)
                    close()
                }
                drawPath(p, c)
            }
            drawCircle(Color(0xFFDDDDEE), 0.07f * w, at(0.5f, 0.07f))
            repeat(60) {
                val c = listOf(Color(0xFFFF5C9E), Color(0xFFFFD166), Color(0xFF4CC9F0), Color(0xFF80ED99))[it % 4]
                drawCircle(c, 0.008f * w + rnd.nextFloat() * 0.006f * w, at(rnd.nextFloat(), rnd.nextFloat() * 0.85f))
            }
            drawRect(Color(0xFF3C096C), at(0f, 0.88f), sz(1f, 0.12f))
        }
        DressBackground.SNOW -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFA9CCF5), Color(0xFFE6F2FF)), y0, y0 + h), area.topLeft, area.size)
            listOf(0.08f, 0.82f, 0.92f).forEachIndexed { i, fx ->
                val p = Path().apply {
                    moveTo(x0 + fx * w, y0 + h * (0.35f + i * 0.05f))
                    lineTo(x0 + (fx - 0.1f) * w, y0 + h * 0.8f)
                    lineTo(x0 + (fx + 0.1f) * w, y0 + h * 0.8f)
                    close()
                }
                drawPath(p, Color(0xFF2D6A4F))
            }
            drawOval(Color.White, at(-0.2f, 0.74f), sz(1.4f, 0.4f))
            repeat(70) { drawCircle(Color.White, 0.006f * w + rnd.nextFloat() * 0.006f * w, at(rnd.nextFloat(), rnd.nextFloat() * 0.8f)) }
        }
        DressBackground.GARDEN -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF8ED6FF), Color(0xFFD9F4FF)), y0, y0 + h * 0.7f), area.topLeft, sz(1f, 0.7f))
            drawOval(Color.White, at(0.1f, 0.1f), sz(0.25f, 0.07f))
            drawOval(Color.White, at(0.6f, 0.16f), sz(0.3f, 0.08f))
            drawOval(Color(0xFF74C69D), at(-0.2f, 0.55f), sz(0.8f, 0.3f))
            drawOval(Color(0xFF52B788), at(0.4f, 0.58f), sz(0.9f, 0.3f))
            drawRect(Color(0xFF95D5B2), at(0f, 0.72f), sz(1f, 0.28f))
            repeat(24) {
                val c = listOf(Color(0xFFFF5C9E), Color(0xFFFFD166), Color(0xFFFFFFFF), Color(0xFFC77DFF))[it % 4]
                val p = at(rnd.nextFloat(), 0.74f + rnd.nextFloat() * 0.24f)
                drawCircle(c, 0.014f * w, p)
                drawCircle(Color(0xFFFFE066), 0.005f * w, p)
            }
        }
        DressBackground.CASTLE -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1F5E), Color(0xFFB57EDC)), y0, y0 + h), area.topLeft, area.size)
            repeat(30) { drawCircle(Color(0xCCFFFFFF), 0.004f * w, at(rnd.nextFloat(), rnd.nextFloat() * 0.4f)) }
            val wall = Color(0xFF3C2A5A)
            drawRect(wall, at(0f, 0.45f), sz(1f, 0.4f))
            for (fx in listOf(0.02f, 0.78f)) {
                drawRect(wall, at(fx, 0.25f), sz(0.2f, 0.6f))
                val p = Path().apply {
                    moveTo(x0 + (fx - 0.02f) * w, y0 + h * 0.25f)
                    lineTo(x0 + (fx + 0.1f) * w, y0 + h * 0.1f)
                    lineTo(x0 + (fx + 0.22f) * w, y0 + h * 0.25f)
                    close()
                }
                drawPath(p, Color(0xFF7B2CBF))
                drawRect(Color(0xFFFFD166), at(fx + 0.08f, 0.35f), sz(0.04f, 0.06f))
            }
            drawRect(Color(0xFF9D4EDD), at(0f, 0.85f), sz(1f, 0.15f))
            drawRect(Color(0xFFE5383B), at(0.35f, 0.85f), sz(0.3f, 0.15f))
        }
    }
}
