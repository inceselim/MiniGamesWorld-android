package com.eomer91.minigamesworld.ui.games.forest

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ekranin yuksekligine sigan dunya birimi (yatay ekranda yaklasik 10-11 karo gorunur). */
private const val VIEW_HEIGHT_UNITS = 10.5f
private val Gold = Color(0xFFF7D046)
private val HpRed = Color(0xFFE04848)
private val MpBlue = Color(0xFF38BDF8)
private val XpPurple = Color(0xFFA66EE0)

@Composable
fun ForestScreen(onBack: () -> Unit, viewModel: ForestViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LandscapeImmersive()

    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Uygulama arka plana gecince oyun duraklar.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) viewModel.pause() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
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

    BackHandler {
        when (state.phase) {
            ForestPhase.PLAYING -> viewModel.pause()
            ForestPhase.PAUSED -> viewModel.resume()
            ForestPhase.RESULT -> viewModel.toHub()
            ForestPhase.LEVEL_UP -> Unit
            ForestPhase.HUB, ForestPhase.LOADING -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
        when (state.phase) {
            ForestPhase.LOADING -> Unit
            ForestPhase.HUB -> HubContent(state, viewModel, onBack, onPlay = { showInterstitial { viewModel.startRun() } })
            else -> {
                GameContent(viewModel)
                when (state.phase) {
                    ForestPhase.LEVEL_UP -> LevelUpOverlay(state.choices, viewModel.engine, viewModel::choose)
                    ForestPhase.PAUSED -> PauseOverlay(viewModel)
                    ForestPhase.RESULT -> ResultOverlay(
                        state = state,
                        onRevive = { showRewarded({ viewModel.revive() }, adNotReady) },
                        onGiveUp = viewModel::declineRevive,
                        onRetry = { showInterstitial { viewModel.startRun() } },
                        onNext = { showInterstitial { viewModel.nextMap() } },
                        onHub = viewModel::toHub,
                    )
                    else -> Unit
                }
                if (state.bossWarning > 0f && state.phase == ForestPhase.PLAYING) {
                    Text(
                        stringResource(R.string.fg_boss_warning),
                        color = HpRed,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** Bu oyunda ekran yatay ve tam ekran; cikinca eski haline doner. */
@Composable
private fun LandscapeImmersive() {
    val activity = LocalActivity.current
    val view = LocalView.current
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

// ============================================================ Hub (harita secimi)

@Composable
private fun HubContent(state: ForestUiState, viewModel: ForestViewModel, onBack: () -> Unit, onPlay: () -> Unit) {
    val p = state.profile
    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(Modifier.width(240.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(AppColors.Surface)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Canvas(Modifier.size(40.dp)) { sprite(ForestSprites.hero, size.width / 2f, size.height * 0.85f, size.height) }
            }
            Text(stringResource(R.string.game_forest_name), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Canvas(Modifier.size(18.dp)) { sprite(ForestSprites.coin, size.width / 2f, size.height, size.height) }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.fg_coins, p.coins), color = Gold, fontWeight = FontWeight.Black)
            }
            Text(stringResource(R.string.fg_how_to), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            Text(
                stringResource(R.string.fg_hero_soon),
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(stringResource(R.string.fg_maps), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Row(
                Modifier.weight(1f).padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ForestMap.entries.forEach { m ->
                    MapCard(m, p, selected = m == state.selectedMap, onClick = { viewModel.selectMap(m) })
                }
            }
            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80), contentColor = AppColors.OnPrimary),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
            ) {
                Text(stringResource(R.string.fg_play, stringResource(state.selectedMap.nameRes)), fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun MapCard(m: ForestMap, p: ForestProfile, selected: Boolean, onClick: () -> Unit) {
    val locked = m.ordinal > p.unlocked
    Column(
        Modifier
            .width(150.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(3.dp, if (selected) Color(0xFF4ADE80) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(enabled = !locked, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(m.grass))
                val u = size.height / 4f
                sprite(ForestSprites.tree, u * 0.9f, u * 2.3f, u * 2.2f)
                sprite(ForestSprites.bush, size.width - u * 0.8f, u * 3.6f, u * 1.4f)
                sprite(ForestSprites.hero, size.width / 2f, u * 3.3f, u * 1.2f)
                val enemy = when (m) {
                    ForestMap.EDGE -> ForestSprites.goblin
                    ForestMap.SWAMP -> ForestSprites.bat
                    ForestMap.MUSHROOM -> ForestSprites.wolf
                    ForestMap.VALLEY -> ForestSprites.orc
                    ForestMap.DARK -> ForestSprites.treant
                }
                sprite(enemy, size.width - u * 1.1f, u * 1.8f, u * 1.3f)
                if (locked) drawRect(Color.Black.copy(alpha = 0.55f))
            }
            if (locked) Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(30.dp))
        }
        Text(
            "${m.ordinal + 1}. " + stringResource(m.nameRes),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            when {
                locked -> stringResource(R.string.fg_locked)
                m.ordinal in p.cleared -> stringResource(R.string.fg_cleared) + " · " + stringResource(R.string.fg_best, p.bestKills[m.ordinal] ?: 0)
                else -> stringResource(R.string.fg_best, p.bestKills[m.ordinal] ?: 0)
            },
            color = if (m.ordinal in p.cleared) Color(0xFF4ADE80) else AppColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = 2,
        )
    }
}

// ============================================================ Oyun ekrani

@Composable
private fun GameContent(viewModel: ForestViewModel) {
    val density = LocalDensity.current
    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { s ->
                val unit = s.height / VIEW_HEIGHT_UNITS
                viewModel.setViewSize(s.width / unit, VIEW_HEIGHT_UNITS)
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val f = viewModel.frame // her karede yeniden ciz
            val e = viewModel.engine ?: return@Canvas
            textPaint.textSize = with(density) { 14.sp.toPx() }
            drawWorld(e, size.height / VIEW_HEIGHT_UNITS, textPaint)
        }
        Joystick(viewModel)
        Hud(viewModel)
    }
}

@Composable
private fun BoxScope.Joystick(viewModel: ForestViewModel) {
    var origin by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = with(LocalDensity.current) { 56.dp.toPx() }
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val o = down.position
                    origin = o
                    knob = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        var d = change.position - o
                        val len = d.getDistance()
                        if (len > maxRadius) d *= maxRadius / len
                        knob = d
                        viewModel.setInput(d.x / maxRadius, d.y / maxRadius)
                        change.consume()
                    }
                    origin = null
                    knob = Offset.Zero
                    viewModel.setInput(0f, 0f)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Dokunulmuyorsa sol altta soluk bir ipucu
            val o = origin ?: Offset(maxRadius * 1.8f, size.height - maxRadius * 1.8f)
            val a = if (origin == null) 0.25f else 0.5f
            drawCircle(Color.White.copy(alpha = a * 0.5f), maxRadius, o)
            drawCircle(Color.White.copy(alpha = a), maxRadius, o, style = Stroke(3.dp.toPx()))
            drawCircle(Color.White.copy(alpha = a + 0.2f), maxRadius * 0.42f, o + knob)
        }
    }
}

@Composable
private fun BoxScope.Hud(viewModel: ForestViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val f = viewModel.frame
    val e = viewModel.engine ?: return

    // Ust: XP cubugu
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(Color.Black.copy(alpha = 0.6f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((e.xp / e.xpNeeded).coerceIn(0f, 1f)).background(XpPurple))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HudChip(stringResource(R.string.fg_lv, e.level))
            Spacer(Modifier.width(6.dp))
            HudChip(formatTime(e.time) + " / " + formatTime(ForestEngine.MAP_SECONDS))
            Spacer(Modifier.width(6.dp))
            HudChip("💀 ${e.kills}")
            Spacer(Modifier.width(6.dp))
            HudChip("🪙 ${e.coins}", Gold)
            Spacer(Modifier.weight(1f))
            // Silah ve pasif simgeleri
            e.weapons.forEach { (w, l) -> SlotIcon(Color(w.color), l) }
            Spacer(Modifier.width(4.dp))
            e.passives.forEach { (p, l) -> SlotIcon(Color(p.color), l, round = true) }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = viewModel::pause,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
            ) { Icon(Icons.Rounded.Pause, null, tint = Color.White) }
        }
        // Boss can cubugu
        e.boss?.takeIf { it.alive }?.let { b ->
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((b.hp / b.maxHp).coerceIn(0f, 1f)).background(HpRed))
            }
        }
    }

    // Sag alt: atilma ve ozel yetenek
    Row(
        Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 24.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ActionButton(
            label = stringResource(R.string.fg_skill),
            size = 74.dp,
            fill = (e.mp / ForestEngine.SKILL_COST).coerceIn(0f, 1f),
            color = MpBlue,
            ready = e.canSkill,
            onPress = viewModel::skill,
        )
        ActionButton(
            label = stringResource(R.string.fg_dash),
            size = 86.dp,
            fill = 1f - (e.dashCd / ForestEngine.DASH_COOLDOWN).coerceIn(0f, 1f),
            color = Color(0xFF4ADE80),
            ready = e.canDash,
            onPress = viewModel::dash,
        )
    }
}

