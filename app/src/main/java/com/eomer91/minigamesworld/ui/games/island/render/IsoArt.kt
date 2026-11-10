package com.eomer91.minigamesworld.ui.games.island.render

import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.Terrain
import com.eomer91.minigamesworld.ui.games.island.render.IsoColor.alpha
import com.eomer91.minigamesworld.ui.games.island.render.IsoColor.mix
import com.eomer91.minigamesworld.ui.games.island.render.IsoColor.shade
import kotlin.math.cos
import kotlin.math.sin

/**
 * Izometrik sanat: zemin karolari ve bina cizimleri (vektorel, platformdan bagimsiz).
 * Karo (x, y) -> ekran: sx = (x - y) * tw/2, sy = (x + y) * th/2. Yukseklik z, tw=64 olcegindeki piksel.
 */
class Iso(val p: IsoPainter, val ox: Float, val oy: Float, val tw: Float, val th: Float) {
    val zs = tw / 64f

    fun sx(x: Float, y: Float) = ox + (x - y) * tw / 2f
    fun sy(x: Float, y: Float, z: Float) = oy + (x + y) * th / 2f - z * zs

    fun quad(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, c: Int) {
        p.poly(floatArrayOf(sx(x1, y1), sx(x2, y2), sx(x3, y3), sx(x4, y4)), floatArrayOf(sy(x1, y1, z1), sy(x2, y2, z2), sy(x3, y3, z3), sy(x4, y4, z4)), c)
    }

    fun tri(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, c: Int) {
        p.poly(floatArrayOf(sx(x1, y1), sx(x2, y2), sx(x3, y3)), floatArrayOf(sy(x1, y1, z1), sy(x2, y2, z2), sy(x3, y3, z3)), c)
    }

    /** Duz zemin dortgeni */
    fun flat(x: Float, y: Float, w: Float, d: Float, z: Float, c: Int) = quad(x, y, z, x + w, y, z, x + w, y + d, z, x, y + d, z, c)

    /** Kutu: x,y koseden w (x yonu), d (y yonu), z0'dan h yukseklik */
    fun box(x: Float, y: Float, w: Float, d: Float, z0: Float, h: Float, c: Int, topC: Int = shade(c, 1.12f)) {
        val z1 = z0 + h
        quad(x + w, y, z0, x + w, y + d, z0, x + w, y + d, z1, x + w, y, z1, shade(c, 0.86f)) // sag yuz
        quad(x, y + d, z0, x + w, y + d, z0, x + w, y + d, z1, x, y + d, z1, shade(c, 0.68f)) // sol yuz
        quad(x, y, z1, x + w, y, z1, x + w, y + d, z1, x, y + d, z1, topC)
    }

    /** Sag yuzde (x = xf) pencere sirasi */
    fun windowsR(xf: Float, y0: Float, y1: Float, z0: Float, floors: Int, fh: Float, c: Int, per: Int = 3) {
        val span = y1 - y0
        val n = (span * per).toInt().coerceAtLeast(1)
        for (f in 0 until floors) for (i in 0 until n) {
            val a = y0 + span * (i + 0.25f) / n; val b = y0 + span * (i + 0.75f) / n
            val za = z0 + f * fh + fh * 0.3f; val zb = za + fh * 0.45f
            quad(xf, a, za, xf, b, za, xf, b, zb, xf, a, zb, c)
        }
    }

    /** Sol yuzde (y = yf) pencere sirasi */
    fun windowsL(yf: Float, x0: Float, x1: Float, z0: Float, floors: Int, fh: Float, c: Int, per: Int = 3) {
        val span = x1 - x0
        val n = (span * per).toInt().coerceAtLeast(1)
        for (f in 0 until floors) for (i in 0 until n) {
            val a = x0 + span * (i + 0.25f) / n; val b = x0 + span * (i + 0.75f) / n
            val za = z0 + f * fh + fh * 0.3f; val zb = za + fh * 0.45f
            quad(a, yf, za, b, yf, za, b, yf, zb, a, yf, zb, c)
        }
    }

    /** Sirti x yonunde uzanan beșik cati */
    fun gableX(x: Float, y: Float, w: Float, d: Float, z: Float, rh: Float, c: Int) {
        val ym = y + d / 2f
        quad(x, y, z, x + w, y, z, x + w, ym, z + rh, x, ym, z + rh, shade(c, 0.8f))
        quad(x, y + d, z, x + w, y + d, z, x + w, ym, z + rh, x, ym, z + rh, c)
        tri(x + w, y, z, x + w, y + d, z, x + w, ym, z + rh, shade(c, 0.9f))
    }

    /** Sirti y yonunde uzanan besik cati */
    fun gableY(x: Float, y: Float, w: Float, d: Float, z: Float, rh: Float, c: Int) {
        val xm = x + w / 2f
        quad(x, y, z, x, y + d, z, xm, y + d, z + rh, xm, y, z + rh, shade(c, 0.8f))
        quad(x + w, y, z, x + w, y + d, z, xm, y + d, z + rh, xm, y, z + rh, shade(c, 1.05f))
        tri(x, y + d, z, x + w, y + d, z, xm, y + d, z + rh, shade(c, 0.75f))
    }

    fun pyramid(x: Float, y: Float, w: Float, d: Float, z: Float, rh: Float, c: Int) {
        val cx = x + w / 2; val cy = y + d / 2
        tri(x + w, y, z, x + w, y + d, z, cx, cy, z + rh, shade(c, 0.9f))
        tri(x, y + d, z, x + w, y + d, z, cx, cy, z + rh, shade(c, 0.72f))
    }

    fun pole(x: Float, y: Float, z0: Float, h: Float, c: Int, wpx: Float = 1.5f) =
        p.line(sx(x, y), sy(x, y, z0), sx(x, y), sy(x, y, z0 + h), wpx * zs, c)

    fun flag(x: Float, y: Float, z: Float, c: Int) {
        pole(x, y, z, 26f, 0xFF5D4037.toInt(), 1.5f)
        val px = sx(x, y); val py = sy(x, y, z + 26f)
        p.poly(floatArrayOf(px, px + 11 * zs, px), floatArrayOf(py, py + 4 * zs, py + 8 * zs), c)
    }

