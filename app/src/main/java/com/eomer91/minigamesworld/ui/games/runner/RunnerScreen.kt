package com.eomer91.minigamesworld.ui.games.runner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
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
import com.eomer91.minigamesworld.ui.games.common.SwipeDirection
import com.eomer91.minigamesworld.ui.games.common.swipeGestures
import com.eomer91.minigamesworld.ui.games.fox.FoxSprites
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RunnerScreen(viewModel: RunnerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF120C08))
            .swipeGestures(threshold = 22.dp) { d ->
                viewModel.swipe(
                    when (d) {
                        SwipeDirection.LEFT -> 0
                        SwipeDirection.RIGHT -> 1
                        SwipeDirection.UP -> 2
                        SwipeDirection.DOWN -> 3
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val f = viewModel.frame
            drawRun(viewModel.engine)
        }
        // HUD
        val e = viewModel.engine
        @Suppress("UNUSED_VARIABLE")
        val f = viewModel.frame
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Hud("📏 ${e.distance.toInt()} m")
            Spacer(Modifier.width(8.dp))
            Hud("🪙 ${e.coins}")
            Spacer(Modifier.weight(1f))
            Hud("🏆 ${maxOf(state.best, e.score)}")
        }

        when (state.phase) {
            RunnerPhase.READY, RunnerPhase.PAUSED -> Message(
                stringResource(if (state.phase == RunnerPhase.READY) R.string.rn_swipe_start else R.string.rn_swipe_continue),
                stringResource(R.string.rn_how),
                onClick = viewModel::start,
            )
            RunnerPhase.CRASHED -> Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.Surface)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.rn_crashed), color = AppColors.Danger, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Button(
                    onClick = {
                        showRewarded({ viewModel.revive() }) {
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(Icons.Rounded.OndemandVideo, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.rn_revive), fontWeight = FontWeight.Black)
                }
                TextButton(onClick = viewModel::giveUp) { Text(stringResource(R.string.rn_give_up), color = AppColors.TextSecondary) }
            }
            RunnerPhase.OVER -> GameOverPanel(
                score = state.score,
                isNewRecord = state.isNewRecord,
                onRestart = { showInterstitial { viewModel.restart() } },
                goldEarned = state.goldEarned,
                modifier = Modifier.align(Alignment.Center).padding(20.dp),
            )
            RunnerPhase.PLAYING -> Unit
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Hud(text: String) {
    Text(
        text,
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun Message(title: String, sub: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(sub, color = Color(0xFFE5E7EB), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ============================================================ Cizim (sahte 3B)

private class Proj(val w: Float, val h: Float) {
    val horizon = h * 0.3f
    val bottom = h * 0.93f
    fun s(z: Float) = DEPTH / (z.coerceAtLeast(-2.5f) + DEPTH)
    fun y(z: Float) = horizon + (bottom - horizon) * s(z)
    fun laneW(z: Float) = w * 0.27f * s(z)
    fun x(lane: Float, z: Float) = w / 2f + lane * laneW(z)
    /** 1 birim yukseklik (piksel) */
    fun unit(z: Float) = laneW(z) * 0.8f

    companion object {
        const val DEPTH = 8f
    }
}

private fun DrawScope.drawRun(e: RunnerEngine) {
    val p = Proj(size.width, size.height)
    val w = size.width
    val h = size.height
    // Tunel arka plani
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B0705), Color(0xFF2A1A10), Color(0xFF3B2414)), 0f, h))
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFB347), Color.Transparent), center = Offset(w / 2, p.horizon), radius = w * 0.35f), w * 0.35f, Offset(w / 2, p.horizon))
    // Duvarlar
    val far = RunnerEngine.FAR
    val lw = Path().apply {
        moveTo(0f, 0f); lineTo(p.x(-1.8f, far), p.horizon - p.unit(far) * 3f); lineTo(p.x(-1.8f, far), p.y(far)); lineTo(p.x(-1.8f, -2f), h); lineTo(0f, h); close()
    }
    val rw = Path().apply {
        moveTo(w, 0f); lineTo(p.x(1.8f, far), p.horizon - p.unit(far) * 3f); lineTo(p.x(1.8f, far), p.y(far)); lineTo(p.x(1.8f, -2f), h); lineTo(w, h); close()
    }
    drawPath(lw, Brush.horizontalGradient(listOf(Color(0xFF3D2B1F), Color(0xFF241810)), 0f, w * 0.3f))
    drawPath(rw, Brush.horizontalGradient(listOf(Color(0xFF241810), Color(0xFF3D2B1F)), w * 0.7f, w))
    // Zemin
    val ground = Path().apply {
        moveTo(p.x(-1.8f, far), p.y(far)); lineTo(p.x(1.8f, far), p.y(far)); lineTo(p.x(1.8f, -2f), h); lineTo(p.x(-1.8f, -2f), h); close()
    }
    drawPath(ground, Brush.verticalGradient(listOf(Color(0xFF2B1D14), Color(0xFF6B4A33)), p.horizon, h))

    // Traversler
    var z = -e.scroll
    while (z < far) {
        if (z > -2f) {
            val y = p.y(z)
            val th = (p.unit(z) * 0.12f).coerceAtLeast(1f)
            drawRect(fog(Color(0xFF5C3A21), z), Offset(p.x(-1.6f, z), y - th / 2), Size(p.laneW(z) * 3.2f, th))
        }
        z += 2f
    }
    // Raylar
    for (lane in -1..1) {
        for (side in listOf(-0.28f, 0.28f)) {
            drawLine(Color(0xFF9CA3AF), Offset(p.x(lane + side, -2f), h), Offset(p.x(lane + side, far), p.y(far)), (w * 0.006f).coerceAtLeast(2f))
        }
    }
    // Mesaleler
    var tz = 12f - (e.distance % 12f)
    while (tz < far) {
        for (sx in listOf(-1.75f, 1.75f)) {
            val x = p.x(sx, tz)
            val y = p.y(tz) - p.unit(tz) * 2.2f
            val flick = 1f + 0.1f * sin(e.time * 20f + tz)
            drawCircle(Brush.radialGradient(listOf(Color(0x88FFB347), Color.Transparent), center = Offset(x, y), radius = p.unit(tz) * 1.4f * flick), p.unit(tz) * 1.4f * flick, Offset(x, y))
            drawRect(Color(0xFF5C3A21), Offset(x - p.unit(tz) * 0.06f, y), Size(p.unit(tz) * 0.12f, p.unit(tz) * 0.5f))
            drawCircle(Color(0xFFFFD166), p.unit(tz) * 0.12f * flick, Offset(x, y))
        }
        tz += 12f
    }

    val sorted = e.obstacles.sortedByDescending { it.z }
    sorted.filter { it.z > 0.3f }.forEach { drawObstacle(p, it, e.time) }
    drawPlayer(p, e)
    sorted.filter { it.z <= 0.3f }.forEach { drawObstacle(p, it, e.time) }

    if (e.hitFlash > 0f) drawRect(Color.Red.copy(alpha = e.hitFlash * 0.5f))
}

