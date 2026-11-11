package com.eomer91.minigamesworld.ui.games.mahjong

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import com.eomer91.minigamesworld.feedback.GameMusicEffect
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MahjongScreen(viewModel: MahjongViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val cache = remember { MjTileCache() }

    // Doga sesi arka planda kisik calsin
    GameMusicEffect(R.raw.music_mahjong, playing = !state.loading, gain = 0.35f)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) viewModel.setPaused(true)
            if (e == Lifecycle.Event.ON_RESUME) viewModel.setPaused(false)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                withFrameNanos { }
                if (viewModel.animating()) viewModel.tick()
            }
        }
    }
    BackHandler(enabled = state.phase != MahjongPhase.HOME) { viewModel.goHome() }

    val level = when (state.phase) {
        MahjongPhase.PLAY -> viewModel.game?.info?.level ?: state.profile.level
        MahjongPhase.WON -> state.profile.level - 1
        else -> state.profile.level
    }
    val theme = MJ_THEMES[MahjongLevels.theme(level.coerceAtLeast(1))]

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom))),
    ) {
        if (theme.key == "night") StarField()
        if (state.loading) return@Box
        when (state.phase) {
            MahjongPhase.HOME -> Home(state, theme, cache, viewModel)
            MahjongPhase.NEW_CHAPTER -> NewChapter(state, theme, cache) { showInterstitial { viewModel.next() } }
            else -> {
                Play(state, theme, cache, viewModel, showRewarded)
                AnimatedVisibility(state.phase == MahjongPhase.WON, enter = fadeIn(), exit = fadeOut()) {
                    Won(state, theme, onNext = { showInterstitial { viewModel.next() } }, onHome = viewModel::goHome)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- Ana ekran

@Composable
private fun Home(state: MahjongUiState, theme: MjTheme, cache: MjTileCache, viewModel: MahjongViewModel) {
    val p = state.profile
    val chapter = MahjongLevels.chapter(p.level)
    val inChapter = (p.level - 1) % MahjongLevels.CHAPTER_SIZE
    val resume = p.current?.level == p.level
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.game_mahjong_name), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Text(
            stringResource(R.string.mj_chapter, chapter + 1) + " · " + stringResource(theme.name),
            color = theme.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(18.dp))
        TileShowcase(MahjongLevels.theme(p.level), cache, count = 3, height = 120.dp)
        Spacer(Modifier.height(18.dp))
        // Bolum ilerlemesi
        Column(Modifier.fillMaxWidth(0.8f)) {
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(inChapter / MahjongLevels.CHAPTER_SIZE.toFloat()).height(10.dp).background(theme.accent))
            }
            Text(
                "$inChapter / ${MahjongLevels.CHAPTER_SIZE}",
                color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = viewModel::play,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = Color(0xFF1B1B1B)),
            modifier = Modifier.fillMaxWidth(0.8f).height(76.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (resume) R.string.mj_continue else R.string.mj_play), fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.mj_level, p.level), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Stat("⭐", p.totalStars.toString())
            Stat("💡", p.hints.toString())
            Stat("🔀", p.shuffles.toString())
        }
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.22f))
                .clickable { viewModel.toggleHighlight() }
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Text(stringResource(R.string.mj_highlight), color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = p.highlightFree, onCheckedChange = { viewModel.toggleHighlight() },
                colors = SwitchDefaults.colors(checkedTrackColor = theme.accent, checkedThumbColor = Color.White),
            )
        }
    }
}

