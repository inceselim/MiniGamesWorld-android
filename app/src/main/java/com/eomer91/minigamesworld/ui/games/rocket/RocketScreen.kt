package com.eomer91.minigamesworld.ui.games.rocket

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val RockColor = Color(0xFF64748B)
private val RockDark = Color(0xFF334155)
private val StarGold = Color(0xFFFACC15)

/** Arka plan yildizlari (sabit, bir kez uretilir): x, y (0..1), parlaklik, katman. */
private val BackgroundStars = Random(42).let { r -> List(70) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } }

@Composable
fun RocketScreen(viewModel: RocketViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Kare dongusu: sadece on planda (reklamda / arka planda oyun durur).
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
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            ScoreHeader(score = state.game.score, best = state.best, onRestart = restartWithAd)

            Box(
                Modifier
                    .padding(top = 12.dp)
                    .weight(1f, fill = false)
                    .aspectRatio(RocketEngine.WIDTH / RocketEngine.HEIGHT)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(Unit) { detectTapGestures(onPress = { viewModel.onTap() }) },
            ) {
                Canvas(Modifier.fillMaxSize()) { drawWorld(state.game) }

                if (!state.game.started) {
                    Text(
                        stringResource(R.string.rocket_tap_to_start),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(top = 160.dp),
                    )
                }
                if (state.game.started && !state.game.gameOver) {
                    Text(
                        state.game.score.toString(),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    )
                }
            }

            if (state.game.gameOver) {
                if (!state.reviveUsed) {
                    Button(
                        onClick = { showRewarded({ viewModel.revive() }, adNotReady) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.rocket_revive), fontWeight = FontWeight.Black)
                    }
                }
                GameOverPanel(
                    score = state.game.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = restartWithAd,
                    detail = stringResource(R.string.rocket_result, state.game.gatesPassed, state.game.stars, state.game.score),
                    goldEarned = state.goldEarned,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun DrawScope.drawWorld(g: RocketGame) {
    val unit = size.width / RocketEngine.WIDTH

    // Uzay arka plani + paralaks yildizlar
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1020), Color(0xFF1E1B4B), Color(0xFF312E81))))
    BackgroundStars.forEach { (x, y, b) ->
        val layerSpeed = 0.15f + b * 0.5f
        val px = ((x * size.width - g.time * unit * layerSpeed * 2f) % size.width + size.width) % size.width
        drawCircle(Color.White.copy(alpha = 0.3f + b * 0.6f), 0.5.dp.toPx() + b * 1.5.dp.toPx(), Offset(px, y * size.height))
    }

    // Zemin (ay yuzeyi)
    val groundTop = RocketEngine.GROUND_Y * unit
    drawRect(Color(0xFF475569), Offset(0f, groundTop), Size(size.width, size.height - groundTop))
    drawRect(Color(0xFF64748B), Offset(0f, groundTop), Size(size.width, 3.dp.toPx()))

    // Asteroit kolonlari
    g.gates.forEach { gate ->
        val left = gate.x * unit
        val w = RocketEngine.GATE_WIDTH * unit
        val gapTop = (gate.gapY - gate.gapH / 2f) * unit
        val gapBottom = (gate.gapY + gate.gapH / 2f) * unit
        drawRock(left, 0f, w, gapTop)
        drawRock(left, gapBottom, w, groundTop - gapBottom)
        if (gate.hasStar && !gate.starTaken) {
            drawStar(Offset(left + w / 2f, gate.gapY * unit), unit * 0.35f, g.time)
        }
    }

    // Roket
    val center = Offset(RocketEngine.ROCKET_X * unit, g.y * unit)
    val tilt = (g.vy * 3.5f).coerceIn(-30f, 60f)
    rotate(degrees = 90f + tilt, pivot = center) { drawRocket(center, RocketEngine.ROCKET_RADIUS * unit, g) }
    if (g.shield > 0f) {
        drawCircle(Color(0xFF38BDF8).copy(alpha = 0.35f + 0.25f * sin(g.time * 20f)), RocketEngine.ROCKET_RADIUS * unit * 1.8f, center, style = Stroke(3.dp.toPx()))
    }
}

/** Ust yonu -y olan bir roket ciz (dondurme cagiran tarafta). */
private fun DrawScope.drawRocket(c: Offset, r: Float, g: RocketGame) {
    // Alev (zipladiktan hemen sonra buyuk)
    val flame = if (g.started && !g.gameOver) (0.5f + (-g.vy / 10f).coerceIn(0f, 1f)) else 0.3f
    val flamePath = Path().apply {
        moveTo(c.x - r * 0.45f, c.y + r * 0.9f)
        lineTo(c.x, c.y + r * (1.2f + flame * 1.2f))
        lineTo(c.x + r * 0.45f, c.y + r * 0.9f)
        close()
    }
    drawPath(flamePath, Color(0xFFF97316))
    // Kanatlar
    val wings = Path().apply {
        moveTo(c.x - r * 0.55f, c.y + r * 0.2f)
        lineTo(c.x - r * 1.05f, c.y + r * 1.0f)
        lineTo(c.x - r * 0.5f, c.y + r * 0.85f)
        close()
        moveTo(c.x + r * 0.55f, c.y + r * 0.2f)
        lineTo(c.x + r * 1.05f, c.y + r * 1.0f)
        lineTo(c.x + r * 0.5f, c.y + r * 0.85f)
        close()
    }
    drawPath(wings, Color(0xFFEF4444))
    // Govde
    drawRoundRect(Color(0xFFF1F5F9), Offset(c.x - r * 0.55f, c.y - r * 0.6f), Size(r * 1.1f, r * 1.55f), CornerRadius(r * 0.5f))
    val nose = Path().apply {
        moveTo(c.x - r * 0.55f, c.y - r * 0.45f)
        quadraticTo(c.x, c.y - r * 1.6f, c.x + r * 0.55f, c.y - r * 0.45f)
        close()
    }
    drawPath(nose, Color(0xFFEF4444))
    // Pencere
    drawCircle(Color(0xFF38BDF8), r * 0.28f, Offset(c.x, c.y - r * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.1f, Offset(c.x - r * 0.08f, c.y - r * 0.13f))
}

private fun DrawScope.drawRock(left: Float, top: Float, width: Float, height: Float) {
    if (height <= 0f) return
    drawRoundRect(RockColor, Offset(left, top), Size(width, height), CornerRadius(width * 0.25f))
    // Krater lekeleri
    val step = width * 1.3f
    var y = top + step * 0.5f
    var k = 0
    while (y < top + height - width * 0.3f) {
        val x = left + width * (if (k % 2 == 0) 0.32f else 0.68f)
        drawCircle(RockDark, width * 0.13f, Offset(x, y))
        y += step
        k++
    }
    drawRoundRect(RockDark, Offset(left, top), Size(width, height), CornerRadius(width * 0.25f), style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawStar(c: Offset, r: Float, time: Float) {
    val path = Path()
    val spin = time * 2f
    for (k in 0 until 10) {
        val rr = if (k % 2 == 0) r else r * 0.45f
        val a = spin + k * PI.toFloat() / 5f - PI.toFloat() / 2f
        val p = Offset(c.x + rr * cos(a), c.y + rr * sin(a))
        if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, StarGold)
}
