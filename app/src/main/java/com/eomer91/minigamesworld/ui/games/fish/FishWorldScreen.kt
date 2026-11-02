package com.eomer91.minigamesworld.ui.games.fish

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val PearlColor = Color(0xFFF1F5F9)
private val SeaTop = Color(0xFF38BDF8)
private val SeaDeep = Color(0xFF020617)
private val SkyTop = Color(0xFF7DD3FC)
private val SkyBottom = Color(0xFFE0F2FE)
private val JellyColor = Color(0xFFF472B6)

/** Gorunen alanin en derin noktasi (renk gecisi icin). */
private const val DEEPEST = 135f

@Composable
fun FishWorldScreen(viewModel: FishWorldViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Geri tusu: alt ekranlardan limana (hub) dondurur.
    BackHandler(enabled = state.screen != FishScreen.HUB && state.screen != FishScreen.LOADING) { viewModel.openHub() }

    // Kare dongusu: sadece on planda.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
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

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        when (state.screen) {
            FishScreen.LOADING -> Unit
            FishScreen.HUB -> HubContent(
                state = state,
                onFish = { showInterstitial { viewModel.startFishing() } },
                onShop = viewModel::openShop,
                onAquarium = viewModel::openAquarium,
            )
            FishScreen.FISHING -> FishingContent(
                state = state,
                viewModel = viewModel,
                onRestart = { showInterstitial { viewModel.startFishing() } },
                onExtraCast = { showRewarded({ viewModel.extraCast() }, adNotReady) },
            )
            FishScreen.SHOP -> ShopContent(state.profile, onBuy = viewModel::buy, onBack = viewModel::openHub)
            FishScreen.AQUARIUM -> AquariumSoonContent(state.profile, onBack = viewModel::openHub)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------- Liman (hub)

@Composable
private fun HubContent(state: FishWorldState, onFish: () -> Unit, onShop: () -> Unit, onAquarium: () -> Unit) {
    val p = state.profile
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PearlCounter(p.pearls)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.game_best) + ": " + state.best,
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Dekoratif deniz manzarasi
        val t = rememberAnimTime()
        Canvas(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(18.dp)),
        ) { drawHubScene(t) }

        Spacer(Modifier.height(18.dp))
        BigButton(stringResource(R.string.fish_go_fishing), AppColors.Primary, onFish)
        Spacer(Modifier.height(10.dp))
        BigButton(stringResource(R.string.fish_rod_shop), AppColors.Gold, onShop)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onAquarium,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                stringResource(R.string.fish_aquarium) + "  ·  " +
                    stringResource(R.string.fish_aquarium_count, p.aquarium.size, p.aquariumCapacity),
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.fish_rod_summary, RodStats.maxDepth(p.rodDepthLevel).roundToInt(), RodStats.capacity(p.hookCapacityLevel)),
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.fish_how_to),
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun rememberAnimTime(): Float {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) time += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    return time
}

private fun DrawScope.drawHubScene(t: Float) {
    val surface = size.height * 0.3f
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom), endY = surface), size = Size(size.width, surface))
    drawRect(
        Brush.verticalGradient(listOf(SeaTop, Color(0xFF0369A1), Color(0xFF0C4A6E)), startY = surface, endY = size.height),
        topLeft = Offset(0f, surface),
        size = Size(size.width, size.height - surface),
    )
    val unit = size.width / FishingEngine.WIDTH
    drawBoat(Offset(size.width * 0.5f, surface), unit, t)
    // Yuzen birkac balik
    val demo = listOf(FishSpecies.CLOWNFISH, FishSpecies.BLUE_TANG, FishSpecies.ANGELFISH, FishSpecies.GOLDFISH)
    demo.forEachIndexed { i, sp ->
        val speed = 0.6f + i * 0.15f
        val dir = if (i % 2 == 0) 1 else -1
        val span = FishingEngine.WIDTH + 2f
        val raw = (t * speed + i * 2.7f) % span
        val x = if (dir > 0) raw - 1f else FishingEngine.WIDTH + 1f - raw
        val y = surface + (size.height - surface) * (0.25f + i * 0.18f) + sin(t * 2f + i) * unit * 0.15f
        drawFish(Offset(x * unit, y), sp, dir, unit * 1.1f, t)
    }
}

