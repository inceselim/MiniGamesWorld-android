package com.eomer91.minigamesworld.ui.games.cooking

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ui.games.makeup.FaceMakeup
import com.eomer91.minigamesworld.ui.games.makeup.drawFace
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

private val Pink = Color(0xFFFF5C9E)
private val Counter = Color(0xFFFFF4E6)

@Composable
fun CookingScreen(viewModel: CookingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.baking) {
        if (state.baking) {
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

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface)
            .padding(12.dp),
    ) {
        OrderCard(state)
        StepBar(state.step, Modifier.padding(top = 8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE8D6), Counter))),
        ) {
            when (state.step) {
                CakeStep.INGREDIENTS -> IngredientsStep(state, viewModel)
                CakeStep.MIX -> MixStep(state, viewModel)
                CakeStep.BAKE -> BakeStep(state, viewModel)
                CakeStep.FROST -> FrostStep(state, viewModel)
                CakeStep.DECORATE -> DecorateStep(state, viewModel)
                CakeStep.SERVE -> ServeStep(state, onNext = { showInterstitial { viewModel.nextCustomer() } })
            }
        }
    }
}

// ============================================================ Ust kisim

@Composable
private fun OrderCard(state: CookingUiState) {
    val o = state.order
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD6E8)),
        ) {
            val s = size.width / 170f
            drawFace(state.customer, FaceMakeup(), -65f * s, -20f * s, s)
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SurfaceHigh)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(stringResource(R.string.ck2_order), color = AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.flavor.emoji + " " + stringResource(flavorName(o.flavor)), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(16.dp).clip(CircleShape).background(Color(o.frosting.color)).border(1.dp, AppColors.TextSecondary, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(o.toppings.joinToString(" ") { it.emoji }, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("★ ${state.totalStars}", color = AppColors.Gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

private fun flavorName(f: Flavor) = when (f) {
    Flavor.VANILLA -> R.string.ck2_vanilla
    Flavor.CHOCOLATE -> R.string.ck2_chocolate
    Flavor.STRAWBERRY -> R.string.ck2_strawberry
}

@Composable
private fun StepBar(step: CakeStep, modifier: Modifier = Modifier) {
    val labels = listOf("🥚", "🥄", "🔥", "🍦", "🍓", "🎂")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        CakeStep.entries.forEachIndexed { i, s ->
            val done = s.ordinal < step.ordinal
            val now = s == step
            Box(
                Modifier
                    .size(if (now) 34.dp else 28.dp)
                    .clip(CircleShape)
                    .background(if (now) Pink else if (done) Color(0xFF4ADE80) else AppColors.SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) { Text(if (done) "✓" else labels[i], fontSize = if (now) 17.sp else 13.sp, color = Color.White, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color(0xFF7A4A2A),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(10.dp),
    )
}

// ============================================================ 1. Malzemeler

@Composable
private fun IngredientsStep(state: CookingUiState, viewModel: CookingViewModel) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Hint(stringResource(R.string.ck2_hint_ingredients, stringResource(flavorName(state.order.flavor))))
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            drawBowl(size.width / 2f, size.height * 0.62f, min(size.width * 0.32f, size.height * 0.42f), null, 0f)
            // Kasedeki malzemeler
            val icons = state.work.added.map { it.emoji }
            drawIntoCanvas { c ->
                val p = android.graphics.Paint().apply { textSize = size.height * 0.12f; textAlign = android.graphics.Paint.Align.CENTER }
                icons.forEachIndexed { i, e ->
                    val x = size.width / 2f + (i - (icons.size - 1) / 2f) * size.height * 0.11f
                    c.nativeCanvas.drawText(e, x, size.height * 0.5f, p)
                }
            }
        }
        if (state.lastMistake) {
            Text(stringResource(R.string.ck2_wrong), color = AppColors.Danger, fontWeight = FontWeight.Black)
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Ingredient.entries.forEach { ing ->
                val used = ing in state.work.added
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (used) Color(0xFFD9F99D) else Color.White)
                        .border(1.dp, Color(0x22000000), RoundedCornerShape(14.dp))
                        .clickable(enabled = !used) { viewModel.add(ing) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(ing.emoji, fontSize = 30.sp)
                    Text(stringResource(ingredientName(ing)), fontSize = 11.sp, color = Color(0xFF5A3A2A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun ingredientName(i: Ingredient) = when (i) {
    Ingredient.FLOUR -> R.string.ck2_flour
    Ingredient.SUGAR -> R.string.ck2_sugar
    Ingredient.EGGS -> R.string.ck2_eggs
    Ingredient.MILK -> R.string.ck2_milk
    Ingredient.VANILLA -> R.string.ck2_vanilla
    Ingredient.COCOA -> R.string.ck2_cocoa
    Ingredient.STRAWBERRY -> R.string.ck2_strawberry
}

// ============================================================ 2. Karistir

@Composable
private fun MixStep(state: CookingUiState, viewModel: CookingViewModel) {
    val spoon = remember { floatArrayOf(-1f, -1f) }
    Column(Modifier.fillMaxSize()) {
        Hint(stringResource(R.string.ck2_hint_mix))
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var lastAngle: Float? = null
                    detectDragGestures(
                        onDragStart = { lastAngle = null },
                        onDragEnd = { lastAngle = null },
                    ) { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height * 0.62f
                        val p = change.position
                        spoon[0] = p.x
                        spoon[1] = p.y
                        val a = atan2(p.y - cy, p.x - cx)
                        lastAngle?.let { la ->
                            var d = a - la
                            if (d > Math.PI) d -= (2 * Math.PI).toFloat()
                            if (d < -Math.PI) d += (2 * Math.PI).toFloat()
                            viewModel.stir(d)
                        }
                        lastAngle = a
                    }
                },
        ) {
            val r = min(size.width * 0.36f, size.height * 0.46f)
            val flavor = state.work.flavor ?: Flavor.VANILLA
            drawBowl(size.width / 2f, size.height * 0.62f, r, Color(flavor.sponge), state.work.mix)
            if (spoon[0] >= 0f) drawSpoon(Offset(spoon[0], spoon[1]), r)
        }
        LinearProgressIndicator(
            progress = { state.work.mix },
            color = Pink,
            trackColor = Color(0x33000000),
            modifier = Modifier.fillMaxWidth().padding(12.dp).height(10.dp).clip(RoundedCornerShape(5.dp)),
        )
    }
}

/** Kase; [batter] verilirse icinde hamur ([mix] 0 = topakli, 1 = puruzsuz). */
private fun DrawScope.drawBowl(cx: Float, cy: Float, r: Float, batter: Color?, mix: Float) {
    drawOval(Color(0x22000000), Offset(cx - r * 1.05f, cy + r * 0.62f), Size(r * 2.1f, r * 0.3f))
    val bowl = Path().apply {
        moveTo(cx - r, cy - r * 0.1f)
        cubicTo(cx - r, cy + r * 0.75f, cx + r, cy + r * 0.75f, cx + r, cy - r * 0.1f)
        close()
    }
    drawOval(Color(0xFFB9D7EA), Offset(cx - r, cy - r * 0.32f), Size(r * 2, r * 0.44f))
    if (batter != null) {
        val raw = Color(0xFFF3E3C3)
        val c = lerp(raw, batter, mix)
        drawOval(c, Offset(cx - r * 0.92f, cy - r * 0.26f), Size(r * 1.84f, r * 0.34f))
        // Topaklar karistirdikca kaybolur
        val lumps = ((1f - mix) * 9).toInt()
        for (i in 0 until lumps) {
            val a = i * 0.7f
            drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.7f), r * 0.05f, Offset(cx + kotlin.math.cos(a) * r * 0.55f * (0.4f + (i % 3) * 0.25f), cy - r * 0.1f + kotlin.math.sin(a) * r * 0.08f))
        }
        // Girdap
        drawArc(lerp(c, Color.White, 0.3f), 20f + mix * 300f, 200f, false, Offset(cx - r * 0.5f, cy - r * 0.2f), Size(r, r * 0.2f), style = Stroke(r * 0.02f))
    }
    drawPath(bowl, Brush.verticalGradient(listOf(Color(0xFF7FB8E0), Color(0xFF4A90C8)), cy, cy + r * 0.6f))
    drawOval(Color(0xFF9CC9E8), Offset(cx - r, cy - r * 0.32f), Size(r * 2, r * 0.44f), style = Stroke(r * 0.05f))
    drawOval(Color(0x55FFFFFF), Offset(cx - r * 0.7f, cy + r * 0.08f), Size(r * 0.35f, r * 0.12f))
}

private fun DrawScope.drawSpoon(p: Offset, r: Float) {
    drawLine(Color(0xFFC08457), p, p + Offset(r * 0.5f, -r * 0.9f), r * 0.07f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawOval(Color(0xFFD9A066), p - Offset(r * 0.1f, r * 0.07f), Size(r * 0.2f, r * 0.14f))
}

// ============================================================ 3. Pisir

@Composable
private fun BakeStep(state: CookingUiState, viewModel: CookingViewModel) {
    val result = state.work.bake
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Hint(stringResource(R.string.ck2_hint_bake))
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            drawOven(state, Rect(size.width * 0.18f, size.height * 0.05f, size.width * 0.82f, size.height * 0.95f))
        }
        // Zamanlayici
        Canvas(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 16.dp)) {
            drawRoundRect(Color(0xFFE5E7EB), cornerRadius = CornerRadius(size.height / 2))
            drawRect(Color(0xFFFDE68A), Offset(0f, 0f), Size(size.width * Cake.PERFECT_FROM, size.height))
            drawRect(Color(0xFF4ADE80), Offset(size.width * Cake.PERFECT_FROM, 0f), Size(size.width * (Cake.PERFECT_TO - Cake.PERFECT_FROM), size.height))
            drawRect(Color(0xFFF87171), Offset(size.width * 0.9f, 0f), Size(size.width * 0.1f, size.height))
            val x = size.width * state.bakeProgress
            drawLine(Color(0xFF1F2937), Offset(x, -4f), Offset(x, size.height + 4f), 6f)
        }
        if (result != null) {
            Text(
                stringResource(
                    when (result) {
                        BakeResult.RAW -> R.string.ck2_bake_raw
                        BakeResult.UNDER -> R.string.ck2_bake_under
                        BakeResult.PERFECT -> R.string.ck2_bake_perfect
                        BakeResult.OK -> R.string.ck2_bake_ok
                        BakeResult.BURNT -> R.string.ck2_bake_burnt
                    },
                ),
                color = if (result == BakeResult.PERFECT) Color(0xFF16A34A) else Color(0xFFB45309),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            Button(
                onClick = { if (state.baking) viewModel.takeOut() else viewModel.startBake() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.baking) Color(0xFFEF4444) else Pink, contentColor = Color.White),
                modifier = Modifier.padding(10.dp).height(50.dp).width(220.dp),
            ) { Text(stringResource(if (state.baking) R.string.ck2_take_out else R.string.ck2_put_in), fontWeight = FontWeight.Black, fontSize = 17.sp) }
        }
    }
}

private fun DrawScope.drawOven(state: CookingUiState, r: Rect) {
    drawRoundRect(Color(0xFFD1D5DB), r.topLeft, r.size, CornerRadius(r.width * 0.06f))
    drawRoundRect(Color(0xFF9CA3AF), r.topLeft, Size(r.width, r.height * 0.16f), CornerRadius(r.width * 0.06f))
    for (i in 0 until 4) drawCircle(Color(0xFF4B5563), r.height * 0.035f, Offset(r.left + r.width * (0.2f + i * 0.2f), r.top + r.height * 0.08f))
    val win = Rect(r.left + r.width * 0.1f, r.top + r.height * 0.24f, r.right - r.width * 0.1f, r.bottom - r.height * 0.1f)
    val glow = if (state.baking) 1f else 0.3f
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1F1F1F), lerp(Color(0xFF1F1F1F), Color(0xFFFF8A3D), 0.6f * glow)), win.top, win.bottom), win.topLeft, win.size, CornerRadius(r.width * 0.04f))
    // Kalip ve kabaran kek
    val flavor = state.work.flavor ?: Flavor.VANILLA
    val p = state.bakeProgress
    val brown = when {
        p < 0.6f -> 0f
        p < 0.85f -> (p - 0.6f) * 1.2f
        else -> 0.3f + (p - 0.85f) * 4f
    }.coerceIn(0f, 1f)
    val cakeColor = lerp(lerp(Color(0xFFF3E3C3), Color(flavor.sponge), (p * 1.6f).coerceAtMost(1f)), Color(0xFF3B2414), brown)
    val panW = win.width * 0.7f
    val panX = win.center.x - panW / 2
    val panY = win.bottom - win.height * 0.18f
    val rise = win.height * (0.12f + 0.3f * p.coerceAtMost(0.8f))
    drawRoundRect(cakeColor, Offset(panX + panW * 0.04f, panY - rise), Size(panW * 0.92f, rise + win.height * 0.05f), CornerRadius(panW * 0.2f, rise * 0.6f))
    drawRect(Color(0xFF6B7280), Offset(panX, panY), Size(panW, win.height * 0.1f))
    drawLine(Color(0xFF9CA3AF), Offset(win.left, panY + win.height * 0.1f), Offset(win.right, panY + win.height * 0.1f), 4f)
    drawRoundRect(Color(0x33FFFFFF), win.topLeft + Offset(win.width * 0.05f, win.height * 0.05f), Size(win.width * 0.15f, win.height * 0.5f), CornerRadius(8f))
}