@Composable
private fun Stat(icon: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

/** Temanin birkac tasini yan yana gosterir. */
@Composable
private fun TileShowcase(theme: Int, cache: MjTileCache, count: Int, height: androidx.compose.ui.unit.Dp) {
    val faces = remember(theme, count) {
        val n = MahjongArt.sets.getValue(MJ_THEMES[theme].key).size
        List(count) { (it * 7 + 3 + theme * 5) % n }
    }
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val k = size.height / MjGeo.ART_H
        val w = MjGeo.ART_W * k
        val gap = w * 0.05f
        val total = count * w + (count - 1) * gap
        var x = (size.width - total) / 2f
        faces.forEachIndexed { i, f ->
            val img = cache.get(theme, f, k)
            val lift = if (i == count / 2) -8f * k else 0f
            drawImage(
                img, IntOffset.Zero, IntSize(img.width, img.height),
                IntOffset(x.roundToInt(), lift.roundToInt()), IntSize(w.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
            x += w + gap
        }
    }
}

// ---------------------------------------------------------------------------- Oyun

/** Tahtanin ekrana yerlesimi: s = birim basina piksel. */
private class BoardGeo(val s: Float, val ox: Float, val oy: Float) {
    fun faceX(t: MahjongTile) = ox + (t.x * MjGeo.STEP_X + t.z * MjGeo.LAYER_X) * s
    fun faceY(t: MahjongTile) = oy + (t.y * MjGeo.STEP_Y + t.z * MjGeo.LAYER_Y) * s
}

private fun fitBoard(tiles: List<MahjongTile>, w: Float, h: Float, maxS: Float): BoardGeo {
    var l = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var t = Float.MAX_VALUE; var b = -Float.MAX_VALUE
    for (tile in tiles) {
        val x = tile.x * MjGeo.STEP_X + tile.z * MjGeo.LAYER_X
        val y = tile.y * MjGeo.STEP_Y + tile.z * MjGeo.LAYER_Y
        l = min(l, x - MjGeo.PAD_L); r = max(r, x + MjGeo.ART_W - MjGeo.PAD_L)
        t = min(t, y - MjGeo.PAD_T); b = max(b, y + MjGeo.ART_H - MjGeo.PAD_T)
    }
    val s = min(min(w / (r - l), h / (b - t)), maxS)
    val ox = (w - (r - l) * s) / 2f - l * s
    val oy = (h - (b - t) * s) / 2f - t * s
    return BoardGeo(s, ox, oy)
}

@Composable
private fun Play(
    state: MahjongUiState,
    theme: MjTheme,
    cache: MjTileCache,
    viewModel: MahjongViewModel,
    showRewarded: (() -> Unit, () -> Unit) -> Unit,
) {
    val game = viewModel.game
    Column(Modifier.fillMaxSize()) {
        // Ust cubuk
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { viewModel.goHome() }.padding(horizontal = 10.dp, vertical = 2.dp),
            )
            Text(
                stringResource(R.string.mj_level, game?.info?.level ?: (state.profile.level - 1)),
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f),
            )
            Chip("⏱ " + "%d:%02d".format(state.seconds / 60, state.seconds % 60))
            Spacer(Modifier.width(8.dp))
            Chip("🀄 ${state.tilesLeft}")
        }
        // Tahta
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (game != null) Board(game, state.profile.highlightFree, theme, cache, viewModel)
            // Column icinde oldugu icin ColumnScope surumu secilmesin: tam adla cagir.
            androidx.compose.animation.AnimatedVisibility(state.stuck && state.phase == MahjongPhase.PLAY, Modifier.align(Alignment.Center), enter = fadeIn(), exit = fadeOut()) {
                StuckPanel(state, theme, viewModel, showRewarded)
            }
            state.toast?.let { t ->
                Text(
                    toastText(t), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        // Alt cubuk
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolButton("↶", stringResource(R.string.mj_undo), null, enabled = state.canUndo, theme, Modifier.weight(1f)) { viewModel.undo() }
            ToolButton("💡", stringResource(R.string.mj_hint), state.profile.hints, enabled = true, theme, Modifier.weight(1f)) {
                if (state.profile.hints > 0) viewModel.hint() else showRewarded({ viewModel.rewardHints() }, { viewModel.adNotReady() })
            }
            ToolButton("🔀", stringResource(R.string.mj_shuffle), state.profile.shuffles, enabled = true, theme, Modifier.weight(1f)) {
                if (state.profile.shuffles > 0) viewModel.shuffle() else showRewarded({ viewModel.rewardShuffles() }, { viewModel.adNotReady() })
            }
        }
    }
}

