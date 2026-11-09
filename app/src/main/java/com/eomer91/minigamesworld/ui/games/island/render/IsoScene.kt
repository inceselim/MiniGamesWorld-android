package com.eomer91.minigamesworld.ui.games.island.render

import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import com.eomer91.minigamesworld.ui.games.island.engine.Terrain
import kotlin.math.floor

/**
 * Dunyanin izometrik yerlesimi. Dunya koordinati: karo (0,0)'in ust kosesi = (originX, 0).
 * Tum harita genisligi n*tw, yuksekligi n*th (+ ust bosluk).
 */
class IsoLayout(val n: Int, val tw: Float, val th: Float, val top: Float) {
    val originX = n * tw / 2f
    val width = n * tw
    val height = n * th + top

    fun tileX(x: Float, y: Float) = originX + (x - y) * tw / 2f
    fun tileY(x: Float, y: Float) = top + (x + y) * th / 2f

    /** Dunya pikselinden karo koordinatina (ondalikli) */
    fun toTileF(px: Float, py: Float): Pair<Float, Float> {
        val a = (px - originX) / (tw / 2f)
        val b = (py - top) / (th / 2f)
        return Pair((a + b) / 2f, (b - a) / 2f)
    }

    fun toTile(px: Float, py: Float): Pair<Int, Int> {
        val (fx, fy) = toTileF(px, py)
        return Pair(floor(fx).toInt(), floor(fy).toInt())
    }
}

object IsoScene {

    fun terrainAt(w: IsWorld, x: Int, y: Int): Terrain =
        if (x < 0 || y < 0 || x >= w.n || y >= w.n) Terrain.DEEP else Terrain.entries[w.terrain[y * w.n + x].toInt()]

    /** Zemin karolarinin tamami (bitmap'e pisirmek icin) */
    fun drawTerrain(p: IsoPainter, w: IsWorld, L: IsoLayout, grow: Float = 0.6f) {
        val n = w.n
        // Ust kosesi tw/2 boyunca dikey sirayla cizildiginde ortusme olmaz; yine de x+y sirasiyla ciz.
        for (s in 0 until 2 * n - 1) {
            for (x in 0..s) {
                val y = s - x
                if (x >= n || y >= n) continue
                val t = terrainAt(w, x, y)
                var land = 0; var water = 0
                for ((dx, dy) in NB) {
                    val o = terrainAt(w, x + dx, y + dy)
                    if (o.isLand) land++ else water++
                }
                IsoArt.tile(p, L.tileX(x.toFloat(), y.toFloat()), L.tileY(x.toFloat(), y.toFloat()), L.tw, L.th, t, x, y,
                    if (t.isLand) 0 else land, if (t.isLand) water else 0, grow)
            }
        }
    }

    private val NB = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    /** Cizim icin binanin anlik kopyasi (motor baska is parcaciginda degistirirken guvenli) */
    class BSnap(val id: Int, val type: BType, val x: Int, val y: Int, val owner: Int, val color: Int, val progress: Float, val building: Boolean, val closed: Boolean)

    /**
     * Binalari cizim sirasina dizer. Izometrikte cok karolu dikdortgenler icin: on kosesi (x+w, y+h)
     * ekrana daha yakin olan sonra cizilir; esitlikte ust kose toplami.
     */
    fun snapshot(w: IsWorld): List<BSnap> {
        val out = ArrayList<BSnap>(w.buildings.size)
        for (i in 0 until w.buildings.size) {
            val b = w.buildings.getOrNull(i) ?: continue
            out.add(BSnap(b.id, b.type, b.x, b.y, b.owner, ownerColor(w, b.owner), 1f - b.buildLeft / b.type.days.coerceAtLeast(1).toFloat(), b.buildLeft > 0, b.closedDays > 0))
        }
        out.sortWith(compareBy({ it.x + it.type.w + it.y + it.type.h }, { it.x + it.y }))
        return out
    }

    fun ownerColor(w: IsWorld, owner: Int): Int =
        if (owner >= 0 && owner < w.companies.size) w.companies[owner].color else 0xFF90A4AE.toInt()

    fun drawBuilding(p: IsoPainter, L: IsoLayout, b: BSnap) {
        val ox = L.tileX(b.x.toFloat(), b.y.toFloat()); val oy = L.tileY(b.x.toFloat(), b.y.toFloat())
        if (b.building) {
            IsoArt.construction(p, b.type.w, b.type.h, ox, oy, L.tw, L.th, b.progress, b.color)
        } else {
            IsoArt.building(p, b.type, ox, oy, L.tw, L.th, b.color)
            if (b.closed) closedMark(p, L, b)
        }
    }

