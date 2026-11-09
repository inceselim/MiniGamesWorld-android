package com.eomer91.minigamesworld.ui.games.island.render

/**
 * Platformdan bagimsiz cizim arayuzu: ayni sanat kodu Android'de (Canvas) ve testte (AWT, onizleme PNG)
 * calisir. Renkler ARGB Int.
 */
interface IsoPainter {
    fun poly(xs: FloatArray, ys: FloatArray, color: Int)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int)
    fun oval(l: Float, t: Float, r: Float, b: Float, color: Int)
    fun ring(cx: Float, cy: Float, rx: Float, ry: Float, width: Float, color: Int)
}

object IsoColor {
    fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    fun shade(c: Int, f: Float): Int {
        val a = c ushr 24 and 0xFF
        val r = ((c shr 16 and 0xFF) * f).toInt().coerceIn(0, 255)
        val g = ((c shr 8 and 0xFF) * f).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) * f).toInt().coerceIn(0, 255)
        return argb(a, r, g, b)
    }
    fun mix(c1: Int, c2: Int, t: Float): Int {
        fun ch(s: Int) = ((c1 shr s and 0xFF) * (1 - t) + (c2 shr s and 0xFF) * t).toInt()
        return argb(ch(24), ch(16), ch(8), ch(0))
    }
    fun alpha(c: Int, a: Int) = (c and 0xFFFFFF) or (a shl 24)
}
