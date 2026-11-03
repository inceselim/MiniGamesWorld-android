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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
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
                GameContent(viewModel, state)
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
                if (state.questToastTime > 0f && state.questToast != null) {
                    val q = state.questToast!!
                    Text(
                        "✔ " + stringResource(R.string.fg_quest_done, q.reward),
                        color = Color(0xFF4ADE80),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                if (state.evolveToast > 0f) {
                    Text(
                        stringResource(R.string.fg_evolution),
                        color = Gold,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center).padding(bottom = 180.dp),
                    )
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
                Canvas(Modifier.size(40.dp)) { sprite(ForestSprites.hero0, size.width / 2f, size.height * 0.85f, size.height) }
            }
            Text(stringResource(R.string.game_forest_name), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Canvas(Modifier.size(18.dp)) { sprite(ForestSprites.coin, size.width / 2f, size.height, size.height) }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.fg_coins, p.coins), color = Gold, fontWeight = FontWeight.Black)
            }
            Text(
                stringResource(R.string.fg_quests, state.quests.count { state.questDone(it) }, state.quests.size) + " · " + stringResource(state.selectedMap.nameRes),
                color = Gold,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            state.quests.forEach { q ->
                val done = state.questDone(q)
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) "✔" else "•", color = if (done) Color(0xFF4ADE80) else AppColors.TextSecondary, fontWeight = FontWeight.Black, modifier = Modifier.width(16.dp))
                    Text(
                        questText(q, if (done) q.target else 0) +
                            "  +" + q.reward + "🪙",
                        color = if (done) AppColors.TextSecondary else AppColors.TextPrimary,
                        fontSize = 12.sp,
                    )
                }
            }
            Text(stringResource(R.string.fg_how_to), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
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
                sprite(ForestSprites.hero0, size.width / 2f, u * 3.3f, u * 1.2f)
                val enemy = when (m) {
                    ForestMap.EDGE -> ForestSprites.goblin0
                    ForestMap.SWAMP -> ForestSprites.bat0
                    ForestMap.MUSHROOM -> ForestSprites.wolf0
                    ForestMap.VALLEY -> ForestSprites.orc0
                    ForestMap.DARK -> ForestSprites.treant0
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
                m.ordinal in p.cleared -> stringResource(R.string.fg_cleared) + " · " + stringResource(R.string.fg_best, p.bestKills[m.ordinal] ?: 0) +
                    " · ★ " + ForestQuests.forMap(m).count { it.id in p.questsDone } + "/3"
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
private fun GameContent(viewModel: ForestViewModel, state: ForestUiState) {
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
        Hud(viewModel, state)
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
private fun BoxScope.Hud(viewModel: ForestViewModel, state: ForestUiState) {
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
            e.weapons.forEach { (w, l) -> SlotIcon(Color(w.color), l, evolved = w in e.evolved) }
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

    // Sag: gorevler
    val quests = state.quests
    val doneCount = quests.count { state.questDone(it) }
    val current = quests.firstOrNull { !state.questDone(it) }
    Column(
        Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(stringResource(R.string.fg_quests, doneCount, quests.size), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Black)
        if (current != null) {
            Text(questText(current, ForestQuests.progress(current, e)), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
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
private fun SlotIcon(color: Color, level: Int, round: Boolean = false, evolved: Boolean = false) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(24.dp)
            .clip(if (round) CircleShape else RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.85f))
            .border(if (evolved) 2.dp else 0.dp, if (evolved) Gold else Color.Transparent, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (evolved) "★" else level.toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

/** Gorev metni: boss gorevi tek sayi alir, digerleri ilerleme/hedef. */
@Composable
private fun questText(q: Quest, progress: Int): String =
    if (q.type == QuestType.BOSS_FAST) stringResource(q.type.textRes, q.target)
    else stringResource(q.type.textRes, progress.coerceIn(0, q.target), q.target)

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

/** 0..1 arasi yumusak deger gurultusu (zemin lekeleri icin). */
private fun noise(x: Float, y: Float): Float {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val fx = x - x0
    val fy = y - y0
    fun v(a: Int, b: Int) = (hash(a, b) % 1000) / 1000f
    val sx = fx * fx * (3 - 2 * fx)
    val sy = fy * fy * (3 - 2 * fy)
    val top = v(x0, y0) + (v(x0 + 1, y0) - v(x0, y0)) * sx
    val bottom = v(x0, y0 + 1) + (v(x0 + 1, y0 + 1) - v(x0, y0 + 1)) * sx
    return top + (bottom - top) * sy
}

/** Derinlik siralamasi icin cizilecek seyler (y'ye gore). */
private class DrawItem(var y: Float, var kind: Int, var ref: Any?, var x: Float = 0f)

private val drawItems = ArrayList<DrawItem>(256)
private var drawCount = 0
private fun item(y: Float, kind: Int, ref: Any?, x: Float = 0f) {
    if (drawCount < drawItems.size) {
        drawItems[drawCount].apply { this.y = y; this.kind = kind; this.ref = ref; this.x = x }
    } else drawItems += DrawItem(y, kind, ref, x)
    drawCount++
}

private const val K_ENEMY = 0
private const val K_HERO = 1
private const val K_TREE = 2
private const val K_BUSH = 3
private const val K_ROCK = 4

private fun DrawScope.drawWorld(e: ForestEngine, unit: Float, paint: android.graphics.Paint) {
    val viewW = size.width / unit
    val viewH = size.height / unit
    val shakeX = if (e.shake > 0f) sin(e.time * 90f) * e.shake * unit * 0.6f else 0f
    val shakeY = if (e.shake > 0f) cos(e.time * 77f) * e.shake * unit * 0.6f else 0f
    val camX = e.heroX - viewW / 2f - shakeX / unit
    val camY = e.heroY - viewH / 2f - shakeY / unit
    fun sx(x: Float) = (x - camX) * unit
    fun sy(y: Float) = (y - camY) * unit
    val grass = Color(e.map.grass)
    val grassDark = Color(e.map.grassDark)
    val grassLight = lerp(grass, Color(0xFFB5E36B), 0.18f)

    // ---- Zemin: yumusak lekeler + cimen saplari
    val x0 = floor(camX).toInt() - 1
    val y0 = floor(camY).toInt() - 1
    val x1 = x0 + viewW.toInt() + 3
    val y1 = y0 + viewH.toInt() + 3
    for (ty in y0..y1) {
        for (tx in x0..x1) {
            val n = noise(tx / 7f, ty / 7f)
            val c = when {
                n < 0.35f -> grassDark
                n > 0.68f -> grassLight
                else -> grass
            }
            drawRect(c, Offset(sx(tx.toFloat()), sy(ty.toFloat())), Size(unit + 1f, unit + 1f))
        }
    }
    val blade = 2f.coerceAtLeast(unit / 16f)
    for (ty in y0..y1) {
        for (tx in x0..x1) {
            val h = hash(tx, ty)
            val px = sx(tx.toFloat())
            val py = sy(ty.toFloat())
            if (h % 3 == 0) {
                // 3 cimen sapi
                val bx = px + (h % 7) * unit / 8f
                val by = py + ((h / 7) % 6) * unit / 8f + unit * 0.4f
                drawRect(grassDark, Offset(bx, by - blade * 3), Size(blade, blade * 3))
                drawRect(grassLight, Offset(bx + blade * 2, by - blade * 4), Size(blade, blade * 4))
                drawRect(grassDark, Offset(bx + blade * 4, by - blade * 2), Size(blade, blade * 2))
            }
            when {
                h % 29 == 4 -> sprite(ForestSprites.flowerY, px + unit * 0.5f, py + unit * 0.7f, unit * 0.45f)
                h % 31 == 6 -> sprite(ForestSprites.flowerP, px + unit * 0.5f, py + unit * 0.7f, unit * 0.45f)
                h % 47 == 8 -> sprite(ForestSprites.mushroom, px + unit * 0.5f, py + unit * 0.8f, unit * 0.55f)
            }
        }
    }

    // ---- Zemine yakin efektler: diken halkasi, buz halkalari, toplanabilirler
    e.weapons[WeaponType.THORNS]?.let { l ->
        val r = e.thornRadius(l) * unit
        val c = Offset(sx(e.heroX), sy(e.heroY))
        drawCircle(Brush.radialGradient(listOf(Color(0x008FE36B), Color(0x448FE36B)), c, r), r, c)
        drawCircle(Color(0xFF8FE36B).copy(alpha = 0.55f), r, c, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), e.time * 30f)))
    }
    for (n in e.novas) {
        val k = 1f - n.life / 0.45f
        val r = n.maxRadius * k * unit
        val c = Offset(sx(n.x), sy(n.y))
        drawCircle(Color(0xFFB8F0FF).copy(alpha = 0.25f * (1f - k)), r, c)
        drawCircle(Color(0xFFE6FBFF).copy(alpha = 0.9f * (1f - k)), r, c, style = Stroke(6.dp.toPx()))
    }
    for (p in e.pickups) {
        val x = sx(p.x)
        val y = sy(p.y)
        if (x < -unit || x > size.width + unit || y < -unit || y > size.height + unit) continue
        val bob = sin(e.time * 4f + p.x * 3f) * unit * 0.06f
        drawOval(Color.Black.copy(alpha = 0.2f), Offset(x - unit * 0.18f, y + unit * 0.12f), Size(unit * 0.36f, unit * 0.1f))
        when (p.kind) {
            PickupKind.GEM -> sprite(ForestSprites.gem, x, y + unit * 0.12f + bob, unit * 0.42f)
            PickupKind.BIG_GEM -> {
                drawCircle(Color(0x335AC8FF), unit * 0.4f, Offset(x, y - unit * 0.1f + bob))
                sprite(ForestSprites.gem2, x, y + unit * 0.15f + bob, unit * 0.55f)
            }
            PickupKind.COIN -> {
                // Donen sikke: yatayda daralip genisler
                val k = kotlin.math.abs(cos(e.time * 5f + p.x))
                withTransform({ scale(0.25f + 0.75f * k, 1f, pivot = Offset(x, y)) }) {
                    sprite(ForestSprites.coin, x, y + unit * 0.12f + bob, unit * 0.42f)
                }
            }
            PickupKind.MEAT -> sprite(ForestSprites.meat, x, y + unit * 0.15f + bob, unit * 0.5f)
            PickupKind.CHEST -> {
                drawCircle(Brush.radialGradient(listOf(Color(0x88FFE680), Color(0x00FFE680)), Offset(x, y), unit * 1.6f), unit * 1.6f, Offset(x, y))
                sprite(ForestSprites.chest, x, y + unit * 0.4f, unit * 1.1f)
            }
        }
    }

    // ---- Derinlige gore sirali: agaclar, calilar, kayalar, dusmanlar, kahraman
    drawCount = 0
    for (ty in (y0 - 2)..(y1 + 2)) {
        for (tx in (x0 - 2)..(x1 + 2)) {
            val h = hash(tx * 3 + 1, ty * 5 + 2)
            when {
                h % 61 == 3 -> item(ty + 1f, K_TREE, null, tx + 0.5f)
                h % 43 == 5 -> item(ty + 1f, K_BUSH, null, tx + 0.5f)
                h % 53 == 7 -> item(ty + 1f, K_ROCK, null, tx + 0.5f)
            }
        }
    }
    for (en in e.enemies) {
        val x = sx(en.x)
        val y = sy(en.y)
        val s = en.type.drawSize * unit * 1.35f
        if (x < -s || x > size.width + s || y < -s || y > size.height + s) continue
        item(en.y, K_ENEMY, en)
    }
    item(e.heroY, K_HERO, null)
    drawItems.subList(0, drawCount).sortBy { it.y }

    for (k in 0 until drawCount) {
        val it = drawItems[k]
        when (it.kind) {
            K_TREE -> {
                val x = sx(it.x)
                val y = sy(it.y)
                drawOval(Color.Black.copy(alpha = 0.22f), Offset(x - unit * 1.2f, y - unit * 0.25f), Size(unit * 2.4f, unit * 0.6f))
                // Kahraman agacin arkasindaysa agac yari saydam
                val behind = kotlin.math.abs(e.heroX - it.x) < 1.3f && e.heroY < it.y && e.heroY > it.y - 3f
                sprite(ForestSprites.tree, x, y, unit * 3.2f, alpha = if (behind) 0.55f else 1f)
            }
            K_BUSH -> sprite(ForestSprites.bush, sx(it.x), sy(it.y), unit * 0.8f)
            K_ROCK -> sprite(ForestSprites.rock, sx(it.x), sy(it.y), unit * 0.6f)
            K_ENEMY -> drawEnemy(e, it.ref as Enemy, unit, ::sx, ::sy)
            K_HERO -> drawHero(e, unit, sx(e.heroX), sy(e.heroY))
        }
    }

    // ---- Donen bicaklar
    for ((bx, by) in e.bladePositions()) {
        val c = Offset(sx(bx), sy(by) - unit * 0.3f)
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)), c, unit * 0.45f), unit * 0.45f, c)
        rotate(e.bladeAngle * 180f / PI.toFloat() * 3f, c) {
            val r = unit * 0.34f
            val path = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.32f, c.y)
                lineTo(c.x, c.y + r)
                lineTo(c.x - r * 0.32f, c.y)
                close()
            }
            drawPath(path, Color(0xFFE8EDF5))
            drawPath(path, Color(0xFF4B5563), style = Stroke(1.5.dp.toPx()))
            drawLine(Color.White, Offset(c.x, c.y - r * 0.8f), Offset(c.x, c.y + r * 0.2f), 1.dp.toPx())
        }
    }

    // ---- Mermiler (parlayan)
    for (p in e.projectiles) {
        val c = Offset(sx(p.x), sy(p.y) - unit * 0.3f)
        when (p.kind) {
            ProjectileKind.BOLT -> {
                val n = kotlin.math.hypot(p.vx, p.vy)
                for (t in 1..3) {
                    val tc = Offset(c.x - p.vx / n * unit * 0.14f * t, c.y - p.vy / n * unit * 0.14f * t)
                    drawCircle(Color(0xFF6FE0FF).copy(alpha = 0.35f / t), unit * (0.18f - 0.03f * t), tc)
                }
                drawCircle(Brush.radialGradient(listOf(Color(0xAA6FE0FF), Color(0x006FE0FF)), c, unit * 0.45f), unit * 0.45f, c)
                drawCircle(Color(0xFFE6FBFF), unit * 0.11f, c)
            }
            ProjectileKind.LANCE -> {
                val n = kotlin.math.hypot(p.vx, p.vy)
                val ux = p.vx / n
                val uy = p.vy / n
                val tail = Offset(c.x - ux * unit * 1.1f, c.y - uy * unit * 1.1f)
                drawLine(Color(0x556FE0FF), tail, c, unit * 0.5f)
                drawLine(Color(0xFFB8F4FF), tail, c, unit * 0.18f)
                drawCircle(Brush.radialGradient(listOf(Color(0xCCFFFFFF), Color(0x006FE0FF)), c, unit * 0.6f), unit * 0.6f, c)
            }
            ProjectileKind.ARROW -> {
                val len = unit * 0.55f
                val n = kotlin.math.hypot(p.vx, p.vy)
                val ux = p.vx / n
                val uy = p.vy / n
                val tail = Offset(c.x - ux * len, c.y - uy * len)
                drawLine(Color(0xFF8B5A2B), tail, c, 3.dp.toPx())
                drawLine(Color(0xFFF2F2F2), tail, Offset(tail.x + ux * unit * 0.12f - uy * unit * 0.08f, tail.y + uy * unit * 0.12f + ux * unit * 0.08f), 2.dp.toPx())
                drawLine(Color(0xFFF2F2F2), tail, Offset(tail.x + ux * unit * 0.12f + uy * unit * 0.08f, tail.y + uy * unit * 0.12f - ux * unit * 0.08f), 2.dp.toPx())
                val head = Path().apply {
                    moveTo(c.x + ux * unit * 0.12f, c.y + uy * unit * 0.12f)
                    lineTo(c.x - uy * unit * 0.07f, c.y + ux * unit * 0.07f)
                    lineTo(c.x + uy * unit * 0.07f, c.y - ux * unit * 0.07f)
                    close()
                }
                drawPath(head, Color(0xFFD1D5DB))
            }
            ProjectileKind.SEED -> Unit
        }
    }
    for (s in e.enemyShots) {
        val c = Offset(sx(s.x), sy(s.y) - unit * 0.2f)
        drawCircle(Brush.radialGradient(listOf(Color(0x88B5FF6B), Color(0x00B5FF6B)), c, unit * 0.4f), unit * 0.4f, c)
        drawCircle(Color(0xFF5A3A1C), unit * 0.17f, c)
        drawCircle(Color(0xFF8FE36B), unit * 0.08f, Offset(c.x - unit * 0.04f, c.y - unit * 0.04f))
    }

    // ---- Yildirimlar
    for (s in e.strikes) {
        val x = sx(s.x)
        val y = sy(s.y)
        val a = (s.life / 0.25f).coerceIn(0f, 1f)
        val path = Path().apply {
            moveTo(x + unit * 0.3f, y - unit * 7f)
            lineTo(x - unit * 0.25f, y - unit * 4.5f)
            lineTo(x + unit * 0.3f, y - unit * 3.6f)
            lineTo(x - unit * 0.2f, y - unit * 1.4f)
            lineTo(x, y)
        }
        drawPath(path, Color(0xFFF7D046).copy(alpha = a * 0.6f), style = Stroke(9.dp.toPx()))
        drawPath(path, Color(0xFFFFFDE8).copy(alpha = a), style = Stroke(3.dp.toPx()))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0).copy(alpha = a), Color(0x00FFF3B0)), Offset(x, y), unit * 1.1f), unit * 1.1f, Offset(x, y))
    }

    // ---- Parcaciklar
    for (p in e.particles) {
        val a = (p.life / p.maxLife).coerceIn(0f, 1f)
        val sz = p.size * unit * (0.5f + 0.5f * a)
        drawRect(Color(p.color).copy(alpha = a), Offset(sx(p.x) - sz / 2, sy(p.y) - sz / 2), Size(sz, sz))
    }

    // ---- Atesbocekleri (ekran uzayinda)
    val dark = e.map == ForestMap.DARK || e.map == ForestMap.SWAMP
    val flies = if (dark) 18 else 8
    for (i in 0 until flies) {
        val fx = ((hash(i, 7) % 1000) / 1000f * size.width + sin(e.time * 0.6f + i) * unit * 1.5f - e.heroX * unit * 0.15f).mod(size.width)
        val fy = ((hash(i, 13) % 1000) / 1000f * size.height + cos(e.time * 0.5f + i * 2f) * unit - e.heroY * unit * 0.15f).mod(size.height)
        val tw = 0.4f + 0.6f * kotlin.math.abs(sin(e.time * 2f + i))
        drawCircle(Brush.radialGradient(listOf(Color(0xAAFFF59D).copy(alpha = 0.6f * tw), Color(0x00FFF59D)), Offset(fx, fy), unit * 0.3f), unit * 0.3f, Offset(fx, fy))
        drawCircle(Color(0xFFFFFDE0).copy(alpha = tw), unit * 0.05f, Offset(fx, fy))
    }

    // ---- Hasar yazilari (dis hatli)
    drawIntoCanvas { canvas ->
        for (t in e.texts) {
            val alpha = (t.life / 0.7f).coerceIn(0f, 1f)
            paint.textSize = if (t.crit) 19.sp.toPx() else 13.sp.toPx()
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 3.dp.toPx()
            paint.color = Color(0xFF1C182A).copy(alpha = alpha).toArgb()
            canvas.nativeCanvas.drawText(t.value.toString(), sx(t.x), sy(t.y), paint)
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = (if (t.crit) Color(0xFFFFD84D) else Color.White).copy(alpha = alpha).toArgb()
            canvas.nativeCanvas.drawText(t.value.toString(), sx(t.x), sy(t.y), paint)
        }
    }

    // ---- Isik: kenar karartmasi + ozel yetenek parlamasi
    val center = Offset(size.width / 2f, size.height / 2f)
    val vignette = if (e.map == ForestMap.DARK) 0.75f else 0.4f
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = vignette)), center, size.width * 0.62f))
    if (e.hp / e.maxHp < 0.3f) {
        val pulse = 0.25f + 0.15f * sin(e.time * 6f)
        drawRect(Brush.radialGradient(listOf(Color.Transparent, HpRed.copy(alpha = pulse)), center, size.width * 0.6f))
    }
    if (e.skillFlash > 0f) drawRect(Color(0xFFB8FFB0).copy(alpha = e.skillFlash * 0.8f))
}