    /** Sabotaj/grev: kirmizi uyari isareti */
    fun closedMark(p: IsoPainter, L: IsoLayout, b: BSnap) {
        val fx = b.x + b.type.w / 2f; val fy = b.y + b.type.h / 2f
        val cx = L.tileX(fx, fy); val cy = L.tileY(fx, fy) - IsoArt.height(b.type.key) * L.tw / 64f - L.tw * 0.25f
        val r = L.tw * 0.16f
        p.oval(cx - r, cy - r, cx + r, cy + r, 0xFFD32F2F.toInt())
        p.line(cx - r * 0.5f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.5f, r * 0.3f, 0xFFFFFFFF.toInt())
        p.line(cx + r * 0.5f, cy - r * 0.5f, cx - r * 0.5f, cy + r * 0.5f, r * 0.3f, 0xFFFFFFFF.toInt())
    }

    /** Ekran noktasinin (dunya pikseli) ustunde gorunen bina: one yakindan arkaya, govde yuksekligi dahil */
    fun pick(list: List<BSnap>, L: IsoLayout, px: Float, py: Float): BSnap? {
        for (i in list.indices.reversed()) {
            val b = list[i]
            val hgt = (IsoArt.height(b.type.key) * 0.85f) * L.tw / 64f
            var dz = 0f
            while (dz <= hgt) {
                val (fx, fy) = L.toTileF(px, py + dz)
                if (fx >= b.x && fx < b.x + b.type.w && fy >= b.y && fy < b.y + b.type.h) return b
                dz += L.th / 3f
            }
        }
        return null
    }

    /** Sirket arsalarinin kenarlari (ince renkli cerceve) */
    fun drawPlots(p: IsoPainter, w: IsWorld, L: IsoLayout, x0: Int = 0, y0: Int = 0, x1: Int = w.n - 1, y1: Int = w.n - 1) {
        val n = w.n
        val lw = L.tw / 28f
        for (y in y0.coerceAtLeast(0)..y1.coerceAtMost(n - 1)) for (x in x0.coerceAtLeast(0)..x1.coerceAtMost(n - 1)) {
            val o = w.owner[y * n + x]
            if (o < 0 || o >= w.companies.size) continue
            val c = IsoColor.alpha(w.companies[o].color, 200)
            val fx = x.toFloat(); val fy = y.toFloat()
            fun own(ax: Int, ay: Int) = ax in 0 until n && ay in 0 until n && w.owner[ay * n + ax] == o
            if (!own(x, y - 1)) p.line(L.tileX(fx, fy), L.tileY(fx, fy), L.tileX(fx + 1, fy), L.tileY(fx + 1, fy), lw, c)
            if (!own(x + 1, y)) p.line(L.tileX(fx + 1, fy), L.tileY(fx + 1, fy), L.tileX(fx + 1, fy + 1), L.tileY(fx + 1, fy + 1), lw, c)
            if (!own(x, y + 1)) p.line(L.tileX(fx, fy + 1), L.tileY(fx, fy + 1), L.tileX(fx + 1, fy + 1), L.tileY(fx + 1, fy + 1), lw, c)
            if (!own(x - 1, y)) p.line(L.tileX(fx, fy), L.tileY(fx, fy), L.tileX(fx, fy + 1), L.tileY(fx, fy + 1), lw, c)
        }
    }

    /** Secim / yerlestirme cercevesi */
    fun drawFootprint(p: IsoPainter, L: IsoLayout, x: Int, y: Int, w: Int, h: Int, fill: Int, stroke: Int) {
        val fx = x.toFloat(); val fy = y.toFloat()
        val xs = floatArrayOf(L.tileX(fx, fy), L.tileX(fx + w, fy), L.tileX(fx + w, fy + h), L.tileX(fx, fy + h))
        val ys = floatArrayOf(L.tileY(fx, fy), L.tileY(fx + w, fy), L.tileY(fx + w, fy + h), L.tileY(fx, fy + h))
        p.poly(xs, ys, fill)
        val lw = L.tw / 20f
        for (i in 0 until 4) { val j = (i + 1) % 4; p.line(xs[i], ys[i], xs[j], ys[j], lw, stroke) }
    }

    /** Onizleme / test: tum sahne */
    fun drawAll(p: IsoPainter, w: IsWorld, L: IsoLayout) {
        drawTerrain(p, w, L)
        drawPlots(p, w, L)
        for (b in snapshot(w)) drawBuilding(p, L, b)
    }
}