/** Uzaklastikca karart. */
private fun fog(c: Color, z: Float): Color = lerp(c, Color(0xFF1A110B), (z / RunnerEngine.FAR).coerceIn(0f, 1f) * 0.75f)

private fun DrawScope.drawObstacle(p: Proj, o: Obstacle, time: Float) {
    if (o.type == ObstacleType.COIN && o.taken) return
    val z = o.z
    if (z > RunnerEngine.FAR || z < -2.4f) return
    val x = p.x(o.lane.toFloat(), z)
    val y = p.y(z)
    val lw = p.laneW(z)
    val u = p.unit(z)
    when (o.type) {
        ObstacleType.ROCK -> {
            drawOval(Color(0x55000000), Offset(x - lw * 0.48f, y - u * 0.12f), Size(lw * 0.96f, u * 0.25f))
            drawOval(fog(Color(0xFF7A7F87), z), Offset(x - lw * 0.45f, y - u * 1.15f), Size(lw * 0.9f, u * 1.2f))
            drawOval(fog(Color(0xFF5B6068), z), Offset(x - lw * 0.1f, y - u * 0.9f), Size(lw * 0.5f, u * 0.9f))
            drawOval(fog(Color(0xFFA3A8B0), z), Offset(x - lw * 0.3f, y - u * 1.05f), Size(lw * 0.25f, u * 0.25f))
        }
        ObstacleType.BEAM -> {
            val top = y - u * 1.55f
            drawRect(fog(Color(0xFF6B4226), z), Offset(x - lw * 0.5f, top), Size(lw, u * 0.32f))
            drawRect(fog(Color(0xFF8B5A2B), z), Offset(x - lw * 0.5f, top), Size(lw, u * 0.1f))
            drawRect(fog(Color(0xFF4A2E1A), z), Offset(x - lw * 0.5f, top), Size(lw * 0.06f, y - top))
            drawRect(fog(Color(0xFF4A2E1A), z), Offset(x + lw * 0.44f, top), Size(lw * 0.06f, y - top))
            // Uyari seridi
            drawRect(fog(Color(0xFFFFD166), z), Offset(x - lw * 0.3f, top + u * 0.12f), Size(lw * 0.6f, u * 0.08f))
        }
        ObstacleType.BARREL -> {
            val bw = lw * 0.58f
            val bh = u * 0.8f
            drawOval(Color(0x55000000), Offset(x - bw * 0.6f, y - u * 0.1f), Size(bw * 1.2f, u * 0.2f))
            drawRoundRect(fog(Color(0xFF8B5A2B), z), Offset(x - bw / 2, y - bh), Size(bw, bh), CornerRadius(bw * 0.25f))
            for (k in listOf(0.2f, 0.75f)) drawRect(fog(Color(0xFF4B5563), z), Offset(x - bw / 2, y - bh * (1 - k) - u * 0.04f), Size(bw, u * 0.08f))
            drawRect(fog(Color(0xFFB07A4A), z), Offset(x - bw * 0.15f, y - bh * 0.95f), Size(bw * 0.1f, bh * 0.85f))
        }
        ObstacleType.COIN -> {
            val r = lw * 0.16f
            val cy = y - u * 0.55f + sin(time * 5f + z) * u * 0.05f
            val sx = abs(cos(time * 6f + z * 0.5f)).coerceAtLeast(0.2f)
            drawOval(fog(Color(0xFFB8860B), z), Offset(x - r * sx, cy - r), Size(r * 2 * sx, r * 2))
            drawOval(fog(Color(0xFFFFD166), z), Offset(x - r * 0.75f * sx, cy - r * 0.75f), Size(r * 1.5f * sx, r * 1.5f))
        }
    }
}