private fun DrawScope.drawEnemy(e: ForestEngine, en: Enemy, unit: Float, sx: (Float) -> Float, sy: (Float) -> Float) {
    val x = sx(en.x)
    val y = sy(en.y)
    val s = en.type.drawSize * unit * 1.35f
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(x - s * 0.28f, y - s * 0.05f), Size(s * 0.56f, s * 0.16f))
    val frame = ((e.time * 6f + en.id * 0.37f).toInt() % 2) == 0
    val bob = if (en.type == EnemyType.BAT) sin(e.time * 12f + en.id) * unit * 0.15f - unit * 0.3f else 0f
    val img = when (en.type) {
        EnemyType.GOBLIN -> if (frame) ForestSprites.goblin0 else ForestSprites.goblin1
        EnemyType.BAT -> if (frame) ForestSprites.bat0 else ForestSprites.bat1
        EnemyType.WOLF -> if (frame) ForestSprites.wolf0 else ForestSprites.wolf1
        EnemyType.ORC -> if (frame) ForestSprites.orc0 else ForestSprites.orc1
        EnemyType.TREANT -> if (frame) ForestSprites.treant0 else ForestSprites.treant1
    }
    // Kurt cizimi sola bakiyor; digerleri saga
    val flip = if (en.type == EnemyType.WOLF) !en.facingLeft else en.facingLeft
    val tint = when {
        en.flash > 0f -> Color.White
        en.frozen > 0f -> Color(0xFFCFF6FF)
        en.slow > 0f -> Color(0xFF9BE7FF)
        else -> null
    }
    if (en.type.isBoss) {
        drawCircle(Brush.radialGradient(listOf(Color(0x558FE36B), Color(0x008FE36B)), Offset(x, y - s * 0.4f), s * 0.8f), s * 0.8f, Offset(x, y - s * 0.4f))
        if (en.charging > 0f) drawCircle(HpRed.copy(alpha = 0.35f), s * 0.55f, Offset(x, y - s * 0.4f))
    }
    sprite(img, x, y + s * 0.08f + bob, s, flip = flip, tint = tint, tintAlpha = if (en.slow > 0f && en.flash <= 0f) 0.45f else 1f)
    // Yarali dusmanlarda kucuk can cubugu
    if (!en.type.isBoss && en.hp < en.maxHp && en.type == EnemyType.ORC) {
        val bw = s * 0.5f
        drawRect(Color.Black.copy(alpha = 0.5f), Offset(x - bw / 2, y - s * 0.95f), Size(bw, unit * 0.07f))
        drawRect(HpRed, Offset(x - bw / 2, y - s * 0.95f), Size(bw * (en.hp / en.maxHp).coerceIn(0f, 1f), unit * 0.07f))
    }
}