    fun palm(x: Float, y: Float, z: Float = 0f, s: Float = 1f) {
        val bx = sx(x, y); val by = sy(x, y, z)
        val tx = bx + 4 * zs * s; val ty = by - 30 * zs * s
        p.line(bx, by, (bx + tx) / 2 + 2 * zs * s, (by + ty) / 2, 3 * zs * s, 0xFF8D6E63.toInt())
        p.line((bx + tx) / 2 + 2 * zs * s, (by + ty) / 2, tx, ty, 2.5f * zs * s, 0xFF8D6E63.toInt())
        val green = 0xFF2E8B3A.toInt()
        for (k in 0 until 6) {
            val a = k * 1.047f + 0.3f
            val ex = tx + cos(a) * 13 * zs * s; val ey = ty + sin(a) * 6 * zs * s + 4 * zs * s
            p.line(tx, ty, ex, ey, 3 * zs * s, if (k % 2 == 0) green else shade(green, 1.25f))
        }
    }

    fun roundTree(x: Float, y: Float, z: Float = 0f, s: Float = 1f, c: Int = 0xFF3E8E41.toInt()) {
        val bx = sx(x, y); val by = sy(x, y, z)
        p.line(bx, by, bx, by - 8 * zs * s, 2.5f * zs * s, 0xFF6D4C41.toInt())
        p.oval(bx - 8 * zs * s, by - 22 * zs * s, bx + 8 * zs * s, by - 6 * zs * s, shade(c, 0.85f))
        p.oval(bx - 6 * zs * s, by - 22 * zs * s, bx + 5 * zs * s, by - 11 * zs * s, shade(c, 1.15f))
    }

    fun coneTree(x: Float, y: Float, z: Float = 0f, s: Float = 1f) {
        val bx = sx(x, y); val by = sy(x, y, z)
        val c = 0xFF2F6B2F.toInt()
        p.poly(floatArrayOf(bx - 7 * zs * s, bx, bx + 7 * zs * s), floatArrayOf(by - 3 * zs * s, by - 26 * zs * s, by - 3 * zs * s), c)
        p.poly(floatArrayOf(bx, bx, bx + 7 * zs * s), floatArrayOf(by - 3 * zs * s, by - 26 * zs * s, by - 3 * zs * s), shade(c, 0.8f))
    }

    fun umbrella(x: Float, y: Float, c: Int, z: Float = 0f) {
        pole(x, y, z, 12f, 0xFFEEEEEE.toInt(), 1.2f)
        val px = sx(x, y); val py = sy(x, y, z + 12f)
        p.oval(px - 7 * zs, py - 3 * zs, px + 7 * zs, py + 3 * zs, c)
        p.oval(px - 3 * zs, py - 2.5f * zs, px + 3 * zs, py + 0.5f * zs, shade(c, 1.25f))
    }

    fun sunbed(x: Float, y: Float) = flat(x, y, 0.28f, 0.12f, 1.5f, 0xFFFFFFFF.toInt())

    fun pool(x: Float, y: Float, w: Float, d: Float, z: Float = 0f) {
        flat(x, y, w, d, z + 0.5f, 0xFFE0E0E0.toInt())
        flat(x + 0.06f, y + 0.06f, w - 0.12f, d - 0.12f, z + 0.6f, 0xFF29B6F6.toInt())
        flat(x + 0.15f, y + 0.12f, (w - 0.3f) * 0.5f, 0.06f, z + 0.7f, 0xFF81D4FA.toInt())
    }
}

object IsoArt {

    private fun hash(x: Int, y: Int): Int {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return h xor (h ushr 16)
    }

    val WATER = 0xFF2E86C1.toInt()
    val DEEP = 0xFF1A5B96.toInt()
    val SHALLOW = 0xFF4FC3D9.toInt()
    val SAND = 0xFFF2DBA2.toInt()
    val GRASS = 0xFF6BAF46.toInt()
    val FOREST = 0xFF4F9338.toInt()
    val HILL = 0xFF8AA35A.toInt()