// ============================================================ 4-5. Krema ve susleme

/** Pastanin ekrandaki yerlesimi. */
private class CakeGeo(w: Float, h: Float) {
    val cw = min(w * 0.7f, h * 1.1f)
    val cx = w / 2f
    val ry = cw * 0.16f
    val bodyH = cw * 0.38f
    val top = h * 0.5f - bodyH * 0.35f
    val left = cx - cw / 2
    val right = cx + cw / 2
    val bottom = top + bodyH

    /** Nokta hangi krema hucresinde (-1 = disarida). Ilk 6 ust yuzey, sonraki 6 yan. */
    fun cell(p: Offset): Int {
        if (p.x < left || p.x > right) return -1
        val col = ((p.x - left) / cw * 6).toInt().coerceIn(0, 5)
        return when {
            p.y in (top - ry)..(top + ry) -> col
            p.y in (top + ry)..(bottom + ry) -> 6 + col
            else -> -1
        }
    }

    /** Ust elips icindeki normalize koordinat (-1..1) ya da null. */
    fun onTop(p: Offset): Offset? {
        val nx = (p.x - cx) / (cw / 2)
        val ny = (p.y - top) / ry
        return if (nx * nx + ny * ny <= 0.85f) Offset(nx, ny) else null
    }
}

private fun DrawScope.drawCake(work: CakeWork, g: CakeGeo, showToppings: Boolean, emojiPaint: android.graphics.Paint) {
    val flavor = work.flavor ?: Flavor.VANILLA
    val sponge = Color(flavor.sponge)
    val burnt = when (work.bake) {
        BakeResult.BURNT -> 0.55f
        BakeResult.OK -> 0.18f
        else -> 0f
    }
    val spongeC = lerp(sponge, Color(0xFF3B2414), burnt)
    // Tabak
    drawOval(Color(0x22000000), Offset(g.left - g.cw * 0.12f, g.bottom - g.ry * 0.2f), Size(g.cw * 1.24f, g.ry * 2.6f))
    drawOval(Color.White, Offset(g.left - g.cw * 0.1f, g.bottom - g.ry * 0.4f), Size(g.cw * 1.2f, g.ry * 2.3f))
    drawOval(Color(0xFFE5E7EB), Offset(g.left - g.cw * 0.1f, g.bottom - g.ry * 0.4f), Size(g.cw * 1.2f, g.ry * 2.3f), style = Stroke(3f))
    // Govde
    val side = Path().apply {
        moveTo(g.left, g.top)
        lineTo(g.left, g.bottom)
        arcTo(Rect(g.left, g.bottom - g.ry, g.right, g.bottom + g.ry), 180f, -180f, false)
        lineTo(g.right, g.top)
        close()
    }
    drawPath(side, Brush.horizontalGradient(listOf(lerp(spongeC, Color.Black, 0.12f), spongeC, lerp(spongeC, Color.Black, 0.18f)), g.left, g.right))
    // Krema katmani (ortada)
    drawRect(Color(0xFFFFF8EE), Offset(g.left, g.top + g.bodyH * 0.5f), Size(g.cw, g.bodyH * 0.08f))
    val topOval = Path().apply { addOval(Rect(g.left, g.top - g.ry, g.right, g.top + g.ry)) }
    drawPath(topOval, lerp(spongeC, Color.White, 0.15f))

    // Krema
    val fr = work.frosting
    if (fr != null) {
        val fc = Color(fr.color)
        val colW = g.cw / 6f
        clipPath(topOval) {
            for (i in 0 until 6) {
                val a = work.frostCells[i]
                if (a > 0f) drawRect(fc.copy(alpha = (a * 1.1f).coerceAtMost(1f)), Offset(g.left + i * colW - 2f, g.top - g.ry), Size(colW + 4f, g.ry * 2))
            }
        }
        clipPath(side) {
            for (i in 0 until 6) {
                val a = work.frostCells[6 + i]
                if (a <= 0f) continue
                val x = g.left + i * colW
                val drip = g.bodyH * (0.25f + 0.6f * a)
                drawRect(fc.copy(alpha = (a * 1.1f).coerceAtMost(1f)), Offset(x - 1f, g.top), Size(colW + 2f, drip))
                drawCircle(fc.copy(alpha = (a * 1.1f).coerceAtMost(1f)), colW * 0.32f, Offset(x + colW / 2, g.top + drip))
            }
        }
        if (work.frostCoverage > 0.05f) drawOval(Color.White.copy(alpha = 0.25f * work.frostCoverage), Offset(g.left + g.cw * 0.15f, g.top - g.ry * 0.6f), Size(g.cw * 0.3f, g.ry * 0.5f))
    }

    if (showToppings) {
        emojiPaint.textSize = g.cw * 0.13f
        drawIntoCanvas { c ->
            work.toppings.sortedBy { it.y }.forEach { t ->
                c.nativeCanvas.drawText(t.topping.emoji, g.cx + t.x * g.cw / 2, g.top + t.y * g.ry + emojiPaint.textSize * 0.35f, emojiPaint)
            }
        }
    }
}

