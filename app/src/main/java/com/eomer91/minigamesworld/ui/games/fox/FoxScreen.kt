package com.eomer91.minigamesworld.ui.games.fox

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
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.eomer91.minigamesworld.feedback.GameMusicEffect
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ekran yuksekligine sigan karo sayisi. */
private const val VIEW_ROWS = 11f
private val Gold = Color(0xFFF7D046)
private val Green = Color(0xFF4ADE80)
private val Red = Color(0xFFE04848)

@Composable
fun FoxScreen(onBack: () -> Unit, viewModel: FoxViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LandscapeImmersive()
    GameMusicEffect(R.raw.music_fox, playing = state.phase == FoxPhase.PLAYING)

    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

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
            FoxPhase.PLAYING -> viewModel.pause()
            FoxPhase.PAUSED -> viewModel.resume()
            FoxPhase.DEAD, FoxPhase.WON -> viewModel.toHub()
            FoxPhase.HUB, FoxPhase.LOADING -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
        when (state.phase) {
            FoxPhase.LOADING -> Unit
            FoxPhase.HUB -> HubContent(state, viewModel, onBack, onPlay = { w, l -> showInterstitial { viewModel.start(w, l) } })
            else -> {
                GameContent(viewModel, state)
                when (state.phase) {
                    FoxPhase.PAUSED -> PauseOverlay(viewModel)
                    FoxPhase.DEAD -> DeadOverlay(
                        state = state,
                        hasCheckpoint = (viewModel.engine?.checkpointCol ?: -1) >= 0,
                        onRevive = { showRewarded({ viewModel.reviveHere() }, adNotReady) },
                        onCheckpoint = viewModel::continueFromCheckpoint,
                        onHub = viewModel::toHub,
                    )
                    FoxPhase.WON -> WonOverlay(
                        state = state,
                        onNext = { showInterstitial { viewModel.next() } },
                        onRetry = { showInterstitial { viewModel.retry() } },
                        onHub = viewModel::toHub,
                    )
                    else -> Unit
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

// ============================================================ Hub (bolum secimi)

@Composable
private fun HubContent(state: FoxUiState, viewModel: FoxViewModel, onBack: () -> Unit, onPlay: (Int, Int) -> Unit) {
    val p = state.profile
    val maxStars = FoxEngine.WORLDS * FoxEngine.LEVELS_PER_WORLD * 3
    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(Modifier.width(230.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(AppColors.Surface)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
                }
                Spacer(Modifier.width(10.dp))
                SpriteIcon(FoxSprites.fox0, 44.dp)
            }
            Text(stringResource(R.string.game_fox_name), color = AppColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SpriteIcon(FoxSprites.star, 20.dp)
                Text(" ${p.totalStars} / $maxStars", color = Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Text(stringResource(R.string.fx_how_to), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            if (p.isCleared(FoxEngine.WORLDS - 1, FoxEngine.LEVELS_PER_WORLD - 1)) {
                Text(stringResource(R.string.fx_all_done), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // Dunya sekmeleri
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoxWorld.entries.forEachIndexed { i, w ->
                    val locked = !p.isUnlocked(i, 0)
                    val selected = state.world == i
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(w.hillNear) else AppColors.Surface)
                            .clickable(enabled = !locked) { viewModel.selectWorld(i) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (locked) Icon(Icons.Rounded.Lock, null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                        Text(
                            "${i + 1}. " + stringResource(w.nameRes),
                            color = if (locked) AppColors.TextSecondary else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
            // Bolum kartlari
            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (l in 0 until FoxEngine.LEVELS_PER_WORLD) {
                    LevelCard(state.world, l, p, Modifier.weight(1f).fillMaxHeight(), onClick = { onPlay(state.world, l) })
                }
            }
        }
    }
}

@Composable
private fun LevelCard(world: Int, level: Int, p: FoxProfile, modifier: Modifier, onClick: () -> Unit) {
    val locked = !p.isUnlocked(world, level)
    val theme = FoxWorld.of(world)
    val stars = p.starsOf(world, level)
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(theme.skyTop), Color(theme.hillNear))))
            .border(2.dp, if (p.isCleared(world, level)) Green else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(enabled = !locked, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${world + 1}-${level + 1}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
        if (locked) {
            Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.fx_locked), tint = Color.White, modifier = Modifier.size(28.dp))
        } else {
            Row {
                repeat(3) { i -> SpriteIcon(FoxSprites.star, 22.dp, alpha = if (i < stars) 1f else 0.25f) }
            }
        }
        Text(
            p.timeOf(world, level)?.let { stringResource(R.string.fx_best_time, formatTime(it.toFloat())) } ?: " ",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpriteIcon(img: ImageBitmap, size: Dp, alpha: Float = 1f) {
    Canvas(Modifier.size(size)) { sprite(img, this.size.width / 2f, this.size.height, this.size.height, alpha = alpha) }
}

// ============================================================ Oyun ekrani

private class Camera {
    var x = 0f
    var y = -1f
}

@Composable
private fun GameContent(viewModel: FoxViewModel, state: FoxUiState) {
    val cam = remember(state.world, state.level) { Camera() }
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val f = viewModel.frame // her karede yeniden ciz
            val e = viewModel.engine ?: return@Canvas
            drawLevel(e, cam, size.height / VIEW_ROWS)
        }
        Hud(viewModel, state)
        Controls(viewModel)
    }
}

@Composable
private fun BoxScope.Hud(viewModel: FoxViewModel, state: FoxUiState) {
    @Suppress("UNUSED_VARIABLE")
    val f = viewModel.frame
    val e = viewModel.engine ?: return
    Row(
        Modifier
            .align(Alignment.TopStart)
            .padding(start = 16.dp, top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(FoxEngine.MAX_HEARTS) { i -> SpriteIcon(FoxSprites.heart, 22.dp, alpha = if (i < e.hearts) 1f else 0.25f) }
        Spacer(Modifier.width(12.dp))
        SpriteIcon(FoxSprites.coin, 20.dp)
        Text(" ${e.coins}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.width(12.dp))
        repeat(e.totalStars) { i -> SpriteIcon(FoxSprites.star, 20.dp, alpha = if (i < e.stars) 1f else 0.25f) }
        Spacer(Modifier.width(12.dp))
        Text(formatTime(e.time), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
    Text(
        stringResource(R.string.fx_level, state.world + 1, state.level + 1),
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
    )
    IconButton(
        onClick = viewModel::pause,
        modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
    ) {
        Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.fx_paused), tint = Color.White)
    }
}

/** Sol altta yon pedi (parmak kaydirilabilir), sag altta ziplama. */
@Composable
private fun BoxScope.Controls(viewModel: FoxViewModel) {
    Row(
        Modifier
            .align(Alignment.BottomStart)
            .padding(start = 20.dp, bottom = 18.dp)
            .size(width = 210.dp, height = 96.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        var l = false
                        var r = false
                        ev.changes.forEach { c ->
                            if (c.pressed) {
                                if (c.position.x < size.width / 2f) l = true else r = true
                            }
                            c.consume()
                        }
                        viewModel.setLeft(l && !r)
                        viewModel.setRight(r && !l)
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PadButton("◀", Modifier.weight(1f))
        PadButton("▶", Modifier.weight(1f))
    }
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 28.dp, bottom = 20.dp)
            .size(96.dp)
            .clip(CircleShape)
            .background(Color(0xFFF59E0B).copy(alpha = 0.55f))
            .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    viewModel.jumpDown()
                    waitForUpOrCancellation()
                    viewModel.jumpUp()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("▲", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PadButton(label: String, modifier: Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .border(2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
    }
}

private fun formatTime(t: Float): String {
    val s = t.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

// ============================================================ Cizim

private fun hash(x: Int, y: Int): Int = ((x * 73856093) xor (y * 19349663) xor 0x5bd1e995).let { (it xor (it ushr 13)) and 0x7fffffff }

private fun DrawScope.drawLevel(e: FoxEngine, cam: Camera, unit: Float) {
    val viewW = size.width / unit
    val theme = FoxWorld.of(e.world)

    // Kamera: yatayda oyuncunun biraz onunu gosterir, dikeyde yumusak takip
    val targetX = (e.px + FoxEngine.PW / 2f - viewW * 0.4f).coerceIn(0f, max(0f, e.width - viewW))
    val targetY = (e.py - VIEW_ROWS * 0.55f).coerceIn(0f, max(0f, e.height - VIEW_ROWS))
    cam.x = targetX
    cam.y = if (cam.y < 0f) targetY else cam.y + (targetY - cam.y) * 0.12f
    val camX = cam.x
    val camY = cam.y
    fun sx(x: Float) = (x - camX) * unit
    fun sy(y: Float) = (y - camY) * unit

    drawBackground(theme, camX, unit)

    val c0 = floor(camX).toInt() - 1
    val c1 = ceil(camX + viewW).toInt() + 1
    val t = e.time

    // Arka plan susleri (carpismasiz)
    for (c in c0..c1) {
        if (c < 0 || c >= e.width) continue
        val surface = (1 until e.height).firstOrNull { r -> e.tile(c, r) == '#' && e.tile(c, r - 1) == '.' } ?: continue
        if (surface < 6) continue
        val h = hash(c, e.world) % 29
        val bottom = sy(surface.toFloat())
        val cx = sx(c + 0.5f)
        when (e.world) {
            0 -> when (h) {
                0, 1 -> sprite(FoxSprites.tree, cx, bottom, unit * 3.4f)
                5, 6, 7 -> sprite(FoxSprites.bush, cx, bottom, unit * 1.1f)
                11 -> sprite(FoxSprites.mushroom, cx, bottom, unit * 0.7f)
                15 -> sprite(FoxSprites.rock, cx, bottom, unit * 0.8f)
            }
            1 -> when (h) {
                0, 4, 9 -> sprite(FoxSprites.mushroom, cx, bottom, unit * 0.8f)
                2, 13 -> sprite(FoxSprites.rock, cx, bottom, unit * 1f)
            }
            else -> when (h) {
                3, 17 -> sprite(FoxSprites.rock, cx, bottom, unit * 0.9f)
            }
        }
    }

    // Karolar
    val r0 = max(0, floor(camY).toInt())
    val r1 = minOf(e.height - 1, ceil(camY + VIEW_ROWS).toInt())
    for (r in r0..r1) for (c in c0..c1) {
        if (c < 0 || c >= e.width) continue
        val ch = e.tiles[r][c]
        if (ch == '.') continue
        val left = sx(c.toFloat())
        val top = sy(r.toFloat())
        val bump = (e.bumps[r * e.width + c] ?: 0f).coerceAtLeast(0f)
        val bumpY = -sin(bump / 0.15f * Math.PI.toFloat()) * unit * 0.25f
        when (ch) {
            '#' -> {
                val above = e.tile(c, r - 1)
                val img = when (e.world) {
                    0 -> if (above == '#') FoxSprites.tDirt else FoxSprites.tGrass
                    1 -> if (above == '#' || r == 0) FoxSprites.tCave else FoxSprites.tCaveTop
                    else -> FoxSprites.tCastle
                }
                tile(img, left, top, unit)
            }
            'B' -> tile(FoxSprites.tBrick, left, top, unit)
            '?', 'H' -> tile(FoxSprites.tQ, left, top + bumpY, unit)
            'U' -> tile(FoxSprites.tUsed, left, top + bumpY, unit)
            '=' -> tile(FoxSprites.tPlat, left, top, unit)
            '^' -> tile(FoxSprites.tSpikes, left, top, unit)
            'S' -> tile(FoxSprites.tSpring, left, top, unit)
            'L' -> {
                tile(FoxSprites.tLava, left, top + sin(t * 2f + c) * unit * 0.05f, unit)
                if (e.tile(c, r - 1) != 'L') drawRect(Color(0x55FF8A2A), Offset(left, top - unit * 0.6f), Size(unit + 1f, unit * 0.6f))
            }
            'c' -> {
                val spin = abs(cos(t * 4f + c * 0.7f))
                val w = unit * 0.7f * max(0.15f, spin)
                val img = FoxSprites.coin
                drawImage(
                    img, IntOffset.Zero, IntSize(img.width, img.height),
                    IntOffset((left + unit / 2f - w / 2f).roundToInt(), (top + unit * 0.15f).roundToInt()),
                    IntSize(max(1, w.roundToInt()), (unit * 0.7f).roundToInt()),
                    filterQuality = FilterQuality.None,
                )
            }
            '*' -> {
                val bob = sin(t * 3f + c) * unit * 0.1f
                drawCircle(Color(0x44FFE680), unit * 0.65f, Offset(left + unit / 2f, top + unit / 2f + bob))
                sprite(FoxSprites.star, left + unit / 2f, top + unit * 0.95f + bob, unit * 0.9f)
            }
            'K' -> sprite(if (e.checkpointCol == c) FoxSprites.checkOn else FoxSprites.checkOff, left + unit / 2f, top + unit, unit * 2f)
            'F' -> {
                val ground = (r + 1 until e.height).firstOrNull { rr -> e.tile(c, rr) == '#' } ?: (r + 3)
                sprite(FoxSprites.flagpole, left + unit / 2f, sy(ground.toFloat()), unit * (ground - r + 0.5f).coerceAtLeast(3f))
            }
        }
    }

    // Bloklardan cikan sikkeler
    e.coinPops.forEach { p -> sprite(FoxSprites.coin, sx(p[0]), sy(p[1]) + unit * 0.35f, unit * 0.7f, alpha = (p[2] / 0.5f).coerceIn(0f, 1f)) }

    // Dusmanlar
    e.enemies.forEach { en ->
        if (!en.alive && en.squash <= 0f) return@forEach
        val cx = sx(en.x + en.type.w / 2f)
        val bottom = sy(en.y + en.type.h)
        val anim = ((en.t * if (en.type.flying) 10f else 4f).toInt() % 2) == 0
        val img = when (en.type) {
            FoxEnemyType.SNAIL -> if (anim) FoxSprites.snail0 else FoxSprites.snail1
            FoxEnemyType.BEE -> if (anim) FoxSprites.bee0 else FoxSprites.bee1
            FoxEnemyType.HEDGEHOG -> if (anim) FoxSprites.hedgehog0 else FoxSprites.hedgehog1
        }
        val h = unit * 1.25f
        if (!en.alive) {
            // Ezilme: yassi ve soluk
            withTransform({ scale(1.2f, 0.4f, pivot = Offset(cx, bottom)) }) {
                sprite(img, cx, bottom, h, flip = en.vx > 0, alpha = (en.squash / 0.4f).coerceIn(0f, 1f))
            }
        } else {
            sprite(img, cx, bottom + unit * 0.12f, h, flip = en.vx > 0)
        }
    }

    // Tilki
    val fx = sx(e.px + FoxEngine.PW / 2f)
    val fb = sy(e.py + FoxEngine.PH)
    val blink = e.iframe > 0f && ((e.iframe * 12f).toInt() % 2 == 0)
    val foxImg = when {
        e.dead -> FoxSprites.fox3
        !e.onGround -> FoxSprites.fox3
        abs(e.vx) > 0.5f -> if ((e.runAnim.toInt() % 2) == 0) FoxSprites.fox1 else FoxSprites.fox2
        else -> FoxSprites.fox0
    }
    if (!blink) {
        if (e.dead) {
            withTransform({ scale(1f, -1f, pivot = Offset(fx, fb - unit * 0.6f)) }) {
                sprite(foxImg, fx, fb + unit * 0.12f, unit * 1.35f, flip = !e.facingRight)
            }
        } else {
            sprite(foxImg, fx, fb + unit * 0.12f, unit * 1.35f, flip = !e.facingRight)
        }
    }

    // Parcaciklar
    e.particles.forEach { p ->
        val s = p.size * unit
        drawRect(Color(p.color).copy(alpha = (p.life * 2f).coerceIn(0f, 1f)), Offset(sx(p.x) - s / 2, sy(p.y) - s / 2), Size(s, s))
    }

    // Tema isigi
    when (e.world) {
        1 -> drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xAA000000)), center = Offset(fx, fb - unit), radius = size.width * 0.7f))
        2 -> drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x44FF5A1F)), startY = size.height * 0.6f, endY = size.height))
    }
}

private fun DrawScope.drawBackground(theme: FoxWorld, camX: Float, unit: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(theme.skyTop), Color(theme.skyBottom))))
    val w = size.width
    val h = size.height
    when (theme) {
        FoxWorld.MEADOW -> {
            // Bulutlar
            val cloudOff = camX * unit * 0.1f
            for (i in -1..6) {
                val period = w / 3f
                val x = ((i * period - cloudOff) % (period * 7) + period * 7) % (period * 7) - period
                val y = h * (0.12f + (hash(i, 7) % 20) / 100f)
                val cw = unit * (2.2f + (hash(i, 3) % 10) / 6f)
                drawOval(Color.White.copy(alpha = 0.9f), Offset(x, y), Size(cw, unit * 0.8f))
                drawOval(Color.White.copy(alpha = 0.9f), Offset(x + cw * 0.25f, y - unit * 0.35f), Size(cw * 0.55f, unit * 0.8f))
            }
            hills(Color(theme.hillFar), camX * unit * 0.25f, h * 0.62f, unit * 5f, unit * 9f)
            hills(Color(theme.hillNear), camX * unit * 0.5f, h * 0.75f, unit * 3.5f, unit * 6f)
        }
        FoxWorld.CAVE -> {
            // Sarkitlar ve kristaller
            val off = camX * unit * 0.3f
            val step = unit * 2.5f
            val n = (w / step).toInt() + 3
            val start = floor(off / step).toInt()
            for (i in start until start + n) {
                val x = i * step - off
                val len = unit * (1f + (hash(i, 1) % 30) / 10f)
                val path = Path().apply {
                    moveTo(x, 0f); lineTo(x + step * 0.5f, len); lineTo(x + step, 0f); close()
                }
                drawPath(path, Color(theme.hillNear))
                if (hash(i, 2) % 4 == 0) {
                    val cy = h * (0.35f + (hash(i, 5) % 25) / 100f)
                    drawCircle(Color(0x3358C8FF), unit * 0.6f, Offset(x + step / 2, cy))
                    drawCircle(Color(0xAA9BE7FF), unit * 0.12f, Offset(x + step / 2, cy))
                }
            }
            hills(Color(theme.hillFar), camX * unit * 0.4f, h * 0.7f, unit * 2.5f, unit * 4f)
        }
        FoxWorld.CASTLE -> {
            val off = camX * unit * 0.35f
            val step = unit * 4f
            val n = (w / step).toInt() + 3
            val start = floor(off / step).toInt()
            for (i in start until start + n) {
                val x = i * step - off
                // Sutun
                drawRect(Color(theme.hillFar), Offset(x, 0f), Size(unit * 1.2f, h))
                drawRect(Color(0x22000000), Offset(x + unit, 0f), Size(unit * 0.2f, h))
                // Pencere
                if (hash(i, 9) % 2 == 0) {
                    val wy = h * 0.25f
                    drawRoundRect(Color(0xFF1A0A10), Offset(x + unit * 2f, wy), Size(unit * 0.9f, unit * 1.6f), CornerRadius(unit * 0.45f))
                    drawRoundRect(Color(0x66FF9A3C), Offset(x + unit * 2.1f, wy + unit * 0.1f), Size(unit * 0.7f, unit * 1.4f), CornerRadius(unit * 0.35f))
                }
            }
        }
    }
}