@Composable
private fun HudChip(text: String, color: Color = Color.White) {
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun SlotIcon(color: Color, level: Int, round: Boolean = false) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(24.dp)
            .clip(if (round) CircleShape else RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(level.toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ActionButton(label: String, size: Dp, fill: Float, color: Color, ready: Boolean, onPress: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures(onPress = { onPress() }) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawArc(
                color.copy(alpha = if (ready) 1f else 0.6f),
                startAngle = -90f,
                sweepAngle = 360f * fill,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(stroke),
            )
            if (ready) drawCircle(color.copy(alpha = 0.25f))
        }
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

private fun formatTime(t: Float): String {
    val s = t.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

// ============================================================ Cizim

private fun hash(x: Int, y: Int): Int = ((x * 73856093) xor (y * 19349663) xor 0x5bd1e995).let { (it xor (it ushr 13)) and 0x7fffffff }

private fun DrawScope.drawWorld(e: ForestEngine, unit: Float, paint: android.graphics.Paint) {
    val viewW = size.width / unit
    val viewH = size.height / unit
    val camX = e.heroX - viewW / 2f
    val camY = e.heroY - viewH / 2f
    fun sx(x: Float) = (x - camX) * unit
    fun sy(y: Float) = (y - camY) * unit

    // Zemin ve sus (sabit tohumlu, kahraman yurudukce akar)
    drawRect(Color(e.map.grass))
    val x0 = floor(camX).toInt() - 2
    val y0 = floor(camY).toInt() - 2
    val dark = Color(e.map.grassDark)
    for (ty in y0..(y0 + viewH.toInt() + 4)) {
        for (tx in x0..(x0 + viewW.toInt() + 4)) {
            val h = hash(tx, ty)
            val px = sx(tx.toFloat())
            val py = sy(ty.toFloat())
            if (h % 7 == 0) drawRect(dark, Offset(px, py), Size(unit + 1, unit + 1))
            when {
                h % 5 == 1 -> sprite(ForestSprites.tuft, px + unit * 0.5f, py + unit * 0.8f, unit * 0.6f)
                h % 97 == 3 -> sprite(ForestSprites.tree, px + unit * 0.5f, py + unit, unit * 2.6f)
                h % 41 == 5 -> sprite(ForestSprites.bush, px + unit * 0.5f, py + unit, unit * 1.2f)
                h % 53 == 7 -> sprite(ForestSprites.rock, px + unit * 0.5f, py + unit, unit * 1.0f)
                h % 31 == 9 -> sprite(ForestSprites.flower, px + unit * 0.5f, py + unit * 0.8f, unit * 0.5f)
            }
        }
    }

    // Diken halkasi (kahramanin altinda)
    e.weapons[WeaponType.THORNS]?.let { l ->
        val r = e.thornRadius(l) * unit
        val c = Offset(sx(e.heroX), sy(e.heroY))
        drawCircle(Color(0xFF8FE36B).copy(alpha = 0.16f), r, c)
        drawCircle(Color(0xFF8FE36B).copy(alpha = 0.5f), r, c, style = Stroke(2.dp.toPx()))
    }

    // Toplanabilirler
    for (p in e.pickups) {
        val x = sx(p.x)
        val y = sy(p.y)
        if (x < -unit || x > size.width + unit || y < -unit || y > size.height + unit) continue
        when (p.kind) {
            PickupKind.GEM -> sprite(ForestSprites.gem, x, y + unit * 0.2f, unit * 0.45f)
            PickupKind.BIG_GEM -> sprite(ForestSprites.gem2, x, y + unit * 0.25f, unit * 0.6f)
            PickupKind.COIN -> sprite(ForestSprites.coin, x, y + unit * 0.2f, unit * 0.45f)
            PickupKind.MEAT -> sprite(ForestSprites.meat, x, y + unit * 0.25f, unit * 0.6f)
            PickupKind.CHEST -> sprite(ForestSprites.chest, x, y + unit * 0.5f, unit * 1.3f)
        }
    }

    // Buz halkalari
    for (n in e.novas) {
        val r = n.maxRadius * (1f - n.life / 0.45f) * unit
        drawCircle(Color(0xFFB8F0FF).copy(alpha = 0.7f * n.life / 0.45f), r, Offset(sx(n.x), sy(n.y)), style = Stroke(5.dp.toPx()))
    }

    // Dusmanlar
    for (en in e.enemies) {
        val x = sx(en.x)
        val y = sy(en.y)
        val s = en.type.drawSize * unit
        if (x < -s || x > size.width + s || y < -s || y > size.height + s) continue
        drawOval(Color.Black.copy(alpha = 0.25f), Offset(x - s * 0.3f, y + s * 0.05f), Size(s * 0.6f, s * 0.18f))
        val bob = if (en.type == EnemyType.BAT) sin(e.time * 14f + en.id) * unit * 0.12f else sin(e.time * 10f + en.id) * unit * 0.04f
        val img = when (en.type) {
            EnemyType.GOBLIN -> ForestSprites.goblin
            EnemyType.BAT -> ForestSprites.bat
            EnemyType.WOLF -> ForestSprites.wolf
            EnemyType.ORC -> ForestSprites.orc
            EnemyType.TREANT -> ForestSprites.treant
        }
        // Kurt cizimi sola bakiyor; digerleri saga
        val flip = if (en.type == EnemyType.WOLF) !en.facingLeft else en.facingLeft
        val tint = when {
            en.flash > 0f -> Color.White
            en.slow > 0f -> Color(0xFF9BE7FF)
            else -> null
        }
        sprite(img, x, y + s * 0.1f + bob, s, flip = flip, tint = tint, tintAlpha = if (en.slow > 0f && en.flash <= 0f) 0.45f else 1f)
        if (en.type.isBoss && en.charging > 0f) drawCircle(HpRed.copy(alpha = 0.3f), s * 0.6f, Offset(x, y - s * 0.3f))
    }

    // Kahraman
    val hx = sx(e.heroX)
    val hy = sy(e.heroY)
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(hx - unit * 0.3f, hy + unit * 0.05f), Size(unit * 0.6f, unit * 0.18f))
    val walk = e.moving && ((e.time * 7f).toInt() % 2 == 0)
    val blink = e.iframe > 0f && ((e.time * 20f).toInt() % 2 == 0) && e.dashTime <= 0f
    if (e.dashTime > 0f) {
        // Atilma izi
        for (k in 1..3) {
            sprite(ForestSprites.hero, hx - e.facingX * unit * 0.35f * k, hy - e.facingY * unit * 0.35f * k + unit * 0.1f, unit * 1.1f, flip = e.facingX < 0f, alpha = 0.25f / k)
        }
    }
    sprite(if (walk) ForestSprites.hero2 else ForestSprites.hero, hx, hy + unit * 0.1f, unit * 1.1f, flip = e.facingX < 0f, alpha = if (blink) 0.4f else 1f)
    // Can cubugu
    val bw = unit * 0.9f
    drawRect(Color.Black.copy(alpha = 0.6f), Offset(hx - bw / 2, hy + unit * 0.25f), Size(bw, unit * 0.1f))
    drawRect(HpRed, Offset(hx - bw / 2, hy + unit * 0.25f), Size(bw * (e.hp / e.maxHp).coerceIn(0f, 1f), unit * 0.1f))

    // Donen bicaklar
    for ((bx, by) in e.bladePositions()) {
        val c = Offset(sx(bx), sy(by))
        rotate(e.bladeAngle * 180f / PI.toFloat() * 3f, c) {
            val r = unit * 0.32f
            val path = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.35f, c.y)
                lineTo(c.x, c.y + r)
                lineTo(c.x - r * 0.35f, c.y)
                close()
            }
            drawPath(path, Color(0xFFD8DEE9))
            drawPath(path, Color(0xFF5D6470), style = Stroke(1.5.dp.toPx()))
        }
    }

    // Mermiler
    for (p in e.projectiles) {
        val c = Offset(sx(p.x), sy(p.y))
        when (p.kind) {
            ProjectileKind.BOLT -> {
                drawCircle(Color(0xFF6FE0FF).copy(alpha = 0.35f), unit * 0.28f, c)
                drawCircle(Color(0xFFD8F5FF), unit * 0.13f, c)
            }
            ProjectileKind.ARROW -> {
                val len = unit * 0.5f
                val n = kotlin.math.hypot(p.vx, p.vy)
                val tail = Offset(c.x - p.vx / n * len, c.y - p.vy / n * len)
                drawLine(Color(0xFF8B5A2B), tail, c, 3.dp.toPx())
                drawCircle(Color(0xFFE5E7EB), 2.5.dp.toPx(), c)
            }
            ProjectileKind.SEED -> Unit
        }
    }
    for (s in e.enemyShots) {
        val c = Offset(sx(s.x), sy(s.y))
        drawCircle(Color(0xFF5A3A1C), unit * 0.2f, c)
        drawCircle(Color(0xFF8FE36B), unit * 0.1f, c)
    }

    // Yildirimlar
    for (s in e.strikes) {
        val x = sx(s.x)
        val y = sy(s.y)
        val a = (s.life / 0.25f).coerceIn(0f, 1f)
        val path = Path().apply {
            moveTo(x + unit * 0.3f, y - unit * 6f)
            lineTo(x - unit * 0.2f, y - unit * 3.5f)
            lineTo(x + unit * 0.25f, y - unit * 3f)
            lineTo(x - unit * 0.15f, y - unit * 1f)
            lineTo(x, y)
        }
        drawPath(path, Color(0xFFF7D046).copy(alpha = a), style = Stroke(4.dp.toPx()))
        drawCircle(Color(0xFFFFF3B0).copy(alpha = a * 0.6f), unit * 0.6f, Offset(x, y))
    }

    // Hasar yazilari
    drawIntoCanvas { canvas ->
        for (t in e.texts) {
            paint.color = (if (t.crit) Color(0xFFFFD84D) else Color.White).copy(alpha = (t.life / 0.7f).coerceIn(0f, 1f)).toArgb()
            paint.textSize = if (t.crit) 18.sp.toPx() else 13.sp.toPx()
            paint.setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
            canvas.nativeCanvas.drawText(t.value.toString(), sx(t.x), sy(t.y), paint)
        }
    }

    if (e.skillFlash > 0f) drawRect(Color(0xFFB8FFB0).copy(alpha = e.skillFlash * 0.8f))
}

/** Piksel art'i keskin ciz: (cx, bottom) alt-orta noktasi, [height] piksel cinsinden yukseklik. */
private fun DrawScope.sprite(
    img: ImageBitmap,
    cx: Float,
    bottom: Float,
    height: Float,
    flip: Boolean = false,
    alpha: Float = 1f,
    tint: Color? = null,
    tintAlpha: Float = 1f,
) {
    val h = height
    val w = height * img.width / img.height
    val left = (cx - w / 2f).roundToInt()
    val top = (bottom - h).roundToInt()
    val filter = tint?.let { ColorFilter.tint(it.copy(alpha = tintAlpha), BlendMode.SrcAtop) }
    val draw: DrawScope.() -> Unit = {
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(max(1, w.roundToInt()), max(1, h.roundToInt())),
            alpha = alpha,
            colorFilter = filter,
            filterQuality = FilterQuality.None,
        )
    }
    if (flip) withTransform({ scale(-1f, 1f, pivot = Offset(cx, bottom)) }, draw) else draw()
}

