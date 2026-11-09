package com.eomer91.minigamesworld.ui.games.island.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** IsoPainter'in Android Canvas uygulamasi. Bir ornek tek is parcaciginda kullanilmali. */
class AndroidIsoPainter(antiAlias: Boolean = true) : IsoPainter {
    var canvas: Canvas? = null
    private val fill = Paint().apply { style = Paint.Style.FILL; isAntiAlias = antiAlias }
    private val stroke = Paint().apply { style = Paint.Style.STROKE; isAntiAlias = antiAlias; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()

    override fun poly(xs: FloatArray, ys: FloatArray, color: Int) {
        val c = canvas ?: return
        path.rewind()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until xs.size) path.lineTo(xs[i], ys[i])
        path.close()
        fill.color = color
        c.drawPath(path, fill)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        val c = canvas ?: return
        stroke.color = color; stroke.strokeWidth = width
        c.drawLine(x1, y1, x2, y2, stroke)
    }

    override fun oval(l: Float, t: Float, r: Float, b: Float, color: Int) {
        val c = canvas ?: return
        fill.color = color
        c.drawOval(l, t, r, b, fill)
    }

    override fun ring(cx: Float, cy: Float, rx: Float, ry: Float, width: Float, color: Int) {
        val c = canvas ?: return
        stroke.color = color; stroke.strokeWidth = width
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, stroke)
    }
}