@Composable
private fun FrostStep(state: CookingUiState, viewModel: CookingViewModel) {
    val paint = remember { android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.CENTER } }
    Column(Modifier.fillMaxSize()) {
        Hint(stringResource(if (state.work.frosting == null) R.string.ck2_hint_pick_frosting else R.string.ck2_hint_frost))
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val g = CakeGeo(size.width.toFloat(), size.height.toFloat())
                        val cell = g.cell(change.position)
                        if (cell >= 0) viewModel.spread(cell, sqrt(drag.x * drag.x + drag.y * drag.y) / g.cw * 300f)
                    }
                },
        ) { drawCake(state.work, CakeGeo(size.width, size.height), false, paint) }
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Frosting.entries.forEach { f ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(f.color))
                            .border(if (state.work.frosting == f) 3.dp else 1.dp, if (state.work.frosting == f) Pink else Color(0x33000000), CircleShape)
                            .clickable { viewModel.pickFrosting(f) },
                    )
                }
            }
            Button(
                onClick = viewModel::frostingDone,
                enabled = state.work.frostCoverage > 0.3f,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
            ) { Text(stringResource(R.string.ck2_next_step), fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun DecorateStep(state: CookingUiState, viewModel: CookingViewModel) {
    val paint = remember { android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.CENTER } }
    Column(Modifier.fillMaxSize()) {
        Hint(stringResource(R.string.ck2_hint_decorate))
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val g = CakeGeo(size.width.toFloat(), size.height.toFloat())
                        g.onTop(p)?.let { viewModel.place(it.x, it.y) }
                    }
                },
        ) { drawCake(state.work, CakeGeo(size.width, size.height), true, paint) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Topping.entries.forEach { t ->
                Text(
                    t.emoji,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.topping == t) Pink.copy(alpha = 0.3f) else Color.White)
                        .border(if (state.topping == t) 2.dp else 1.dp, if (state.topping == t) Pink else Color(0x22000000), RoundedCornerShape(12.dp))
                        .clickable { viewModel.pickTopping(t) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = viewModel::undoTopping,
                enabled = state.work.toppings.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHigh, contentColor = AppColors.TextPrimary),
            ) { Text("↶", fontWeight = FontWeight.Black, fontSize = 18.sp) }
            Button(
                onClick = viewModel::serve,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.ck2_serve), fontWeight = FontWeight.Black, fontSize = 16.sp) }
        }
    }
}

// ============================================================ 6. Servis

@Composable
private fun ServeStep(state: CookingUiState, onNext: () -> Unit) {
    val paint = remember { android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.CENTER } }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.weight(1f).fillMaxWidth()) { drawCake(state.work, CakeGeo(size.width, size.height), true, paint) }
        Text("★".repeat(state.stars) + "☆".repeat(3 - state.stars), color = AppColors.Gold, fontSize = 34.sp)
        Text(
            stringResource(
                when (state.stars) {
                    3 -> R.string.ck2_result_3
                    2 -> R.string.ck2_result_2
                    1 -> R.string.ck2_result_1
                    else -> R.string.ck2_result_0
                },
            ),
            color = Color(0xFF5A3A2A),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (state.goldEarned > 0) {
            Text(stringResource(R.string.game_gold_earned, state.goldEarned), color = Color(0xFFB45309), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
            modifier = Modifier.padding(10.dp),
        ) { Text(stringResource(R.string.mk_next), fontWeight = FontWeight.Black) }
    }
}