// ---------------------------------------------------------------- Balik tutma

@Composable
private fun FishingContent(
    state: FishWorldState,
    viewModel: FishWorldViewModel,
    onRestart: () -> Unit,
    onExtraCast: () -> Unit,
) {
    val cast = state.cast ?: return
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        ScoreHeader(score = state.sessionScore, best = state.best, onRestart = onRestart)

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            InfoChip(stringResource(R.string.fish_casts_left, state.castsLeft))
            Spacer(Modifier.width(6.dp))
            InfoChip(stringResource(R.string.fish_depth, cast.depth.roundToInt(), cast.maxDepth.roundToInt()))
            Spacer(Modifier.width(6.dp))
            InfoChip(stringResource(R.string.fish_hooked, cast.hooked.size, cast.capacity))
            Spacer(Modifier.weight(1f))
            PearlCounter(state.profile.pearls, small = true)
        }

        Box(
            Modifier
                .padding(top = 10.dp)
                .weight(1f, fill = false)
                .aspectRatio(FishingEngine.WIDTH / FishingEngine.VIEW_HEIGHT)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val unit = size.width / FishingEngine.WIDTH
                        viewModel.onPress(down.position.x / unit)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            viewModel.onDrag(change.position.x / unit)
                            change.consume()
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) { drawSea(cast) }

            if (cast.phase == CastPhase.READY && !state.sessionOver) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    state.lastCastValue?.let {
                        Text(
                            stringResource(R.string.fish_cast_result, it),
                            color = AppColors.Gold,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        stringResource(R.string.fish_tap_to_cast),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        if (state.sessionOver) {
            if (!state.extraCastUsed) {
                Button(
                    onClick = onExtraCast,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                ) {
                    Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.fish_extra_cast), fontWeight = FontWeight.Black)
                }
            }
            val pearls = state.sessionScore / FishWorldViewModel.SCORE_PER_PEARL
            GameOverPanel(
                score = state.sessionScore,
                isNewRecord = state.isNewRecord,
                onRestart = onRestart,
                title = stringResource(R.string.fish_session_over),
                titleColor = AppColors.Primary,
                detail = stringResource(R.string.fish_session_detail, state.sessionCatch.size, pearls) +
                    if (state.overflowSold > 0) "\n" + stringResource(R.string.fish_overflow, state.overflowSold) else "",
                goldEarned = state.goldEarned,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = viewModel::openHub, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.fish_back_to_harbor), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun DrawScope.drawSea(c: FishingCast) {
    val unit = size.width / FishingEngine.WIDTH
    val viewH = size.height / unit
    // Kamera: kanca ekranin ust ucte birinde kalsin; yuzeyde gokyuzu gorunsun
    val camTop = maxOf(-3.5f, c.depth - viewH * 0.33f)
    fun y(depth: Float) = (depth - camTop) * unit

    // Su rengi derinlige gore koyulasir
    val topColor = lerp(SeaTop, SeaDeep, (maxOf(camTop, 0f) / DEEPEST).coerceIn(0f, 1f))
    val bottomColor = lerp(SeaTop, SeaDeep, ((camTop + viewH) / DEEPEST).coerceIn(0f, 1f))
    val surfaceY = y(0f)
    drawRect(Brush.verticalGradient(listOf(topColor, bottomColor)))
    if (surfaceY > 0f) {
        drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom), endY = surfaceY), size = Size(size.width, surfaceY))
        // Dalga cizgisi
        val wave = Path().apply {
            moveTo(0f, surfaceY)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, surfaceY + sin(x / unit * 2f + c.time * 3f) * unit * 0.08f)
                x += unit * 0.25f
            }
        }
        drawPath(wave, Color.White.copy(alpha = 0.8f), style = Stroke(2.dp.toPx()))
    }

    // Derinlik isaretleri (her 10 m)
    var mark = floor(maxOf(camTop, 0f) / 10f) * 10f
    while (mark <= camTop + viewH) {
        if (mark > 0f) {
            val my = y(mark)
            drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, my), Offset(unit * 0.6f, my), 2.dp.toPx())
        }
        mark += 10f
    }

    // Oltanin ulasabildigi en derin nokta
    val maxY = y(c.maxDepth)
    if (maxY in 0f..size.height) {
        drawLine(
            Color(0xFFF87171).copy(alpha = 0.7f),
            Offset(0f, maxY),
            Offset(size.width, maxY),
            2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
    }

    // Canlilar
    c.creatures.forEach { cr ->
        if (cr.caught) return@forEach
        val cy = y(cr.depth)
        if (cy < -unit * 2 || cy > size.height + unit * 2) return@forEach
        val center = Offset(cr.x * unit, cy)
        val sp = cr.species
        if (sp == null) drawJelly(center, unit, c.time + cr.id) else drawFish(center, sp, cr.dir, sp.size * unit, c.time)
    }

    // Tekne + misina + kanca
    val boatPos = Offset(FishingEngine.WIDTH / 2f * unit, surfaceY)
    if (surfaceY > -unit * 3) drawBoat(boatPos, unit, c.time)
    val rodTip = Offset(boatPos.x + unit * 1.1f, surfaceY - unit * 1.3f)
    val hook = Offset(c.hookX * unit, y(c.depth))
    drawLine(Color.White.copy(alpha = 0.85f), rodTip, hook, 1.5.dp.toPx())
    val stunned = c.stunned > 0f && sin(c.time * 40f) > 0f
    drawHook(hook, unit, if (stunned) JellyColor else Color(0xFFE2E8F0))

    // Kancadaki baliklar asagi sarkar
    c.hooked.forEachIndexed { i, sp ->
        val len = sp.size * unit * 0.9f
        val pos = Offset(hook.x + sin(c.time * 6f + i) * unit * 0.1f, hook.y + unit * 0.35f + i * unit * 0.35f + len * 0.5f)
        rotate(-90f, pos) { drawFish(pos, sp, 1, len, c.time) }
    }
}

