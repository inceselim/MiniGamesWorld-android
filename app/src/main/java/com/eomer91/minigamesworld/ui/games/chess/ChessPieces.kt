package com.eomer91.minigamesworld.ui.games.chess

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs

/*
 * Satranc taslari: emoji/font farkliliklarina takilmamak icin basit vektor sekiller.
 * Koordinatlar karenin icinde 0..1 arasidir (y asagi). tools/covers/chess.py ayni sekilleri kullanir.
 */

private sealed interface Shape
private class Poly(val pts: List<Pair<Float, Float>>) : Shape
private class Circle(val cx: Float, val cy: Float, val r: Float, val detail: Boolean = false) : Shape
private class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : Shape
private class Rect(val x0: Float, val y0: Float, val x1: Float, val y1: Float) : Shape
private class DetailLine(val x0: Float, val y0: Float, val x1: Float, val y1: Float) : Shape

private val BASE = listOf(Rect(0.20f, 0.80f, 0.80f, 0.90f), Rect(0.26f, 0.75f, 0.74f, 0.81f))

private fun poly(vararg p: Float) = Poly(p.toList().chunked(2) { it[0] to it[1] })

private val SHAPES: Map<Int, List<Shape>> = mapOf(
    Chess.PAWN to BASE + listOf(
        poly(0.33f, 0.76f, 0.41f, 0.55f, 0.59f, 0.55f, 0.67f, 0.76f),
        Oval(0.5f, 0.55f, 0.15f, 0.04f),
        Circle(0.5f, 0.40f, 0.125f),
    ),
    Chess.ROOK to BASE + listOf(
        poly(0.30f, 0.76f, 0.33f, 0.42f, 0.67f, 0.42f, 0.70f, 0.76f),
        Rect(0.28f, 0.38f, 0.72f, 0.44f),
        poly(
            0.25f, 0.40f, 0.25f, 0.20f, 0.36f, 0.20f, 0.36f, 0.28f, 0.445f, 0.28f, 0.445f, 0.20f,
            0.555f, 0.20f, 0.555f, 0.28f, 0.64f, 0.28f, 0.64f, 0.20f, 0.75f, 0.20f, 0.75f, 0.40f,
        ),
    ),
    Chess.BISHOP to BASE + listOf(
        poly(0.34f, 0.76f, 0.43f, 0.58f, 0.57f, 0.58f, 0.66f, 0.76f),
        Oval(0.5f, 0.58f, 0.17f, 0.045f),
        Oval(0.5f, 0.40f, 0.145f, 0.19f),
        Circle(0.5f, 0.165f, 0.055f),
        DetailLine(0.56f, 0.30f, 0.46f, 0.43f),
    ),
    Chess.QUEEN to BASE + listOf(
        poly(0.31f, 0.76f, 0.39f, 0.50f, 0.61f, 0.50f, 0.69f, 0.76f),
        Oval(0.5f, 0.50f, 0.20f, 0.05f),
        poly(
            0.27f, 0.50f, 0.19f, 0.25f, 0.33f, 0.40f, 0.35f, 0.19f, 0.45f, 0.38f, 0.5f, 0.14f,
            0.55f, 0.38f, 0.65f, 0.19f, 0.67f, 0.40f, 0.81f, 0.25f, 0.73f, 0.50f,
        ),
        Circle(0.19f, 0.23f, 0.045f), Circle(0.35f, 0.17f, 0.045f), Circle(0.5f, 0.12f, 0.045f),
        Circle(0.65f, 0.17f, 0.045f), Circle(0.81f, 0.23f, 0.045f),
    ),
    Chess.KING to BASE + listOf(
        poly(0.31f, 0.76f, 0.38f, 0.48f, 0.62f, 0.48f, 0.69f, 0.76f),
        Oval(0.5f, 0.48f, 0.20f, 0.05f),
        poly(0.29f, 0.49f, 0.24f, 0.36f, 0.30f, 0.28f, 0.42f, 0.27f, 0.5f, 0.32f, 0.58f, 0.27f, 0.70f, 0.28f, 0.76f, 0.36f, 0.71f, 0.49f),
        Rect(0.465f, 0.07f, 0.535f, 0.30f),
        Rect(0.40f, 0.125f, 0.60f, 0.19f),
    ),
    Chess.KNIGHT to BASE + listOf(
        poly(
            0.29f, 0.76f, 0.31f, 0.62f, 0.40f, 0.50f, 0.40f, 0.47f, 0.30f, 0.50f, 0.22f, 0.55f, 0.15f, 0.49f,
            0.17f, 0.42f, 0.30f, 0.30f, 0.38f, 0.20f, 0.40f, 0.11f, 0.47f, 0.17f, 0.58f, 0.16f, 0.70f, 0.25f,
            0.77f, 0.45f, 0.75f, 0.76f,
        ),
        Circle(0.40f, 0.29f, 0.028f, detail = true),
        DetailLine(0.68f, 0.30f, 0.72f, 0.52f),
    ),
)