    /**
     * Zemin karosu. (ox, oy) karonun ust kosesi. landNear: suyun yanindaki kara sayisi (siglik icin),
     * waterNear: karanin yanindaki su sayisi (kopuk/kiyi icin).
     */
    fun tile(p: IsoPainter, ox: Float, oy: Float, tw: Float, th: Float, t: Terrain, x: Int, y: Int, landNear: Int, waterNear: Int, grow: Float = 0f) {
        val h = hash(x, y)
        // Yumusak, karodan karoya surekli renk dalgalanmasi (dama gorunumu olmasin)
        val v = 1f + 0.035f * sin(x * 0.55f + y * 0.21f) + 0.025f * sin(y * 0.47f - x * 0.33f + 1.7f) + ((h and 0xFF) / 255f - 0.5f) * 0.02f
        val base = when (t) {
            Terrain.DEEP -> DEEP
            Terrain.WATER -> if (landNear > 0) mix(WATER, SHALLOW, (landNear / 4f).coerceAtMost(0.85f)) else WATER
            Terrain.SAND -> SAND
            Terrain.GRASS -> GRASS
            Terrain.FOREST -> FOREST
            Terrain.HILL -> HILL
        }
        val c = shade(base, v)
        // grow: karolar arasinda kenar yumusatma cizgisi kalmasin diye taban biraz buyutulur
        val g = grow; val gy = grow / 2f
        val xs = floatArrayOf(ox, ox + tw / 2 + g, ox, ox - tw / 2 - g)
        val ys = floatArrayOf(oy - gy, oy + th / 2, oy + th + gy, oy + th / 2)
        p.poly(xs, ys, c)
        when (t) {
            Terrain.WATER, Terrain.DEEP -> {
                // Kucuk dalga cizgisi
                if ((h ushr 8) % 3 == 0) {
                    val cx = ox + ((h ushr 12) % 9 - 4) * tw / 24; val cy = oy + th / 2
                    p.line(cx - tw / 10, cy, cx + tw / 10, cy, th / 16, alpha(0xFFFFFF, 70))
                }
            }
            Terrain.SAND -> {
                if (waterNear > 0) p.poly(floatArrayOf(ox, ox + tw / 2, ox + tw / 2 - tw / 10, ox), floatArrayOf(oy + th, oy + th / 2, oy + th / 2 + th / 20, oy + th - th / 10), alpha(0xFFFFFF, 60))
                if ((h ushr 9) % 4 == 0) p.oval(ox - tw / 20, oy + th * 0.45f, ox + tw / 20, oy + th * 0.55f, shade(SAND, 0.85f))
            }
            Terrain.GRASS -> {
                if ((h ushr 9) % 5 == 0) {
                    val cx = ox + ((h ushr 13) % 7 - 3) * tw / 16
                    p.line(cx, oy + th * 0.6f, cx - tw / 40, oy + th * 0.45f, tw / 64, shade(GRASS, 0.75f))
                    p.line(cx, oy + th * 0.6f, cx + tw / 40, oy + th * 0.47f, tw / 64, shade(GRASS, 0.75f))
                }
                if ((h ushr 11) % 13 == 0) p.oval(ox - tw / 32, oy + th * 0.4f, ox + tw / 32, oy + th * 0.5f, if (h and 1 == 0) 0xFFFFEB3B.toInt() else 0xFFFFFFFF.toInt())
            }
            Terrain.HILL -> {
                // Kaya ve kuru cali
                val k = (h ushr 9) % 6
                if (k == 0) {
                    val cx = ox + ((h ushr 12) % 5 - 2) * tw / 14; val cy = oy + th * 0.5f
                    p.oval(cx - tw / 9, cy - th / 5, cx + tw / 9, cy + th / 6, 0xFF8D8D85.toInt())
                    p.oval(cx - tw / 14, cy - th / 5, cx + tw / 30, cy, 0xFFB0B0A8.toInt())
                } else if (k == 1) {
                    val cx = ox + ((h ushr 12) % 5 - 2) * tw / 14; val cy = oy + th * 0.55f
                    p.oval(cx - tw / 12, cy - th / 4, cx + tw / 12, cy + th / 10, shade(HILL, 0.75f))
                }
            }
            Terrain.FOREST -> {
                val iso = Iso(p, ox, oy, tw, th)
                val k = 2 + (h ushr 10) % 2
                for (i in 0 until k) {
                    val fx = 0.25f + ((h ushr (12 + i * 3)) % 5) * 0.12f
                    val fy = 0.25f + ((h ushr (14 + i * 3)) % 5) * 0.12f
                    if ((h ushr (20 + i)) and 1 == 0) iso.roundTree(fx, fy, 0f, 0.7f) else iso.coneTree(fx, fy, 0f, 0.75f)
                }
            }
        }
    }

    // ------------------------------------------------------------------ binalar

    /** Sprite tuvalinin boyutu (tw olcegine gore): genislik, yukseklik, ayak izinin ust kosesinin konumu */
    class SpriteBox(val w: Float, val h: Float, val ox: Float, val oy: Float)

    fun spriteBox(t: BType, tw: Float, th: Float): SpriteBox {
        val top = height(t.key) * tw / 64f + 10f * tw / 64f
        // Sag ve altta golge payi
        val w = (t.w + t.h) * tw / 2f + 0.7f * tw / 2f + 4f
        val h = (t.w + t.h) * th / 2f + 1.4f * th / 2f + top + 4f
        return SpriteBox(w, h, t.h * tw / 2f + 2f, top + 2f)
    }

    /** Binanin zemin ustundeki en yuksek noktasi (tw=64 pikseli) */
    fun height(key: String): Float = when (key) {
        "tent" -> 22f; "pension" -> 54f; "bungalow" -> 40f; "hotel3" -> 86f; "hotel4" -> 140f; "resort" -> 210f; "villa" -> 52f
        "kiosk" -> 34f; "fastfood" -> 56f; "restaurant" -> 52f; "finedining" -> 58f; "pub" -> 48f; "disco" -> 66f
        "arcade" -> 50f; "funfair" -> 150f; "waterpark" -> 110f; "casino" -> 120f; "souvenir" -> 36f; "market" -> 44f
        "mall" -> 70f; "beachclub" -> 22f; "park" -> 30f; "palm" -> 42f; "garden" -> 14f; "road" -> 2f
        "airport" -> 80f; "harbor" -> 60f; "motel" -> 64f; else -> 40f
    }