private fun DrawScope.drawHook(p: Offset, unit: Float, color: Color) {
    val r = unit * 0.18f
    val path = Path().apply {
        moveTo(p.x, p.y - r * 1.4f)
        lineTo(p.x, p.y + r * 0.4f)
        quadraticTo(p.x, p.y + r * 1.5f, p.x - r, p.y + r * 1.2f)
        lineTo(p.x - r * 1.2f, p.y + r * 0.5f)
    }
    drawPath(path, color, style = Stroke(2.5.dp.toPx()))
    drawCircle(Color(0xFFEF4444), r * 0.55f, Offset(p.x, p.y - r * 1.6f))
}

private fun DrawScope.drawBoat(surface: Offset, unit: Float, t: Float) {
    val bob = sin(t * 2f) * unit * 0.06f
    val w = unit * 2.6f
    val h = unit * 0.7f
    val top = surface.y - h * 0.55f + bob
    val hull = Path().apply {
        moveTo(surface.x - w / 2f, top)
        lineTo(surface.x + w / 2f, top)
        lineTo(surface.x + w * 0.36f, top + h)
        lineTo(surface.x - w * 0.36f, top + h)
        close()
    }
    drawPath(hull, Color(0xFFB45309))
    drawRect(Color(0xFF78350F), Offset(surface.x - w / 2f, top), Size(w, h * 0.18f))
    // Balikci (basit)
    drawCircle(Color(0xFFFCD34D), unit * 0.22f, Offset(surface.x - unit * 0.2f, top - unit * 0.55f))
    drawRect(Color(0xFF2563EB), Offset(surface.x - unit * 0.42f, top - unit * 0.35f), Size(unit * 0.44f, unit * 0.36f))
    // Olta kamisi
    drawLine(
        Color(0xFF451A03),
        Offset(surface.x - unit * 0.05f, top - unit * 0.2f),
        Offset(surface.x + unit * 1.1f, surface.y - unit * 1.3f + bob),
        3.dp.toPx(),
    )
}