private fun DrawScope.drawHero(e: ForestEngine, unit: Float, hx: Float, hy: Float) {
    val h = unit * 1.45f
    // Kahramanin etrafinda yumusak isik
    drawCircle(Brush.radialGradient(listOf(Color(0x33FFF8E1), Color(0x00FFF8E1)), Offset(hx, hy - unit * 0.4f), unit * 2.2f), unit * 2.2f, Offset(hx, hy - unit * 0.4f))
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(hx - unit * 0.32f, hy - unit * 0.05f), Size(unit * 0.64f, unit * 0.18f))
    val walk = e.moving && ((e.time * 7f).toInt() % 2 == 0)
    val blink = e.iframe > 0f && ((e.time * 20f).toInt() % 2 == 0) && e.dashTime <= 0f
    if (e.dashTime > 0f) {
        for (k in 1..3) {
            sprite(ForestSprites.hero0, hx - e.facingX * unit * 0.35f * k, hy - e.facingY * unit * 0.35f * k + unit * 0.05f, h, flip = e.facingX < 0f, alpha = 0.3f / k, tint = Color(0xFF8FE36B))
        }
    }
    // Asanin ucundaki tas parlar
    val staffX = if (e.facingX < 0f) hx - h * 0.25f else hx + h * 0.25f
    drawCircle(Brush.radialGradient(listOf(Color(0x996FE0FF), Color(0x006FE0FF)), Offset(staffX, hy - h * 0.85f), unit * 0.35f), unit * 0.35f, Offset(staffX, hy - h * 0.85f))
    sprite(if (walk) ForestSprites.hero1 else ForestSprites.hero0, hx, hy + unit * 0.05f, h, flip = e.facingX < 0f, alpha = if (blink) 0.4f else 1f)
    // Can ve MP cubuklari
    val bw = unit * 0.9f
    val by = hy + unit * 0.2f
    drawRect(Color(0xFF1C182A), Offset(hx - bw / 2 - 1, by - 1), Size(bw + 2, unit * 0.16f + 2))
    drawRect(HpRed, Offset(hx - bw / 2, by), Size(bw * (e.hp / e.maxHp).coerceIn(0f, 1f), unit * 0.09f))
    drawRect(MpBlue, Offset(hx - bw / 2, by + unit * 0.1f), Size(bw * (e.mp / e.maxMp).coerceIn(0f, 1f), unit * 0.06f))
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
        is Upgrade.Evolve -> Quad(Gold, stringResource(u.type.evolvedNameRes), stringResource(u.type.evolvedDescRes), -1)
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
                if (level == -1) {
                    Text(stringResource(R.string.fg_evolution), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
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
            Text(stringResource(R.string.fg_evolutions), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
            WeaponType.entries.forEach { w ->
                val pas = ForestEngine.evolutionPassive(w)
                Text(
                    stringResource(w.nameRes) + " + " + stringResource(pas.nameRes) + " → " + stringResource(w.evolvedNameRes),
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                )
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
            if (state.runQuestCoins > 0) {
                Text("✔ " + stringResource(R.string.fg_quest_done, state.runQuestCoins), color = Color(0xFF4ADE80), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
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