/** Tekrarlanan yumusak tepeler. */
private fun DrawScope.hills(color: Color, offset: Float, baseY: Float, height: Float, width: Float) {
    val n = (size.width / width).toInt() + 3
    val start = floor(offset / width).toInt()
    for (i in start until start + n) {
        val x = i * width - offset
        val hh = height * (0.6f + (hash(i, 11) % 40) / 100f)
        drawOval(color, Offset(x - width * 0.2f, baseY - hh), Size(width * 1.4f, hh * 2f))
    }
    drawRect(color, Offset(0f, baseY), Size(size.width, size.height - baseY))
}

/** Bir karoyu boşluksuz ciz (komsu karolarla ayni piksel sinirina oturur). */
private fun DrawScope.tile(img: ImageBitmap, left: Float, top: Float, unit: Float) {
    val l = floor(left).toInt()
    val t = floor(top).toInt()
    val r = floor(left + unit).toInt()
    val b = floor(top + unit).toInt()
    drawImage(
        img, IntOffset.Zero, IntSize(img.width, img.height),
        IntOffset(l, t), IntSize(max(1, r - l), max(1, b - t)),
        filterQuality = FilterQuality.None,
    )
}

/** Piksel art'i keskin ciz: (cx, bottom) alt-orta noktasi, [height] piksel cinsinden yukseklik. */
private fun DrawScope.sprite(
    img: ImageBitmap,
    cx: Float,
    bottom: Float,
    height: Float,
    flip: Boolean = false,
    alpha: Float = 1f,
) {
    val h = height
    val w = height * img.width / img.height
    val left = (cx - w / 2f).roundToInt()
    val top = (bottom - h).roundToInt()
    val draw: DrawScope.() -> Unit = {
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(max(1, w.roundToInt()), max(1, h.roundToInt())),
            alpha = alpha,
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
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 18.dp),
        ) { content() }
    }
}