/** Yana bakan bir balik: [length] burundan kuyruga piksel. */
private fun DrawScope.drawFish(c: Offset, sp: FishSpecies, dir: Int, length: Float, t: Float) {
    val body = Color(sp.color)
    val accent = Color(sp.accent)
    val h = length * (if (sp == FishSpecies.ANGELFISH) 0.9f else if (sp == FishSpecies.SWORDFISH) 0.35f else 0.55f)
    val bodyLen = length * 0.75f
    val wag = sin(t * 10f + c.x) * h * 0.12f
    // Kuyruk
    val tailBase = c.x - dir * bodyLen * 0.45f
    val tail = Path().apply {
        moveTo(tailBase, c.y)
        lineTo(tailBase - dir * length * 0.25f, c.y - h * 0.45f + wag)
        lineTo(tailBase - dir * length * 0.25f, c.y + h * 0.45f + wag)
        close()
    }
    drawPath(tail, accent.copy(alpha = 0.9f))
    // Govde
    drawOval(body, Offset(c.x - bodyLen / 2f, c.y - h / 2f), Size(bodyLen, h))
    // Ozel desenler
    when (sp) {
        FishSpecies.CLOWNFISH -> listOf(-0.15f, 0.15f).forEach { k ->
            drawRect(accent, Offset(c.x + k * bodyLen - bodyLen * 0.05f, c.y - h * 0.42f), Size(bodyLen * 0.1f, h * 0.84f))
        }
        FishSpecies.SWORDFISH -> drawLine(accent, Offset(c.x + dir * bodyLen * 0.45f, c.y), Offset(c.x + dir * length * 0.75f, c.y), h * 0.15f)
        FishSpecies.ANGLERFISH -> {
            val lure = Offset(c.x + dir * bodyLen * 0.55f, c.y - h * 0.9f)
            drawLine(Color(0xFF263238), Offset(c.x + dir * bodyLen * 0.2f, c.y - h * 0.45f), lure, 2f)
            drawCircle(accent.copy(alpha = 0.6f + 0.4f * sin(t * 5f)), h * 0.16f, lure)
        }
        FishSpecies.GOLDFISH -> drawCircle(Color.White.copy(alpha = 0.5f + 0.5f * sin(t * 6f)), h * 0.12f, Offset(c.x - dir * bodyLen * 0.1f, c.y - h * 0.25f))
        FishSpecies.PUFFERFISH -> drawCircle(accent.copy(alpha = 0.5f), h * 0.1f, Offset(c.x, c.y + h * 0.15f))
        else -> drawLine(accent.copy(alpha = 0.6f), Offset(c.x - bodyLen * 0.3f, c.y), Offset(c.x + bodyLen * 0.3f, c.y), h * 0.12f)
    }
    // Goz
    val eye = Offset(c.x + dir * bodyLen * 0.28f, c.y - h * 0.1f)
    drawCircle(Color.White, h * 0.13f, eye)
    drawCircle(Color.Black, h * 0.07f, eye)
}