@Composable
private fun toastText(t: MahjongToast): String = stringResource(
    when (t) {
        MahjongToast.NO_HINTS -> R.string.mj_no_hints
        MahjongToast.NO_SHUFFLES -> R.string.mj_no_shuffles
        MahjongToast.AD_NOT_READY -> R.string.ad_not_ready
        MahjongToast.REWARD_HINTS -> R.string.mj_reward_hints
        MahjongToast.REWARD_SHUFFLES -> R.string.mj_reward_shuffles
        MahjongToast.TRAY_FULL -> R.string.mj_tray_full
        MahjongToast.NO_HINT_MOVE -> R.string.mj_no_hint_move
    },
)

@Composable
private fun Chip(text: String) {
    Text(
        text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun ToolButton(icon: String, label: String, count: Int?, enabled: Boolean, theme: MjTheme, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = if (enabled) 0.3f else 0.15f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 24.sp, color = Color.White.copy(alpha = if (enabled) 1f else 0.4f))
            Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = if (enabled) 0.9f else 0.4f), fontWeight = FontWeight.Bold)
        }
        if (count != null) {
            Text(
                if (count > 0) count.toString() else "▶",
                fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B1B1B),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (count > 0) theme.accent else Color(0xFF4ADE80)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun StuckPanel(state: MahjongUiState, theme: MjTheme, viewModel: MahjongViewModel, showRewarded: (() -> Unit, () -> Unit) -> Unit) {
    Column(
        Modifier.padding(24.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xEE111827)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.mj_tray_full), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(stringResource(R.string.mj_tray_full_help), color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = viewModel::undo,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = Color(0xFF1B1B1B)),
        ) { Text("↶ " + stringResource(R.string.mj_undo), fontWeight = FontWeight.Black, fontSize = 18.sp) }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.mj_restart), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { viewModel.restart() }.padding(8.dp),
        )
    }
}

/** Tepsi yerlesimi (piksel). */
private class TrayGeo(width: Float, top: Float) {
    val tw = min(width / 6f, 92f * 2.6f)          // tepsi tasi yuz genisligi
    val s = tw / MjGeo.FACE_W
    val pitch = tw * 1.16f
    val panelW = pitch * MahjongGame.TRAY_SIZE + tw * 0.3f
    val panelH = MjGeo.FACE_H * s + tw * 0.34f
    val panelL = (width - panelW) / 2f
    val panelT = top
    val height = panelH + tw * 0.25f
    fun slotX(i: Float) = panelL + tw * 0.15f + (pitch - tw) / 2f + i * pitch
    val slotY = panelT + tw * 0.17f
}

