package com.eomer91.minigamesworld.ui.games.coloring

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
fun ColoringScreen(viewModel: ColoringViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Surface),
    ) {
        if (state.page == null) PagePicker(state, viewModel) else Editor(state, viewModel)
    }
}

@Composable
private fun PagePicker(state: ColoringUiState, viewModel: ColoringViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text(stringResource(R.string.co_choose), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 10.dp))
        COLORING_PAGES.indices.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { i ->
                    val (name, res) = COLORING_PAGES[i]
                    val savedBmp = remember(state.saved, name) {
                        if (name in state.saved) BitmapFactory.decodeFile(viewModel.savedFile(name).absolutePath)?.asImageBitmap() else null
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, if (savedBmp != null) AppColors.Primary else Color(0x22000000), RoundedCornerShape(16.dp))
                            .clickable { viewModel.open(i) },
                    ) {
                        if (savedBmp != null) {
                            Image(savedBmp, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        } else {
                            Image(painterResource(res), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Editor(state: ColoringUiState, viewModel: ColoringViewModel) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolButton("←") { viewModel.close() }
            Spacer(Modifier.weight(1f))
            if (scale > 1.01f) ToolButton("⤢") { scale = 1f; offset = Offset.Zero }
            ToolButton("↶", enabled = state.canUndo) { viewModel.undo() }
            ToolButton("🗑") { viewModel.clearPage() }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val img = state.image
            if (img != null) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val ns = (scale * zoom).coerceIn(1f, 5f)
                                // Parmaklar arasindaki nokta sabit kalsin
                                val cp = (centroid - offset) / scale
                                val o = centroid - cp * ns + pan
                                scale = ns
                                offset = if (ns <= 1.001f) Offset.Zero else Offset(
                                    o.x.coerceIn(size.width * (1 - ns), 0f),
                                    o.y.coerceIn(size.height * (1 - ns), 0f),
                                )
                            }
                        }
                        .pointerInput(img) {
                            detectTapGestures { p ->
                                val s = minOf(size.width, size.height).toFloat()
                                val left = (size.width - s) / 2f
                                val top = (size.height - s) / 2f
                                // Ekran -> tuval (yakinlastirma geri alinir) -> resim
                                val cx = (p.x - offset.x) / scale
                                val cy = (p.y - offset.y) / scale
                                val ix = ((cx - left) / s * img.width).roundToInt()
                                val iy = ((cy - top) / s * img.height).roundToInt()
                                viewModel.tap(ix, iy)
                            }
                        },
                ) {
                    @Suppress("UNUSED_VARIABLE")
                    val v = state.version
                    val s = minOf(size.width, size.height)
                    withTransform({
                        translate(offset.x, offset.y)
                        scale(scale, scale, pivot = Offset.Zero)
                    }) {
                        drawImage(
                            img, IntOffset.Zero, IntSize(img.width, img.height),
                            IntOffset(((size.width - s) / 2).roundToInt(), ((size.height - s) / 2).roundToInt()), IntSize(s.roundToInt(), s.roundToInt()),
                            filterQuality = FilterQuality.Medium,
                        )
                    }
                }
            }
        }
        // Palet: iki satir
        COLORING_PALETTE.chunked(12).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { c ->
                    val sel = c == state.color
                    Box(
                        Modifier
                            .size(if (sel) 28.dp else 24.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (sel) 3.dp else 1.dp, if (sel) AppColors.Primary else Color(0x44000000), CircleShape)
                            .clickable { viewModel.pickColor(c) },
                    )
                }
            }
        }
        Text(stringResource(R.string.co_hint), color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ToolButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        color = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary.copy(alpha = 0.4f),
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