private fun DrawScope.drawJelly(c: Offset, unit: Float, t: Float) {
    val r = unit * 0.45f
    val pulse = 1f + sin(t * 4f) * 0.08f
    // Dokunaclar
    for (k in -2..2) {
        val x = c.x + k * r * 0.35f
        val path = Path().apply {
            moveTo(x, c.y)
            quadraticTo(x + sin(t * 3f + k) * r * 0.3f, c.y + r * 0.8f, x, c.y + r * 1.5f)
        }
        drawPath(path, JellyColor.copy(alpha = 0.6f), style = Stroke(2.dp.toPx()))
    }
    drawArc(
        JellyColor.copy(alpha = 0.8f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(c.x - r * pulse, c.y - r * pulse),
        size = Size(r * 2f * pulse, r * 2f * pulse),
    )
}

// ---------------------------------------------------------------- Olta dukkani

@Composable
private fun ShopContent(profile: FishProfile, onBuy: (RodUpgrade) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        HeaderRow(stringResource(R.string.fish_rod_shop), profile.pearls, onBack)
        Spacer(Modifier.height(12.dp))
        RodUpgrade.entries.forEach { up ->
            val level = up.level(profile)
            val maxed = level >= up.maxLevel
            val cost = if (maxed) 0 else up.cost(level)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.SurfaceHigh)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(up.labelRes), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.fish_level, level, up.maxLevel), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
                Text(
                    statText(up, level) + if (maxed) "" else "  →  " + statText(up, level + 1),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Button(
                    onClick = { onBuy(up) },
                    enabled = !maxed && profile.pearls >= cost,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(44.dp),
                ) {
                    if (maxed) {
                        Text(stringResource(R.string.fish_max), fontWeight = FontWeight.Black)
                    } else {
                        PearlDot(14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.fish_buy, cost), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun statText(up: RodUpgrade, level: Int): String = when (up) {
    RodUpgrade.DEPTH -> stringResource(R.string.fish_stat_depth, RodStats.maxDepth(level).roundToInt())
    RodUpgrade.CAPACITY -> stringResource(R.string.fish_stat_capacity, RodStats.capacity(level))
    RodUpgrade.REEL -> stringResource(R.string.fish_stat_reel, (RodStats.speedFactor(level) * 100).roundToInt())
}

// ---------------------------------------------------------------- Akvaryum (adim 2'de gelecek)

@Composable
private fun AquariumSoonContent(profile: FishProfile, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        HeaderRow(stringResource(R.string.fish_aquarium), profile.pearls, onBack)
        Text(
            stringResource(R.string.fish_aquarium_soon),
            color = AppColors.Gold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(R.string.fish_aquarium_count, profile.aquarium.size, profile.aquariumCapacity),
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        val discovered = FishSpecies.entries.count { (profile.caughtTotals[it.name] ?: 0) > 0 }
        Text(
            stringResource(R.string.fish_collection, discovered, FishSpecies.entries.size),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FishSpecies.entries.forEach { sp ->
            val total = profile.caughtTotals[sp.name] ?: 0
            val inTank = profile.aquarium.count { it.species == sp.name }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(width = 44.dp, height = 28.dp)) {
                    if (total > 0) drawFish(Offset(size.width / 2f, size.height / 2f), sp, 1, size.width * 0.8f, 0f)
                    else drawCircle(AppColors.TextSecondary.copy(alpha = 0.3f), size.height * 0.35f)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (total > 0) stringResource(sp.nameRes) else "???",
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.fish_species_info, sp.minDepth.roundToInt(), sp.value),
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Text("×$inTank / $total", color = AppColors.TextMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------------- Ortak parcalar

@Composable
private fun HeaderRow(title: String, pearls: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = AppColors.TextPrimary)
        }
        Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        PearlCounter(pearls)
    }
}

@Composable
private fun BigButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = AppColors.OnPrimary),
        modifier = Modifier.fillMaxWidth().height(54.dp),
    ) { Text(text, fontWeight = FontWeight.Black, fontSize = 17.sp) }
}

@Composable
private fun PearlDot(sizeDp: Dp) {
    Box(
        Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White, PearlColor, Color(0xFFCBD5E1)))),
    )
}

@Composable
private fun PearlCounter(pearls: Int, small: Boolean = false) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.SurfaceHigh)
            .padding(horizontal = 10.dp, vertical = if (small) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        PearlDot(if (small) 12.dp else 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(pearls.toString(), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = if (small) 13.sp else 16.sp)
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text,
        color = AppColors.TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