private fun DrawScope.drawPlayer(p: Proj, e: RunnerEngine) {
    val z = 0f
    val x = p.x(e.laneX, z)
    val lw = p.laneW(z)
    val u = p.unit(z)
    val ground = p.y(z)
    val lift = e.jumpY * u
    // Golge
    drawOval(Color(0x66000000), Offset(x - lw * 0.42f, ground - u * 0.08f), Size(lw * 0.84f, u * 0.2f))
    val base = ground - lift
    val cw = lw * 0.78f
    val ch = u * 0.62f
    // Tilki (arabanin icinden) - egilince saklanir
    val foxH = u * 1.1f
    val foxTop = base - ch - foxH * (if (e.ducking) 0.25f else 0.85f)
    val img = if ((e.time * 8f).toInt() % 2 == 0) FoxSprites.fox1 else FoxSprites.fox2
    val fw = foxH * img.width / img.height
    drawImage(
        img,
        srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
        srcSize = androidx.compose.ui.unit.IntSize(img.width, img.height),
        dstOffset = androidx.compose.ui.unit.IntOffset((x - fw / 2).roundToInt(), foxTop.roundToInt()),
        dstSize = androidx.compose.ui.unit.IntSize(fw.roundToInt(), foxH.roundToInt()),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
    )
    // Araba
    val cart = Path().apply {
        moveTo(x - cw / 2, base - ch)
        lineTo(x + cw / 2, base - ch)
        lineTo(x + cw * 0.42f, base - u * 0.12f)
        lineTo(x - cw * 0.42f, base - u * 0.12f)
        close()
    }
    drawPath(cart, Brush.verticalGradient(listOf(Color(0xFF8D99AE), Color(0xFF4A5568)), base - ch, base))
    drawRect(Color(0xFF2D3748), Offset(x - cw / 2, base - ch), Size(cw, u * 0.08f))
    for (k in listOf(-0.25f, 0.25f)) drawRect(Color(0xFFB8860B), Offset(x + cw * k - u * 0.03f, base - ch), Size(u * 0.06f, ch - u * 0.12f))
    // Tekerlekler
    for (k in listOf(-0.3f, 0.3f)) {
        drawCircle(Color(0xFF1A202C), u * 0.14f, Offset(x + cw * k, base - u * 0.08f))
        drawCircle(Color(0xFF718096), u * 0.06f, Offset(x + cw * k, base - u * 0.08f))
    }
}
