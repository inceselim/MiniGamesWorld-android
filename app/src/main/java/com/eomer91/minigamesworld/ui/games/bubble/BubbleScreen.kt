package com.eomer91.minigamesworld.ui.games.bubble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ads.rememberInterstitialLauncher
import com.eomer91.minigamesworld.ads.rememberRewardedLauncher
import com.eomer91.minigamesworld.ui.games.common.GameOverPanel
import com.eomer91.minigamesworld.ui.games.common.ScoreHeader
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.atan2

private val FieldColor = Color(0xFF020617)
private val DangerColor = Color(0xFFEF4444)

private val BubbleColors = listOf(
    Color(0xFFEF4444),
    Color(0xFFFACC15),
    Color(0xFF22C55E),
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
)

private fun colorOf(index: Int) = BubbleColors[index % BubbleColors.size]

@Composable
fun BubbleScreen(viewModel: BubbleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val engine = viewModel.engine
    val showInterstitial = rememberInterstitialLauncher()
    val restartWithAd = { showInterstitial { viewModel.restart() } }

    val showRewarded = rememberRewardedLauncher()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adNotReady: () -> Unit = { scope.launch { snackbar.showSnackbar(context.getString(R.string.ad_not_ready)) } }

    // Ucan balonun yol uzerindeki ilerlemesi (0..1)
    val flightProgress = remember { Animatable(0f) }
    LaunchedEffect(state.flying?.id) {
        val f = state.flying ?: return@LaunchedEffect
        flightProgress.snapTo(0f)
        flightProgress.animateTo(1f, tween(f.durationMs, easing = LinearEasing))
    }

    // Patlama efektlerinin ilerlemesi (id -> 0..1)
    val effectProgress = remember { mutableStateMapOf<Long, Float>() }
    state.effects.forEach { effect ->
        key(effect.id) {
            LaunchedEffect(Unit) {
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(BubbleViewModel.EFFECT_MS.toInt())) { effectProgress[effect.id] = value }
                effectProgress.remove(effect.id)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScoreHeader(score = state.score, best = state.best, onRestart = restartWithAd)

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.bubble_level, state.level), color = AppColors.Primary, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.bubble_new_row_in, state.shotsUntilNewRow),
                    color = if (state.shotsUntilNewRow <= 1) AppColors.Danger else AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                Modifier
                    .padding(top = 8.dp)
                    // Genislik yerine en-boy oranini korur; kucuk ekranlarda yukseklige gore kuculur.
                    .weight(1f, fill = false)
                    .aspectRatio(engine.width / engine.height)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FieldColor)
                    .pointerInput(state.gameOver) {
                        if (state.gameOver) return@pointerInput
                        detectTapGestures { offset ->
                            val unit = size.width / engine.width
                            val p = Vec(offset.x / unit, offset.y / unit)
                            if (p.dist(engine.shooter) < 0.9f || p.dist(nextPosition(engine)) < 0.8f) {
                                viewModel.swap()
                            } else if (p.y < engine.shooter.y - 0.3f) {
                                viewModel.shoot(angleTo(engine.shooter, p))
                            }
                        }
                    }
                    .pointerInput(state.gameOver) {
                        if (state.gameOver) return@pointerInput
                        var last: Float? = null
                        detectDragGestures(
                            onDragStart = { offset ->
                                val unit = size.width / engine.width
                                last = angleTo(engine.shooter, Vec(offset.x / unit, offset.y / unit))
                                viewModel.aim(last)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val unit = size.width / engine.width
                                last = angleTo(engine.shooter, Vec(change.position.x / unit, change.position.y / unit))
                                viewModel.aim(last)
                            },
                            onDragEnd = {
                                last?.let { viewModel.shoot(it) }
                                last = null
                            },
                            onDragCancel = {
                                viewModel.aim(null)
                                last = null
                            },
                        )
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val unit = size.width / engine.width
                    fun pt(v: Vec) = Offset(v.x * unit, v.y * unit)
                    val radius = unit * 0.47f

                    // Tehlike cizgisi
                    val dangerY = (0.5f + BubbleEngine.DANGER_ROW * BubbleEngine.ROW_H + 0.5f) * unit
                    drawLine(
                        DangerColor.copy(alpha = 0.6f),
                        Offset(0f, dangerY),
                        Offset(size.width, dangerY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    )

                    // Tahtadaki balonlar
                    state.board.cells.forEach { (slot, b) -> drawBubble(pt(engine.center(state.board, slot)), radius, colorOf(b.color)) }

                    // Patlama ve dusme efektleri
                    state.effects.forEach { effect ->
                        val t = effectProgress[effect.id] ?: 0f
                        effect.popped.forEach { (pos, c) ->
                            drawBubble(pt(pos), radius * (1f + t * 0.4f), colorOf(c), alpha = 1f - t)
                        }
                        effect.dropped.forEach { (pos, c) ->
                            drawBubble(pt(pos + Vec(0f, t * t * 8f)), radius, colorOf(c), alpha = 1f - t)
                        }
                    }

                    // Nisan cizgisi
                    val angle = state.aimAngle
                    if (angle != null && state.flying == null) {
                        val path = engine.aimPath(state.board, angle)
                        path.zipWithNext { a, b ->
                            drawLine(
                                Color.White.copy(alpha = 0.55f),
                                pt(a),
                                pt(b),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)),
                            )
                        }
                    }

                    // Ucan balon
                    state.flying?.let { f -> drawBubble(pt(pointAlong(f.path, flightProgress.value)), radius, colorOf(f.color)) }

                    // Atici ve siradaki balon
                    if (!state.gameOver) {
                        if (state.flying == null) drawBubble(pt(engine.shooter), radius, colorOf(state.current))
                        drawBubble(pt(nextPosition(engine)), radius * 0.7f, colorOf(state.next))
                    }
                }
            }

            if (state.gameOver) {
                if (!state.continueUsed) {
                    Button(
                        onClick = { showRewarded({ viewModel.continueAfterLoss() }, adNotReady) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold, contentColor = AppColors.OnPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.bubble_continue), fontWeight = FontWeight.Black)
                    }
                }
                GameOverPanel(
                    score = state.score,
                    isNewRecord = state.isNewRecord,
                    onRestart = restartWithAd,
                    goldEarned = state.goldEarned,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                Text(
                    stringResource(R.string.bubble_hint),
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** Siradaki balonun yeri: aticinin solunda. */
private fun nextPosition(engine: BubbleEngine) = engine.shooter + Vec(-1.6f, 0.15f)

/** Aticidan hedefe aci (0 = saga, PI/2 = yukari). */
private fun angleTo(from: Vec, to: Vec): Float = atan2(from.y - to.y, to.x - from.x)

/** Kirik cizgi uzerinde [t] (0..1) oranindaki nokta. */
private fun pointAlong(path: List<Vec>, t: Float): Vec {
    if (path.size < 2) return path.firstOrNull() ?: Vec(0f, 0f)
    val lengths = path.zipWithNext { a, b -> a.dist(b) }
    var remaining = lengths.sum() * t.coerceIn(0f, 1f)
    for (i in lengths.indices) {
        if (remaining <= lengths[i] || i == lengths.lastIndex) {
            val k = if (lengths[i] == 0f) 0f else (remaining / lengths[i]).coerceIn(0f, 1f)
            return path[i] + (path[i + 1] - path[i]) * k
        }
        remaining -= lengths[i]
    }
    return path.last()
}

private fun DrawScope.drawBubble(center: Offset, radius: Float, color: Color, alpha: Float = 1f) {
    val a = alpha.coerceIn(0f, 1f)
    drawCircle(color.copy(alpha = a), radius, center)
    drawCircle(Color.Black.copy(alpha = 0.18f * a), radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(radius * 0.08f))
    drawCircle(Color.White.copy(alpha = 0.4f * a), radius * 0.28f, center + Offset(-radius * 0.32f, -radius * 0.36f))
}