    /**
     * Bina cizimi. (ox, oy): ayak izinin ust kosesi. accent: sirket rengi (cati/tente).
     */
    fun building(p: IsoPainter, t: BType, ox: Float, oy: Float, tw: Float, th: Float, accent: Int) {
        val s = Iso(p, ox, oy, tw, th)
        val W = t.w.toFloat(); val D = t.h.toFloat()
        val white = 0xFFF5F5F0.toInt(); val glass = 0xFF4FC3F7.toInt(); val darkGlass = 0xFF0D47A1.toInt()
        val win = 0xFF37474F.toInt(); val winLit = 0xFFFFE082.toInt()
        val paving = 0xFFBDB7A6.toInt()
        shadow(s, t, W, D)
        when (t.key) {
            "tent" -> {
                s.flat(0.1f, 0.1f, W - 0.2f, D - 0.2f, 0.3f, 0xFF8BC34A.toInt())
                val cols = intArrayOf(accent, 0xFFFF7043.toInt(), 0xFF26A69A.toInt(), 0xFFFFCA28.toInt())
                val spots = listOf(0.35f to 0.35f, 1.15f to 0.3f, 0.3f to 1.15f, 1.15f to 1.15f)
                for ((i, sp) in spots.withIndex()) {
                    val (x, y) = sp
                    val c = cols[i]
                    s.tri(x, y + 0.55f, 0f, x + 0.55f, y + 0.55f, 0f, x + 0.275f, y + 0.275f, 20f, shade(c, 0.75f))
                    s.tri(x + 0.55f, y, 0f, x + 0.55f, y + 0.55f, 0f, x + 0.275f, y + 0.275f, 20f, c)
                }
                // Kamp atesi
                s.p.oval(s.sx(1f, 1f) - 3 * s.zs, s.sy(1f, 1f, 0f) - 2 * s.zs, s.sx(1f, 1f) + 3 * s.zs, s.sy(1f, 1f, 0f) + 2 * s.zs, 0xFFFF7043.toInt())
                s.flag(0.15f, 0.15f, 0f, accent)
            }
            "pension" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF9CCC65.toInt())
                s.box(0.3f, 0.3f, 1.4f, 1.2f, 0f, 30f, 0xFFFFF3E0.toInt())
                s.windowsR(1.7f, 0.3f, 1.5f, 0f, 2, 14f, win, 2)
                s.windowsL(1.5f, 0.3f, 1.7f, 0f, 2, 14f, win, 2)
                s.gableX(0.25f, 0.25f, 1.5f, 1.3f, 30f, 20f, shade(accent, 0.95f))
                s.roundTree(1.85f, 0.25f, 0f, 0.8f)
            }
            "bungalow" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF9CCC65.toInt())
                val thatch = 0xFFC8A165.toInt()
                for (k in 0 until 3) {
                    val x = 0.15f + k * 0.95f
                    s.box(x, 0.55f, 0.7f, 0.7f, 0f, 16f, 0xFFD7CCC8.toInt())
                    s.windowsR(x + 0.7f, 0.55f, 1.25f, 0f, 1, 14f, win, 1)
                    s.pyramid(x - 0.08f, 0.47f, 0.86f, 0.86f, 16f, 20f, thatch)
                }
                s.palm(0.3f, 1.65f); s.palm(2.6f, 0.3f, 0f, 0.9f)
                s.flat(0.6f, 1.55f, 1.6f, 0.3f, 0.4f, accent)
            }
            "hotel3" -> {
                s.flat(0f, 0f, W, D, 0.2f, paving)
                s.box(0.3f, 0.4f, 2.4f, 1.6f, 0f, 66f, 0xFFFFE0B2.toInt())
                s.windowsR(2.7f, 0.4f, 2f, 0f, 4, 16f, win, 3)
                s.windowsL(2f, 0.3f, 2.7f, 0f, 4, 16f, win, 3)
                s.box(0.3f, 0.4f, 2.4f, 1.6f, 66f, 4f, accent)
                s.box(1.0f, 0.9f, 0.8f, 0.4f, 70f, 12f, white)
                s.pool(0.4f, 2.2f, 1.3f, 0.6f)
                s.palm(2.5f, 2.5f, 0f, 0.8f)
            }
            "hotel4" -> {
                s.flat(0f, 0f, W, D, 0.2f, paving)
                s.box(0.4f, 0.3f, 1.8f, 1.5f, 0f, 124f, 0xFF90CAF9.toInt(), shade(accent, 1.0f))
                for (f in 0 until 7) {
                    s.windowsR(2.2f, 0.3f, 1.8f, f * 17f, 1, 17f, if (f % 2 == 0) glass else shade(glass, 0.85f), 4)
                    s.windowsL(1.8f, 0.4f, 2.2f, f * 17f, 1, 17f, if (f % 2 == 0) glass else shade(glass, 0.85f), 4)
                }
                s.box(0.9f, 0.7f, 0.6f, 0.5f, 124f, 12f, white)
                s.pool(0.3f, 2.0f, 1.6f, 0.75f)
                s.umbrella(2.2f, 2.3f, accent); s.umbrella(2.6f, 2.0f, accent)
                s.palm(2.6f, 2.7f, 0f, 0.8f)
            }
            "resort" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFFDCE775.toInt())
                s.box(0.3f, 0.3f, 1.4f, 1.4f, 0f, 180f, 0xFFE3F2FD.toInt(), shade(accent, 1.05f))
                for (f in 0 until 10) {
                    s.windowsR(1.7f, 0.3f, 1.7f, f * 17.5f, 1, 17.5f, darkGlass, 3)
                    s.windowsL(1.7f, 0.3f, 1.7f, f * 17.5f, 1, 17.5f, darkGlass, 3)
                }
                s.box(0.75f, 0.75f, 0.5f, 0.5f, 180f, 22f, accent)
                s.box(2.0f, 0.3f, 1.7f, 1.0f, 0f, 36f, white)
                s.windowsR(3.7f, 0.3f, 1.3f, 0f, 2, 17f, darkGlass, 3)
                s.windowsL(1.3f, 2.0f, 3.7f, 0f, 2, 17f, darkGlass, 3)
                s.pool(0.4f, 2.1f, 1.8f, 1.4f)
                s.pool(2.4f, 2.0f, 1.2f, 0.8f)
                for (k in 0 until 4) s.umbrella(2.3f + k * 0.4f, 3.2f, if (k % 2 == 0) accent else 0xFFFFFFFF.toInt())
                s.palm(0.2f, 3.7f); s.palm(3.7f, 3.6f); s.palm(3.7f, 1.6f, 0f, 0.8f)
            }
            "villa" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF8BC34A.toInt())
                s.box(0.25f, 0.25f, 1.3f, 0.9f, 0f, 22f, white)
                s.box(0.25f, 0.25f, 0.8f, 0.9f, 22f, 18f, white)
                s.windowsR(1.55f, 0.25f, 1.15f, 0f, 1, 20f, glass, 2)
                s.windowsL(1.15f, 0.25f, 1.55f, 0f, 1, 20f, glass, 2)
                s.windowsL(1.15f, 0.25f, 1.05f, 22f, 1, 18f, glass, 2)
                s.box(0.2f, 0.2f, 0.9f, 1.0f, 40f, 3f, accent)
                s.pool(0.3f, 1.3f, 1.4f, 0.55f)
                s.palm(1.8f, 0.3f, 0f, 0.85f)
            }
            "kiosk" -> {
                s.box(0.2f, 0.2f, 0.6f, 0.6f, 0f, 16f, 0xFFFFF8E1.toInt())
                s.quad(0.15f, 0.8f, 18f, 0.85f, 0.8f, 18f, 0.85f, 0.95f, 12f, 0.15f, 0.95f, 12f, accent)
                s.quad(0.8f, 0.15f, 18f, 0.8f, 0.85f, 18f, 0.95f, 0.85f, 12f, 0.95f, 0.15f, 12f, shade(accent, 0.85f))
                s.box(0.2f, 0.2f, 0.6f, 0.6f, 16f, 4f, accent)
                s.umbrella(0.15f, 0.9f, 0xFFFFEB3B.toInt())
            }
            "fastfood" -> {
                s.flat(0f, 0f, W, D, 0.2f, paving)
                s.box(0.15f, 0.15f, 1.6f, 0.7f, 0f, 28f, 0xFFE53935.toInt())
                s.windowsR(1.75f, 0.15f, 0.85f, 0f, 1, 24f, winLit, 2)
                s.windowsL(0.85f, 0.15f, 1.75f, 0f, 1, 24f, winLit, 3)
                s.box(0.15f, 0.15f, 1.6f, 0.7f, 28f, 5f, 0xFFFFC107.toInt())
                s.pole(1.6f, 0.2f, 33f, 10f, 0xFF616161.toInt())
                s.box(1.35f, 0.15f, 0.5f, 0.12f, 43f, 12f, accent)
            }
            "restaurant" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFFD7CCC8.toInt())
                s.box(0.2f, 0.2f, 1.6f, 0.9f, 0f, 30f, 0xFFFFF8E1.toInt())
                s.windowsR(1.8f, 0.2f, 1.1f, 0f, 1, 28f, winLit, 2)
                s.windowsL(1.1f, 0.2f, 1.8f, 0f, 1, 28f, winLit, 3)
                s.gableX(0.15f, 0.15f, 1.7f, 1.0f, 30f, 16f, 0xFFBF5B3A.toInt())
                for (k in 0 until 3) s.umbrella(0.4f + k * 0.55f, 1.55f, if (k % 2 == 0) accent else 0xFFFFFFFF.toInt())
            }
            "finedining" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF8BC34A.toInt())
                s.box(0.25f, 0.25f, 1.5f, 1.2f, 0f, 38f, 0xFF37474F.toInt())
                s.windowsR(1.75f, 0.25f, 1.45f, 0f, 1, 34f, winLit, 3)
                s.windowsL(1.45f, 0.25f, 1.75f, 0f, 1, 34f, winLit, 3)
                s.box(0.2f, 0.2f, 1.6f, 1.3f, 38f, 5f, 0xFFFFD54F.toInt())
                s.pyramid(0.6f, 0.5f, 0.8f, 0.7f, 43f, 14f, accent)
                s.roundTree(1.85f, 1.85f, 0f, 0.7f); s.roundTree(0.2f, 1.75f, 0f, 0.7f)
            }
            "pub" -> {
                s.flat(0f, 0f, W, D, 0.2f, paving)
                s.box(0.15f, 0.15f, 1.7f, 0.7f, 0f, 26f, 0xFF8D6E63.toInt())
                s.windowsR(1.85f, 0.15f, 0.85f, 0f, 1, 24f, winLit, 2)
                s.windowsL(0.85f, 0.15f, 1.85f, 0f, 1, 24f, winLit, 3)
                s.gableX(0.1f, 0.1f, 1.8f, 0.8f, 26f, 14f, 0xFF4E342E.toInt())
                s.pole(1.9f, 0.5f, 10f, 14f, 0xFF3E2723.toInt())
                s.box(1.88f, 0.35f, 0.04f, 0.3f, 18f, 8f, accent)
            }
            "disco" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF424242.toInt())
                s.box(0.2f, 0.2f, 1.6f, 1.4f, 0f, 44f, 0xFF311B92.toInt())
                val neon = intArrayOf(0xFFFF4081.toInt(), 0xFF00E5FF.toInt(), 0xFFFFEA00.toInt())
                for (k in 0 until 3) {
                    s.quad(1.8f, 0.2f, 8f + k * 12f, 1.8f, 1.6f, 8f + k * 12f, 1.8f, 1.6f, 10f + k * 12f, 1.8f, 0.2f, 10f + k * 12f, neon[k])
                    s.quad(0.2f, 1.6f, 8f + k * 12f, 1.8f, 1.6f, 8f + k * 12f, 1.8f, 1.6f, 10f + k * 12f, 0.2f, 1.6f, 10f + k * 12f, neon[(k + 1) % 3])
                }
                s.box(0.6f, 0.6f, 0.8f, 0.6f, 44f, 6f, accent)
                val bx = s.sx(1f, 0.9f); val by = s.sy(1f, 0.9f, 58f)
                p.oval(bx - 6 * s.zs, by - 6 * s.zs, bx + 6 * s.zs, by + 6 * s.zs, 0xFFE0E0E0.toInt())
                p.oval(bx - 3 * s.zs, by - 4 * s.zs, bx + 1 * s.zs, by - 1 * s.zs, 0xFFFFFFFF.toInt())
            }
            "arcade" -> {
                s.flat(0f, 0f, W, D, 0.2f, paving)
                s.box(0.15f, 0.15f, 1.7f, 0.7f, 0f, 30f, 0xFF7B1FA2.toInt())
                val cols = intArrayOf(0xFFFF5252.toInt(), 0xFFFFEB3B.toInt(), 0xFF69F0AE.toInt(), 0xFF40C4FF.toInt())
                for (k in 0 until 4) s.quad(0.3f + k * 0.38f, 0.85f, 6f, 0.6f + k * 0.38f, 0.85f, 6f, 0.6f + k * 0.38f, 0.85f, 20f, 0.3f + k * 0.38f, 0.85f, 20f, cols[k])
                s.box(0.15f, 0.15f, 1.7f, 0.7f, 30f, 8f, accent)
                s.box(0.5f, 0.3f, 1.0f, 0.1f, 38f, 12f, 0xFFFFEB3B.toInt())
            }
            "funfair" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFFA5D6A7.toInt())
                // Hiz treni rayi
                val track = 0xFFD32F2F.toInt()
                var px = 0.3f; var py = 2.6f; var pz = 4f
                for (k in 1..16) {
                    val a = k / 16f * 6.283f
                    val nx = 2.4f + cos(a) * 1.4f; val ny = 1.9f + sin(a) * 0.8f; val nz = 18f + sin(a * 2) * 14f
                    p.line(s.sx(px, py), s.sy(px, py, pz), s.sx(nx, ny), s.sy(nx, ny, nz), 2.5f * s.zs, track)
                    s.pole(nx, ny, 0f, nz, 0xFF9E9E9E.toInt(), 1f)
                    px = nx; py = ny; pz = nz
                }
                // Donme dolap
                val wx = s.sx(0.9f, 0.9f); val wy = s.sy(0.9f, 0.9f, 70f)
                val r = 52f * s.zs
                p.line(wx, wy, wx - 18 * s.zs, s.sy(0.9f, 0.9f, 0f), 3 * s.zs, 0xFF757575.toInt())
                p.line(wx, wy, wx + 18 * s.zs, s.sy(0.9f, 0.9f, 0f), 3 * s.zs, 0xFF757575.toInt())
                p.ring(wx, wy, r, r, 3 * s.zs, accent)
                for (k in 0 until 8) {
                    val a = k * 0.785f
                    p.line(wx, wy, wx + cos(a) * r, wy + sin(a) * r, 1.2f * s.zs, 0xFFBDBDBD.toInt())
                    val cx = wx + cos(a) * r; val cy = wy + sin(a) * r
                    p.oval(cx - 5 * s.zs, cy - 2 * s.zs, cx + 5 * s.zs, cy + 6 * s.zs, if (k % 2 == 0) 0xFFFFEB3B.toInt() else 0xFF29B6F6.toInt())
                }
                // Cadir
                s.box(2.6f, 0.2f, 1.0f, 0.8f, 0f, 14f, 0xFFFFFFFF.toInt())
                s.pyramid(2.55f, 0.15f, 1.1f, 0.9f, 14f, 22f, 0xFFE53935.toInt())
            }
            "waterpark" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFFE0E0E0.toInt())
                s.pool(0.3f, 1.8f, 2.2f, 1.9f)
                s.pool(2.7f, 2.4f, 1.0f, 1.3f)
                // Kaydiraklar
                s.box(0.3f, 0.3f, 0.6f, 0.6f, 0f, 90f, 0xFF9E9E9E.toInt())
                val sl = intArrayOf(accent, 0xFFFFEB3B.toInt(), 0xFFFF7043.toInt())
                for (k in 0 until 3) {
                    var x0 = 0.9f; var y0 = 0.6f; var z0 = 88f - k * 22f
                    for (j in 1..10) {
                        val tt = j / 10f
                        val x1 = 0.9f + tt * (1.2f + k * 0.5f) + sin(tt * 6f + k) * 0.25f
                        val y1 = 0.6f + tt * (1.4f + k * 0.3f)
                        val z1 = z0 - (88f - k * 22f) / 10f
                        p.line(s.sx(x0, y0), s.sy(x0, y0, z0), s.sx(x1, y1), s.sy(x1, y1, z1), 4 * s.zs, sl[k])
                        x0 = x1; y0 = y1; z0 = z1
                    }
                }
                s.palm(3.6f, 0.4f); s.umbrella(3.0f, 1.2f, accent); s.umbrella(3.4f, 1.6f, 0xFFFFFFFF.toInt())
            }
            "casino" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF424242.toInt())
                s.box(0.3f, 0.3f, 2.4f, 2.1f, 0f, 60f, 0xFFB71C1C.toInt())
                s.windowsR(2.7f, 0.3f, 2.4f, 0f, 3, 20f, 0xFFFFD54F.toInt(), 3)
                s.windowsL(2.4f, 0.3f, 2.7f, 0f, 3, 20f, 0xFFFFD54F.toInt(), 3)
                s.box(0.25f, 0.25f, 2.5f, 2.2f, 60f, 6f, 0xFFFFC107.toInt())
                val dx = s.sx(1.5f, 1.35f); val dy = s.sy(1.5f, 1.35f, 66f)
                p.oval(dx - 30 * s.zs, dy - 34 * s.zs, dx + 30 * s.zs, dy + 8 * s.zs, 0xFFFFD54F.toInt())
                p.oval(dx - 22 * s.zs, dy - 30 * s.zs, dx + 6 * s.zs, dy - 10 * s.zs, 0xFFFFECB3.toInt())
                s.pole(1.5f, 1.35f, 100f, 18f, accent, 2f)
            }
            "souvenir" -> {
                s.box(0.15f, 0.15f, 0.7f, 0.7f, 0f, 20f, 0xFFFFF3E0.toInt())
                s.windowsR(0.85f, 0.15f, 0.85f, 0f, 1, 18f, winLit, 1)
                s.quad(0.1f, 0.85f, 20f, 0.9f, 0.85f, 20f, 0.9f, 1.0f, 13f, 0.1f, 1.0f, 13f, accent)
                s.gableY(0.1f, 0.1f, 0.8f, 0.8f, 20f, 12f, 0xFF26A69A.toInt())
            }
            "market" -> {
                s.flat(0f, 0f, W, D, 0.2f, paving)
                s.box(0.15f, 0.15f, 1.7f, 0.7f, 0f, 28f, white)
                s.windowsL(0.85f, 0.15f, 1.85f, 0f, 1, 26f, glass, 3)
                s.windowsR(1.85f, 0.15f, 0.85f, 0f, 1, 26f, glass, 2)
                s.box(0.15f, 0.15f, 1.7f, 0.7f, 28f, 6f, 0xFF43A047.toInt())
                s.box(0.6f, 0.75f, 0.8f, 0.05f, 22f, 8f, accent)
            }
            "mall" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF9E9E9E.toInt())
                for (k in 0 until 6) s.flat(0.3f + k * 0.55f, 2.3f, 0.06f, 0.5f, 0.4f, 0xFFFFFFFF.toInt())
                s.box(0.25f, 0.25f, 3.5f, 1.9f, 0f, 46f, 0xFFECEFF1.toInt())
                s.windowsR(3.75f, 0.25f, 2.15f, 0f, 2, 22f, glass, 3)
                s.windowsL(2.15f, 0.25f, 3.75f, 0f, 2, 22f, glass, 3)
                s.box(0.2f, 0.2f, 3.6f, 2.0f, 46f, 6f, accent)
                s.box(1.5f, 0.7f, 1.0f, 0.9f, 52f, 10f, glass)
            }
            "beachclub" -> {
                s.flat(0.05f, 0.05f, W - 0.1f, D - 0.1f, 0.3f, 0xFFF8E7C0.toInt())
                for (k in 0 until 3) {
                    s.sunbed(0.2f + k * 0.6f, 0.65f)
                    s.umbrella(0.35f + k * 0.6f, 0.45f, if (k % 2 == 0) accent else 0xFFFFFFFF.toInt())
                }
                s.box(0.1f, 0.1f, 0.4f, 0.25f, 0f, 10f, 0xFF8D6E63.toInt())
            }
            "park" -> {
                s.flat(0f, 0f, W, D, 0.3f, 0xFF7CB342.toInt())
                s.flat(0.9f, 0f, 0.2f, D, 0.5f, 0xFFD7CCC8.toInt())
                s.flat(0f, 0.9f, W, 0.2f, 0.5f, 0xFFD7CCC8.toInt())
                val fx = s.sx(1f, 1f); val fy = s.sy(1f, 1f, 1f)
                p.oval(fx - 12 * s.zs, fy - 6 * s.zs, fx + 12 * s.zs, fy + 6 * s.zs, 0xFFB0BEC5.toInt())
                p.oval(fx - 9 * s.zs, fy - 4.5f * s.zs, fx + 9 * s.zs, fy + 4.5f * s.zs, 0xFF4FC3F7.toInt())
                p.line(fx, fy, fx, fy - 10 * s.zs, 2 * s.zs, 0xFFB3E5FC.toInt())
                s.roundTree(0.4f, 0.4f, 0f, 0.9f); s.roundTree(1.6f, 0.4f, 0f, 0.8f)
                s.roundTree(0.4f, 1.6f, 0f, 0.8f); s.roundTree(1.6f, 1.6f, 0f, 0.9f, 0xFF66BB6A.toInt())
            }
            "palm" -> s.palm(0.5f, 0.5f)
            "garden" -> {
                s.flat(0.1f, 0.1f, 0.8f, 0.8f, 0.4f, 0xFF689F38.toInt())
                val fl = intArrayOf(0xFFE91E63.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF7043.toInt(), 0xFFAB47BC.toInt())
                for (k in 0 until 9) {
                    val x = 0.2f + (k % 3) * 0.3f; val y = 0.2f + (k / 3) * 0.3f
                    val cx = s.sx(x, y); val cy = s.sy(x, y, 2f)
                    p.oval(cx - 3 * s.zs, cy - 2 * s.zs, cx + 3 * s.zs, cy + 2 * s.zs, fl[k % fl.size])
                }
            }
            "road" -> {
                s.flat(0f, 0f, 1f, 1f, 0.3f, 0xFF616161.toInt())
                s.flat(0.47f, 0.15f, 0.06f, 0.25f, 0.5f, 0xFFEEEEEE.toInt())
                s.flat(0.47f, 0.6f, 0.06f, 0.25f, 0.5f, 0xFFEEEEEE.toInt())
            }
            "airport" -> {
                s.flat(0f, 0f, W, D, 0.2f, 0xFF9CCC65.toInt())
                s.flat(0.1f, 1.9f, 3.8f, 0.8f, 0.4f, 0xFF424242.toInt())
                for (k in 0 until 7) s.flat(0.3f + k * 0.52f, 2.27f, 0.28f, 0.06f, 0.6f, 0xFFFFFFFF.toInt())
                s.box(0.3f, 0.2f, 2.2f, 1.1f, 0f, 22f, 0xFFECEFF1.toInt())
                s.windowsL(1.3f, 0.3f, 2.5f, 0f, 1, 20f, glass, 4)
                s.windowsR(2.5f, 0.2f, 1.3f, 0f, 1, 20f, glass, 2)
                s.box(2.9f, 0.4f, 0.45f, 0.45f, 0f, 60f, 0xFFCFD8DC.toInt())
                s.box(2.82f, 0.32f, 0.6f, 0.6f, 60f, 12f, glass)
                // Ucak
                val ax = s.sx(2.2f, 2.3f); val ay = s.sy(2.2f, 2.3f, 3f)
                p.line(ax - 26 * s.zs, ay - 4 * s.zs, ax + 26 * s.zs, ay + 4 * s.zs, 6 * s.zs, 0xFFFFFFFF.toInt())
                p.line(ax - 4 * s.zs, ay - 14 * s.zs, ax + 4 * s.zs, ay + 14 * s.zs, 4 * s.zs, 0xFFE0E0E0.toInt())
                p.line(ax - 24 * s.zs, ay - 8 * s.zs, ax - 20 * s.zs, ay + 2 * s.zs, 3 * s.zs, 0xFF1976D2.toInt())
            }
            "harbor" -> {
                s.flat(0f, 0f, W, D, 0.3f, 0xFFA1887F.toInt())
                for (k in 0 until 6) s.flat(0.1f + k * 0.48f, 0f, 0.04f, D, 0.5f, 0xFF6D4C41.toInt())
                s.box(0.2f, 0.2f, 0.9f, 0.7f, 0f, 20f, 0xFFFFF3E0.toInt())
                s.gableX(0.15f, 0.15f, 1.0f, 0.8f, 20f, 12f, 0xFF1E88E5.toInt())
                // Gemi
                val bx = s.sx(2.3f, 1.4f); val by = s.sy(2.3f, 1.4f, 2f)
                p.poly(floatArrayOf(bx - 30 * s.zs, bx + 30 * s.zs, bx + 22 * s.zs, bx - 24 * s.zs), floatArrayOf(by - 10 * s.zs, by - 10 * s.zs, by + 2 * s.zs, by + 2 * s.zs), 0xFFFFFFFF.toInt())
                p.poly(floatArrayOf(bx - 16 * s.zs, bx + 14 * s.zs, bx + 14 * s.zs, bx - 16 * s.zs), floatArrayOf(by - 22 * s.zs, by - 22 * s.zs, by - 10 * s.zs, by - 10 * s.zs), 0xFFE3F2FD.toInt())
                p.line(bx - 26 * s.zs, by - 4 * s.zs, bx + 26 * s.zs, by - 4 * s.zs, 2 * s.zs, 0xFF1565C0.toInt())
                s.pole(1.9f, 0.2f, 20f, 18f, 0xFF5D4037.toInt())
            }
            "motel" -> {
                // Otopark
                s.flat(0f, 0f, W, D, 0.2f, 0xFF757575.toInt())
                for (k in 0 until 3) s.flat(0.35f + k * 0.5f, 1.45f, 0.05f, 0.45f, 0.35f, 0xFFFFFFFF.toInt())
                // Iki katli uzun govde (arka sirada), on yuzde dis koridor
                val body = 0xFFFFE0B2.toInt()
                s.box(0.15f, 0.15f, 1.7f, 0.75f, 0f, 30f, body)
                // Kapilar: alt ve ust kat, sirket renginde
                for (f in 0 until 2) for (k in 0 until 4) {
                    val x0 = 0.25f + k * 0.42f; val z0 = f * 15f
                    s.quad(x0, 0.9f, z0, x0 + 0.16f, 0.9f, z0, x0 + 0.16f, 0.9f, z0 + 11f, x0, 0.9f, z0 + 11f, if (k % 2 == 0) accent else shade(accent, 0.8f))
                    s.quad(x0 + 0.22f, 0.9f, z0 + 4f, x0 + 0.34f, 0.9f, z0 + 4f, x0 + 0.34f, 0.9f, z0 + 9f, x0 + 0.22f, 0.9f, z0 + 9f, winLit)
                }
                // Ust kat koridoru ve korkuluk
                s.flat(0.15f, 0.9f, 1.7f, 0.18f, 15f, 0xFFBCAAA4.toInt())
                s.quad(0.15f, 1.08f, 15f, 1.85f, 1.08f, 15f, 1.85f, 1.08f, 19f, 0.15f, 1.08f, 19f, 0xFF8D6E63.toInt())
                for (k in 0..4) s.pole(0.15f + k * 0.425f, 1.08f, 0f, 15f, 0xFF8D6E63.toInt(), 1.2f)
                s.windowsR(1.85f, 0.15f, 0.9f, 0f, 2, 15f, win, 2)
                // Duz cati + sirket renginde saçak
                s.box(0.1f, 0.1f, 1.8f, 0.85f, 30f, 3f, accent)
                // Neon tabela
                s.pole(1.85f, 1.8f, 0f, 44f, 0xFF616161.toInt(), 2f)
                val tx = s.sx(1.85f, 1.8f); val ty = s.sy(1.85f, 1.8f, 44f)
                p.poly(floatArrayOf(tx - 14 * s.zs, tx + 14 * s.zs, tx + 14 * s.zs, tx - 14 * s.zs), floatArrayOf(ty - 16 * s.zs, ty - 16 * s.zs, ty, ty), 0xFF1A237E.toInt())
                p.line(tx - 11 * s.zs, ty - 8 * s.zs, tx + 11 * s.zs, ty - 8 * s.zs, 4 * s.zs, 0xFFFF4081.toInt())
                p.line(tx - 11 * s.zs, ty - 3 * s.zs, tx + 11 * s.zs, ty - 3 * s.zs, 1.5f * s.zs, 0xFF00E5FF.toInt())
                p.oval(tx + 9 * s.zs, ty - 22 * s.zs, tx + 15 * s.zs, ty - 16 * s.zs, 0xFFFFEA00.toInt())
                // Araba
                val cx = s.sx(0.85f, 1.65f); val cy = s.sy(0.85f, 1.65f, 0f)
                p.poly(floatArrayOf(cx - 11 * s.zs, cx + 11 * s.zs, cx + 11 * s.zs, cx - 11 * s.zs), floatArrayOf(cy - 8 * s.zs, cy - 3 * s.zs, cy + 2 * s.zs, cy - 3 * s.zs), 0xFF26A69A.toInt())
                p.poly(floatArrayOf(cx - 5 * s.zs, cx + 6 * s.zs, cx + 6 * s.zs, cx - 5 * s.zs), floatArrayOf(cy - 12 * s.zs, cy - 9 * s.zs, cy - 5 * s.zs, cy - 8 * s.zs), 0xFFB2EBF2.toInt())
                s.palm(0.2f, 1.75f, 0f, 0.8f)
            }
            else -> s.box(0.1f, 0.1f, W - 0.2f, D - 0.2f, 0f, 30f, white)
        }
    }

    /** Yumusak gölge: ayak izi, sag-one dogru (isik sol-arkadan) */
    private fun shadow(s: Iso, t: BType, W: Float, D: Float) {
        val hgt = height(t.key)
        if (hgt < 12f || t.key == "road" || t.key == "garden") return
        val vx = (hgt / 110f).coerceIn(0.25f, 1.0f); val vy = vx * 0.35f
        val xs = floatArrayOf(0f, W, W + vx, W + vx, vx, 0f)
        val ys = floatArrayOf(0f, 0f, vy, D + vy, D + vy, D)
        s.p.poly(FloatArray(6) { s.sx(xs[it], ys[it]) }, FloatArray(6) { s.sy(xs[it], ys[it], 0f) }, alpha(0x000000, 46))
    }

    /** Insaat alani: toprak, iskele, vinc (progress 0..1) */
    fun construction(p: IsoPainter, w: Int, h: Int, ox: Float, oy: Float, tw: Float, th: Float, progress: Float, accent: Int) {
        val s = Iso(p, ox, oy, tw, th)
        val W = w.toFloat(); val D = h.toFloat()
        s.flat(0.05f, 0.05f, W - 0.1f, D - 0.1f, 0.3f, 0xFFA1887F.toInt())
        val hgt = 8f + 30f * progress * (W + D) / 4f
        s.box(0.3f, 0.3f, W - 0.6f, D - 0.6f, 0f, hgt * 0.7f, 0xFFBCAAA4.toInt())
        val sc = 0xFFFFA000.toInt()
        for (k in 0..3) {
            val z = hgt * k / 3f
            s.quad(0.25f, D - 0.25f, z, W - 0.25f, D - 0.25f, z, W - 0.25f, D - 0.25f, z + 1.5f, 0.25f, D - 0.25f, z + 1.5f, sc)
        }
        s.pole(0.25f, D - 0.25f, 0f, hgt, sc, 1.2f); s.pole(W - 0.25f, D - 0.25f, 0f, hgt, sc, 1.2f)
        // Vinc
        val cx = W - 0.4f; val cy = 0.4f
        s.pole(cx, cy, 0f, hgt + 40f, 0xFFFBC02D.toInt(), 2.5f)
        val tx = s.sx(cx, cy); val ty = s.sy(cx, cy, hgt + 40f)
        p.line(tx + 10 * s.zs, ty, tx - 40 * s.zs, ty + 6 * s.zs, 2.5f * s.zs, 0xFFFBC02D.toInt())
        p.line(tx - 34 * s.zs, ty + 5 * s.zs, tx - 34 * s.zs, ty + 24 * s.zs, 1 * s.zs, 0xFF424242.toInt())
        s.flag(0.15f, 0.15f, 0f, accent)
    }
}
