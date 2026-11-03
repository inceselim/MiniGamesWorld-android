package com.eomer91.minigamesworld.ui.games.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Vektor cizim parcasi: SVG yol verisi + dolgu.
 * [fill] sabit renk ('#RRGGBB' / '#AARRGGBB') ya da renk yuvasi: main, skin, hair, eye, lips ...
 * Yuva adinin sonundaki 'D' koyu, 'L' acik tonu verir (mainD, skinL gibi).
 * Cizimler tools/vector betikleriyle uretilir.
 */
class VP(val d: String, val fill: String, val stroke: Boolean = true)

object VectorArt {
    private val paths = HashMap<String, Path>()
    private val fixed = HashMap<String, Color>()

    fun path(d: String): Path = paths.getOrPut(d) { PathParser().parsePathString(d).toPath() }

    /** Parcalarin toplam siniri (kucuk resimleri ortalamak icin). */
    fun bounds(parts: List<VP>): Rect {
        var r: Rect? = null
        for (p in parts) {
            val b = path(p.d).getBounds()
            r = r?.let { Rect(minOf(it.left, b.left), minOf(it.top, b.top), maxOf(it.right, b.right), maxOf(it.bottom, b.bottom)) } ?: b
        }
        return r ?: Rect.Zero
    }

    fun parseHex(h: String): Color = fixed.getOrPut(h) {
        val v = h.removePrefix("#").toLong(16)
        if (h.length == 9) Color(v) else Color(0xFF000000 or v)
    }

    /** f < 1 koyulastirir, f > 1 beyaza dogru acar. */
    fun shade(c: Color, f: Float): Color =
        if (f <= 1f) Color(c.red * f, c.green * f, c.blue * f, c.alpha)
        else {
            val t = f - 1f
            Color(c.red + (1 - c.red) * t, c.green + (1 - c.green) * t, c.blue + (1 - c.blue) * t, c.alpha)
        }

    /** Dolgu adini renge cevir; [slot] yuva adini (D/L soneki olmadan) renge esler. */
    fun resolve(fill: String, slot: (String) -> Color?): Color {
        if (fill.startsWith("#")) return parseHex(fill)
        slot(fill)?.let { return it }
        val base = fill.dropLast(1)
        val c = slot(base) ?: return Color.Magenta
        return when (fill.last()) {
            'D' -> shade(c, 0.78f)
            'L' -> shade(c, 1.35f)
            else -> c
        }
    }
}

/**
 * Parcalari ciz. (left, top) cizimin sol ustu, [scale] viewBox biriminden piksele carpan.
 * Her parca kendi renginin koyusuyla ince dis hat alir (cizgi film gorunumu).
 */
fun DrawScope.drawVector(
    parts: List<VP>,
    left: Float,
    top: Float,
    scale: Float,
    alpha: Float = 1f,
    strokeWidth: Float = 1.4f,
    slot: (String) -> Color?,
) {
    if (parts.isEmpty()) return
    withTransform({
        translate(left, top)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        for (p in parts) {
            val path = VectorArt.path(p.d)
            val color = VectorArt.resolve(p.fill, slot)
            drawPath(path, color, alpha = alpha)
            if (p.stroke) {
                drawPath(
                    path,
                    VectorArt.shade(color, 0.55f).copy(alpha = 1f),
                    alpha = alpha * 0.85f,
                    style = Stroke(strokeWidth, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/** Parcalari [dst] alanina sigdirip ortalayarak ciz (kucuk resimler icin). */
fun DrawScope.drawVectorFit(parts: List<VP>, dst: Rect, padding: Float = 0.1f, slot: (String) -> Color?) {
    val b = VectorArt.bounds(parts)
    if (b.width <= 0f || b.height <= 0f) return
    val avail = dst.deflate(minOf(dst.width, dst.height) * padding)
    val s = minOf(avail.width / b.width, avail.height / b.height)
    val left = avail.center.x - b.center.x * s
    val top = avail.center.y - b.center.y * s
    drawVector(parts, left, top, s, strokeWidth = 1.4f / s.coerceAtLeast(0.01f) * 1.2f, slot = slot)
}