@Composable
private fun Board(game: MahjongGame, highlight: Boolean, theme: MjTheme, cache: MjTileCache, viewModel: MahjongViewModel) {
    // Cizim sirasi: alt kat once; ayni katta ust satir once, sagdan sola (sol-alt govde ondeki tasin altinda kalsin).
    val order = remember(game) {
        game.tiles.indices.sortedWith(compareBy({ game.tiles[it].z }, { game.tiles[it].y }, { -game.tiles[it].x }))
    }
    var zoom by remember(game.info.level) { mutableFloatStateOf(1f) }
    var pan by remember(game.info.level) { mutableStateOf(Offset.Zero) }
    val themeIndex = game.info.theme

    fun geo(w: Float, h: Float): Pair<TrayGeo, BoardGeo> {
        val tray = TrayGeo(w, 4f)
        val top = tray.height
        val fit = fitBoard(game.tiles, w, h - top, maxScale(w))
        return tray to BoardGeo(fit.s * zoom, fit.ox * zoom + pan.x, fit.oy * zoom + pan.y + top)
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(game) {
                detectTransformGestures { centroid, p, z, _ ->
                    val nz = (zoom * z).coerceIn(1f, 3f)
                    pan = (pan - centroid) * (nz / zoom) + centroid + p
                    zoom = nz
                    if (zoom <= 1.001f) pan = Offset.Zero
                }
            }
            .pointerInput(game) {
                detectTapGestures { pos ->
                    val (tray, g) = geo(size.width.toFloat(), size.height.toFloat())
                    if (pos.y < tray.height) return@detectTapGestures
                    for (i in order.indices.reversed()) {
                        val id = order[i]
                        if (!game.onBoard(id)) continue
                        val t = game.tiles[id]
                        val x = g.faceX(t); val y = g.faceY(t)
                        if (pos.x >= x && pos.x <= x + MjGeo.FACE_W * g.s && pos.y >= y && pos.y <= y + MjGeo.FACE_H * g.s) {
                            viewModel.tap(id)
                            break
                        }
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val f = viewModel.frame
        val now = System.currentTimeMillis()
        val (tray, g) = geo(size.width, size.height)
        val s = g.s
        val k = (s / zoom * 2f).coerceAtMost(2.6f)
        val kTray = (tray.s * 1.5f).coerceAtMost(2.6f)
        val fx = viewModel.fx
        val enterT = ((now - fx.enterAt) / MahjongFx.ENTER_MS.toFloat()).coerceIn(0f, 1f)
        val shuffleT = ((now - fx.shuffleAt) / MahjongFx.SHUFFLE_MS.toFloat()).coerceIn(0f, 1f)
        val hintOn = fx.hint.isNotEmpty() && now - fx.hintAt < MahjongFx.HINT_MS
        val pulse = 0.5f + 0.5f * sin((now - fx.hintAt) / 140f)

        // ---- Tahta
        clipRect(top = tray.height) {
            for ((rank, id) in order.withIndex()) {
                if (!game.onBoard(id)) continue
                val t = game.tiles[id]
                var x = g.faceX(t)
                var y = g.faceY(t)
                var alpha = 1f
                var sc = 1f
                if (enterT < 1f) {
                    val local = ((enterT - rank / order.size.toFloat() * 0.5f) / 0.5f).coerceIn(0f, 1f)
                    alpha = local
                    y -= (1f - local) * 40f * s
                }
                if (shuffleT < 1f) sc *= 1f - 0.12f * sin(shuffleT * PI.toFloat())
                if (id == fx.shakeId && now - fx.shakeAt < MahjongFx.SHAKE_MS) {
                    x += sin((now - fx.shakeAt) / 25f) * 4f * s * (1f - (now - fx.shakeAt) / MahjongFx.SHAKE_MS.toFloat())
                }
                if (alpha <= 0.01f) continue
                val cx = x + MjGeo.FACE_W * s / 2f
                val cy = y + MjGeo.FACE_H * s / 2f
                withTransform({ scale(sc, sc, Offset(cx, cy)) }) {
                    drawTile(cache.get(themeIndex, t.face, k), x, y, s, alpha)
                    val faceSize = Size(MjGeo.FACE_W * s, MjGeo.FACE_H * s)
                    val r = CornerRadius(MjGeo.RADIUS * s)
                    if (highlight && !game.isFree(id)) drawRoundRect(Color(0xFF1B2433).copy(alpha = 0.26f), Offset(x, y), faceSize, r)
                    if (hintOn && id in fx.hint) {
                        drawRoundRect(Color(0xFF4ADE80).copy(alpha = 0.25f + 0.3f * pulse), Offset(x, y), faceSize, r)
                        drawRoundRect(Color(0xFF4ADE80), Offset(x, y), faceSize, r, style = Stroke((3f + 3f * pulse) * s))
                    }
                }
            }
        }

        // ---- Tepsi paneli
        var shakeX = 0f
        if (fx.shakeId == -2 && now - fx.shakeAt < MahjongFx.SHAKE_MS) {
            shakeX = sin((now - fx.shakeAt) / 22f) * 8f * (1f - (now - fx.shakeAt) / MahjongFx.SHAKE_MS.toFloat())
        }
        val full = game.isTrayFull
        drawRoundRect(
            Color.Black.copy(alpha = 0.32f), Offset(tray.panelL + shakeX, tray.panelT), Size(tray.panelW, tray.panelH), CornerRadius(tray.tw * 0.22f),
        )
        if (full) drawRoundRect(
            Color(0xFFF87171), Offset(tray.panelL + shakeX, tray.panelT), Size(tray.panelW, tray.panelH), CornerRadius(tray.tw * 0.22f), style = Stroke(3f),
        )
        for (i in 0 until MahjongGame.TRAY_SIZE) {
            drawRoundRect(
                Color.White.copy(alpha = 0.10f), Offset(tray.slotX(i.toFloat()) + shakeX, tray.slotY),
                Size(tray.tw, MjGeo.FACE_H * tray.s), CornerRadius(MjGeo.RADIUS * tray.s),
            )
        }
        fun boardPos(id: Int) = Offset(g.faceX(game.tiles[id]), g.faceY(game.tiles[id]))
        fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        fun ease(t: Float) = 1f - (1f - t) * (1f - t)

        // Tepsideki taslar (ucanlar dahil)
        for (id in game.tray) {
            val slot = fx.trayX[id] ?: game.tray.indexOf(id).toFloat()
            val target = Offset(tray.slotX(slot) + shakeX, tray.slotY)
            val flyStart = fx.fly[id]
            if (flyStart != null && now - flyStart < MahjongFx.FLY_MS) {
                val t = ease((now - flyStart) / MahjongFx.FLY_MS.toFloat())
                val p = lerp(boardPos(id), target, t)
                val sc = s + (tray.s - s) * t
                drawTile(cache.get(themeIndex, game.tiles[id].face, max(k, kTray)), p.x, p.y, sc, 1f)
            } else {
                drawTile(cache.get(themeIndex, game.tiles[id].face, kTray), target.x, target.y, tray.s, 1f)
                if (hintOn && id in fx.hint) {
                    drawRoundRect(Color(0xFF4ADE80), target, Size(tray.tw, MjGeo.FACE_H * tray.s), CornerRadius(MjGeo.RADIUS * tray.s), style = Stroke(4f))
                }
            }
        }
        // Eslesen ciftler: es tepside bekler, yeni tas ustune ucar, sonra ikisi birden patlar
        for (pf in fx.pairs) {
            val target = Offset(tray.slotX(pf.slot), tray.slotY)
            val el = now - pf.start
            val faceIdx = game.tiles[pf.id].face
            if (el < MahjongFx.FLY_MS) {
                drawTile(cache.get(themeIndex, faceIdx, kTray), target.x, target.y, tray.s, 1f)
                val t = ease(el / MahjongFx.FLY_MS.toFloat())
                val p = lerp(boardPos(pf.id), Offset(target.x + tray.tw * 0.18f, target.y - tray.tw * 0.1f), t)
                drawTile(cache.get(themeIndex, faceIdx, max(k, kTray)), p.x, p.y, s + (tray.s - s) * t, 1f)
            } else {
                val bt = ((el - MahjongFx.FLY_MS) / MahjongFx.BURST_MS.toFloat()).coerceIn(0f, 1f)
                val cx = target.x + tray.tw / 2f
                val cy = target.y + MjGeo.FACE_H * tray.s / 2f
                withTransform({ scale(1f + 0.35f * bt, 1f + 0.35f * bt, Offset(cx, cy)) }) {
                    drawTile(cache.get(themeIndex, faceIdx, kTray), target.x, target.y, tray.s, 1f - bt)
                }
                sparkles(cx, cy, tray.s * 1.4f, bt, theme.accent)
            }
        }
    }
}

/** Onbellekteki tas resmini, yuzunun sol ustu (x, y) ve olcek [s] ile ciz. */
private fun DrawScope.drawTile(img: androidx.compose.ui.graphics.ImageBitmap, x: Float, y: Float, s: Float, alpha: Float) {
    drawImage(
        img, IntOffset.Zero, IntSize(img.width, img.height),
        IntOffset((x - MjGeo.PAD_L * s).roundToInt(), (y - MjGeo.PAD_T * s).roundToInt()),
        IntSize((MjGeo.ART_W * s).roundToInt(), (MjGeo.ART_H * s).roundToInt()),
        alpha = alpha.coerceIn(0f, 1f), filterQuality = FilterQuality.Medium,
    )
}

/** Telefonda tas genisligi en fazla ~ekranin 1/4'u olsun (kucuk tahtalar dev gorunmesin). */
private fun maxScale(width: Float) = width / 4.2f / MjGeo.FACE_W

private fun DrawScope.sparkles(cx: Float, cy: Float, s: Float, t: Float, color: Color) {
    if (t >= 1f) return
    for (i in 0 until 8) {
        val a = i * PI.toFloat() / 4f + 0.3f
        val d = (20f + 70f * t) * s
        drawCircle(
            if (i % 2 == 0) color else Color.White,
            radius = (6f * (1f - t) + 1f) * s,
            center = Offset(cx + kotlin.math.cos(a) * d, cy + sin(a) * d),
            alpha = 1f - t,
        )
    }
}

// ---------------------------------------------------------------------------- Bitis ve bolum

@Composable
private fun Won(state: MahjongUiState, theme: MjTheme, onNext: () -> Unit, onHome: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xF2111827)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.mj_won_title), color = theme.accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.mj_level_done, state.profile.level - 1), color = Color.White, fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
            Row { repeat(3) { i -> Text(if (i < state.stars) "⭐" else "☆", fontSize = 44.sp, color = if (i < state.stars) Color.Unspecified else Color.White.copy(alpha = 0.35f)) } }
            Spacer(Modifier.height(6.dp))
            Text("⏱ %d:%02d".format(state.seconds / 60, state.seconds % 60), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = Color(0xFF1B1B1B)),
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) { Text(stringResource(R.string.mj_next) + " ▶", fontSize = 20.sp, fontWeight = FontWeight.Black) }
            Text(
                stringResource(R.string.mj_home), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onHome).padding(8.dp),
            )
        }
    }
}

@Composable
private fun NewChapter(state: MahjongUiState, theme: MjTheme, cache: MjTileCache, onStart: () -> Unit) {
    val p = state.profile
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.mj_new_chapter), color = theme.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(theme.name), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.mj_chapter, MahjongLevels.chapter(p.level) + 1), color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        Spacer(Modifier.height(20.dp))
        TileShowcase(MahjongLevels.theme(p.level), cache, count = 4, height = 110.dp)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.mj_new_tiles), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = Color(0xFF1B1B1B)),
            modifier = Modifier.fillMaxWidth(0.8f).height(64.dp),
        ) { Text(stringResource(R.string.mj_level, p.level) + " ▶", fontSize = 22.sp, fontWeight = FontWeight.Black) }
    }
}

/** Gece temasinda arka planda yildizlar. */
@Composable
private fun StarField() {
    val stars = remember { List(60) { Triple((it * 73 % 101) / 101f, (it * 37 % 97) / 97f, 0.6f + (it % 5) * 0.35f) } }
    Canvas(Modifier.fillMaxSize()) {
        for ((x, y, r) in stars) drawCircle(Color.White.copy(alpha = 0.25f + r * 0.15f), r * 1.4f, Offset(x * size.width, y * size.height))
    }
}