// ============================================================ Katmanlar

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun LevelUpOverlay(choices: List<Upgrade>, engine: ForestEngine?, onChoose: (Upgrade) -> Unit) {
    Overlay {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.fg_level_up), color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.fg_choose), color = Color.White, fontSize = 14.sp)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                choices.forEach { u -> UpgradeCard(u, onChoose) }
            }
            engine?.let {
                Text(stringResource(R.string.fg_lv, it.level), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun UpgradeCard(u: Upgrade, onChoose: (Upgrade) -> Unit) {
    val (color, name, desc, level) = when (u) {
        is Upgrade.Weapon -> Quad(Color(u.type.color), stringResource(u.type.nameRes), stringResource(u.type.descRes), u.newLevel)
        is Upgrade.Passive -> Quad(Color(u.type.color), stringResource(u.type.nameRes), stringResource(u.type.descRes), u.newLevel)
        Upgrade.Heal -> Quad(HpRed, stringResource(R.string.fg_heal), stringResource(R.string.fg_heal_desc), 0)
    }
    Column(
        Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(2.dp, color, RoundedCornerShape(16.dp))
            .clickable { onChoose(u) }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(if (u is Upgrade.Passive) CircleShape else RoundedCornerShape(8.dp)).background(color),
                contentAlignment = Alignment.Center,
            ) { Text(name.take(1), color = Color.Black, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
                if (level > 0) {
                    Text(
                        if (level == 1) stringResource(R.string.fg_new) else stringResource(R.string.fg_lv, level),
                        color = if (level == 1) Gold else AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(desc, color = AppColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

private data class Quad(val a: Color, val b: String, val c: String, val d: Int)

@Composable
private fun PauseOverlay(viewModel: ForestViewModel) {
    Overlay {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.fg_paused), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Button(
                onClick = viewModel::resume,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80), contentColor = AppColors.OnPrimary),
                modifier = Modifier.padding(top = 16.dp).width(240.dp).height(50.dp),
            ) { Text(stringResource(R.string.fg_resume), fontWeight = FontWeight.Black) }
            TextButton(onClick = viewModel::quitRun, modifier = Modifier.padding(top = 6.dp)) {
                Text(stringResource(R.string.fg_quit), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    state: ForestUiState,
    onRevive: () -> Unit,
    onGiveUp: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onHub: () -> Unit,
) {
    Overlay {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .padding(horizontal = 28.dp, vertical = 18.dp),
        ) {
            Text(
                stringResource(if (state.victory) R.string.fg_victory else R.string.fg_defeat),
                color = if (state.victory) Color(0xFF4ADE80) else HpRed,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                stringResource(R.string.fg_result, formatTime(state.runTime.toFloat()), state.runKills, state.runLevel, state.runCoins),
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (state.canRevive) {
                Button(
                    onClick = onRevive,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = AppColors.OnPrimary),
                    modifier = Modifier.padding(top = 14.dp).width(280.dp).height(48.dp),
                ) {
                    Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.fg_revive), fontWeight = FontWeight.Black)
                }
                TextButton(onClick = onGiveUp) {
                    Text(stringResource(R.string.fg_give_up), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    stringResource(R.string.game_score) + " " + state.score + (if (state.isNewRecord) "  ·  " + stringResource(R.string.game_new_record) else ""),
                    color = Gold,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (state.goldEarned > 0) {
                    Text(stringResource(R.string.game_gold_earned, state.goldEarned), color = Gold, fontSize = 13.sp)
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val hasNext = state.victory && state.selectedMap.ordinal < ForestMap.entries.lastIndex
                    if (hasNext) {
                        Button(onClick = onNext, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80), contentColor = AppColors.OnPrimary)) {
                            Text(stringResource(R.string.fg_next_map), fontWeight = FontWeight.Black)
                        }
                    }
                    Button(onClick = onRetry, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary)) {
                        Text(stringResource(R.string.fg_retry), fontWeight = FontWeight.Black)
                    }
                    Button(onClick = onHub, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHigh, contentColor = AppColors.TextPrimary)) {
                        Text(stringResource(R.string.fg_to_hub), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