@Composable
private fun PauseOverlay(viewModel: FoxViewModel) {
    Overlay {
        Text(stringResource(R.string.fx_paused), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Button(
            onClick = viewModel::resume,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = AppColors.OnPrimary),
            modifier = Modifier.padding(top = 14.dp).width(260.dp).height(48.dp),
        ) { Text(stringResource(R.string.fx_resume), fontWeight = FontWeight.Black) }
        TextButton(onClick = viewModel::retry) {
            Text(stringResource(R.string.fx_restart), color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = viewModel::toHub) {
            Text(stringResource(R.string.fx_to_levels), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeadOverlay(state: FoxUiState, hasCheckpoint: Boolean, onRevive: () -> Unit, onCheckpoint: () -> Unit, onHub: () -> Unit) {
    Overlay {
        Text(stringResource(R.string.fx_dead), color = Red, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.fx_level, state.world + 1, state.level + 1), color = AppColors.TextSecondary, fontSize = 13.sp)
        if (!state.reviveUsed) {
            Button(
                onClick = onRevive,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = AppColors.OnPrimary),
                modifier = Modifier.padding(top = 14.dp).width(300.dp).height(48.dp),
            ) {
                Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.fx_revive), fontWeight = FontWeight.Black)
            }
        }
        Button(
            onClick = onCheckpoint,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = AppColors.OnPrimary),
            modifier = Modifier.padding(top = 8.dp).width(300.dp).height(48.dp),
        ) {
            Text(stringResource(if (hasCheckpoint) R.string.fx_from_checkpoint else R.string.fx_from_start), fontWeight = FontWeight.Black)
        }
        TextButton(onClick = onHub) {
            Text(stringResource(R.string.fx_to_levels), color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WonOverlay(state: FoxUiState, onNext: () -> Unit, onRetry: () -> Unit, onHub: () -> Unit) {
    val r = state.result ?: return
    Overlay {
        Text(stringResource(R.string.fx_complete), color = Green, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Row(Modifier.padding(top = 8.dp)) {
            repeat(3) { i -> SpriteIcon(FoxSprites.star, 44.dp, alpha = if (i < r.stars) 1f else 0.2f) }
        }
        Text(
            stringResource(R.string.fx_result, r.coins, formatTime(r.time)),
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            stringResource(R.string.game_score) + " " + state.score + (if (state.newBestStars) "  ·  " + stringResource(R.string.game_new_record) else ""),
            color = Gold,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.hasNext) {
                Button(onClick = onNext, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = AppColors.OnPrimary)) {
                    Text(stringResource(R.string.fx_next), fontWeight = FontWeight.Black)
                }
            }
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary)) {
                Text(stringResource(R.string.fx_retry), fontWeight = FontWeight.Black)
            }
            Button(onClick = onHub, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHigh, contentColor = AppColors.TextPrimary)) {
                Text(stringResource(R.string.fx_to_levels), fontWeight = FontWeight.Black)
            }
        }
    }
}
