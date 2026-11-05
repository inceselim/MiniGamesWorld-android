package com.eomer91.minigamesworld.ui.games.mahjong

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.common.drawVector
import kotlin.math.ceil

/** Bolum temasi: tas govdesi ve arka plan renkleri. */
data class MjTheme(
    val key: String,
    @StringRes val name: Int,
    val face: Color,
    val face2: Color,
    val back: Color,
    val edge: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
)

val MJ_THEMES = listOf(
    MjTheme("classic", R.string.mj_theme_classic, Color(0xFFFBF5E6), Color(0xFFEFE3C6), Color(0xFF2F8F5B), Color(0xFFD9C9A3), Color(0xFF2F6B4F), Color(0xFF143426), Color(0xFFFFD166)),
    MjTheme("jade", R.string.mj_theme_jade, Color(0xFFF1FAF2), Color(0xFFD8EEDC), Color(0xFF13785A), Color(0xFFB9D9C0), Color(0xFF3E8E6E), Color(0xFF173F31), Color(0xFFB9F6CA)),
    MjTheme("ocean", R.string.mj_theme_ocean, Color(0xFFF2F8FC), Color(0xFFD9E9F4), Color(0xFF1D5FA0), Color(0xFFB7CFE2), Color(0xFF2B6CB0), Color(0xFF0D2745), Color(0xFF7DD3FC)),
    MjTheme("forest", R.string.mj_theme_forest, Color(0xFFF8EBD2), Color(0xFFE9D2A8), Color(0xFF8A5A2E), Color(0xFFCFAF7C), Color(0xFF7A5232), Color(0xFF2E1D10), Color(0xFFFFCC80)),
    MjTheme("fruit", R.string.mj_theme_fruit, Color(0xFFFFF3F5), Color(0xFFFBDDE4), Color(0xFFE0567F), Color(0xFFEDB8C6), Color(0xFFC24F7C), Color(0xFF4A1C36), Color(0xFFFFB3C7)),
    MjTheme("night", R.string.mj_theme_night, Color(0xFFF4F1FF), Color(0xFFDCD6F5), Color(0xFF2C2F7A), Color(0xFFE3B341), Color(0xFF2A2F6E), Color(0xFF080B24), Color(0xFFE3B341)),
)

/**
 * Tas olculeri "birim" cinsinden: yuz 100 x 130; govde sol-alta dogru (8, 9) kalinlikta.
 * Ust katlar (+8, -9) kayar. Bir yarim-izgara adimi x'te 50, y'de 65 birimdir.
 */
object MjGeo {
    const val FACE_W = 100f
    const val FACE_H = 130f
    const val STEP_X = 50f
    const val STEP_Y = 65f
    const val LAYER_X = 8f
    const val LAYER_Y = -9f
    /** Onbellek resminin kapsadigi alan (golge + govde dahil). */
    const val PAD_L = 14f
    const val PAD_T = 6f
    const val ART_W = 120f
    const val ART_H = 152f
    const val RADIUS = 12f
}

/** Tas resimleri onbellegi: her (tema, yuz) icin govde+yuz bir kez cizilir, sonra olceklenerek kullanilir. */
class MjTileCache {
    private var themeKey = ""
    private var k = 0f
    private val images = HashMap<Int, ImageBitmap>()

    /** [pxPerUnit] cizim cozunurlugu (birim basina piksel). */
    fun get(theme: Int, face: Int, pxPerUnit: Float): ImageBitmap {
        val t = MJ_THEMES[theme]
        val kk = (ceil(pxPerUnit * 4f) / 4f).coerceIn(0.25f, 3f)
        if (t.key != themeKey || kk != k) {
            images.clear(); themeKey = t.key; k = kk
        }
        return images.getOrPut(face) { render(t, MahjongArt.sets.getValue(t.key).getOrNull(face), kk) }
    }

    private fun render(t: MjTheme, parts: List<com.eomer91.minigamesworld.ui.games.common.VP>?, k: Float): ImageBitmap {
        val w = ceil(MjGeo.ART_W * k).toInt()
        val h = ceil(MjGeo.ART_H * k).toInt()
        val bmp = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bmp), Size(w.toFloat(), h.toFloat())) {
            val ox = MjGeo.PAD_L * k
            val oy = MjGeo.PAD_T * k
            drawTileBody(t, ox, oy, k)
            if (parts != null) drawVector(parts, ox, oy, k, strokeWidth = 1.2f) { null }
        }
        return bmp
    }
}

/** Govde: golge, renkli arka katman, kenar, gradyanli yuz, ic parlaklik. (ox, oy) yuzun sol ustu. */
fun DrawScope.drawTileBody(t: MjTheme, ox: Float, oy: Float, k: Float) {
    fun rr(x0: Float, y0: Float, x1: Float, y1: Float, c: Color, rad: Float = MjGeo.RADIUS + 1) =
        drawRoundRect(c, Offset(ox + x0 * k, oy + y0 * k), Size((x1 - x0) * k, (y1 - y0) * k), CornerRadius(rad * k))
    rr(-11f, 13f, 89f, 143f, Color.Black.copy(alpha = 0.24f))
    rr(-8f, 9f, 92f, 139f, t.back)
    rr(-4.5f, 4.5f, 95.5f, 134.5f, t.edge)
    drawRoundRect(
        Brush.linearGradient(listOf(t.face, t.face2), start = Offset(ox, oy), end = Offset(ox + 30f * k, oy + 130f * k)),
        Offset(ox, oy), Size(MjGeo.FACE_W * k, MjGeo.FACE_H * k), CornerRadius(MjGeo.RADIUS * k),
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.8f), Offset(ox + 1.5f * k, oy + 1.5f * k), Size(97f * k, 127f * k),
        CornerRadius((MjGeo.RADIUS - 1) * k), style = Stroke(2f * k),
    )
}