private val WhiteFill = Color(0xFFFAF6EC)
private val WhiteLine = Color(0xFF1E1E1E)
private val BlackFill = Color(0xFF303036)
private val BlackLine = Color(0xFF0A0A0C)

/** [v] tas kodu (isaretli). (left, top) karenin sol ustu, [s] kare boyu. */
fun DrawScope.drawChessPiece(v: Int, left: Float, top: Float, s: Float, alpha: Float = 1f) {
    val shapes = SHAPES[abs(v)] ?: return
    val white = v > 0
    val fill = if (white) WhiteFill else BlackFill
    val line = if (white) WhiteLine else BlackLine
    val detail = if (white) WhiteLine else Color(0xFFDDDDDD)
    val w = s * 0.035f
    fun X(x: Float) = left + x * s
    fun Y(y: Float) = top + y * s
    fun path(p: Poly) = Path().apply {
        moveTo(X(p.pts[0].first), Y(p.pts[0].second))
        for (k in 1 until p.pts.size) lineTo(X(p.pts[k].first), Y(p.pts[k].second))
        close()
    }
    // 1) Dis hat: tum sekiller kalin cizgiyle  2) Dolgu: kesisimlerde ic cizgi kalmaz
    for (pass in 0..1) {
        val color = if (pass == 0) line else fill
        val ext = if (pass == 0) w else 0f
        for (sh in shapes) {
            when (sh) {
                is Poly -> {
                    val p = path(sh)
                    if (pass == 0) drawPath(p, line, alpha = alpha, style = Stroke(w * 2, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(p, color, alpha = alpha)
                }
                is Circle -> if (!sh.detail) drawCircle(color, sh.r * s + ext, Offset(X(sh.cx), Y(sh.cy)), alpha = alpha)
                is Oval -> drawOval(
                    color,
                    Offset(X(sh.cx - sh.rx) - ext, Y(sh.cy - sh.ry) - ext),
                    Size(sh.rx * 2 * s + 2 * ext, sh.ry * 2 * s + 2 * ext),
                    alpha = alpha,
                )
                is Rect -> drawRect(
                    color,
                    Offset(X(sh.x0) - ext, Y(sh.y0) - ext),
                    Size((sh.x1 - sh.x0) * s + 2 * ext, (sh.y1 - sh.y0) * s + 2 * ext),
                    alpha = alpha,
                )
                is DetailLine -> Unit
            }
        }
    }
    for (sh in shapes) {
        when (sh) {
            is DetailLine -> drawLine(detail, Offset(X(sh.x0), Y(sh.y0)), Offset(X(sh.x1), Y(sh.y1)), w, cap = StrokeCap.Round, alpha = alpha)
            is Circle -> if (sh.detail) drawCircle(detail, sh.r * s, Offset(X(sh.cx), Y(sh.cy)), alpha = alpha)
            else -> Unit
        }
    }
}
