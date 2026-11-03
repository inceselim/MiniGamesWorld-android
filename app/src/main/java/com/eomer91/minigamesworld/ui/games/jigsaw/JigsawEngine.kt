package com.eomer91.minigamesworld.ui.games.jigsaw

import kotlin.math.abs
import kotlin.random.Random

/*
 * Yapboz kurallari (Android'den bagimsiz). Koordinatlar "hucre" birimindedir: tahta cols x rows,
 * (0,0) tahtanin sol ustu. Tahtanin altindaki tepsi bolgesi y > rows.
 */

enum class JigsawLevel(val cols: Int, val rows: Int, val score: Int) {
    EASY(4, 3, 200),
    NORMAL(6, 4, 450),
    HARD(8, 5, 900),
}

class JigsawPiece(val r: Int, val c: Int, var x: Float, var y: Float) {
    var placed = false
}

class JigsawEngine(val cols: Int, val rows: Int, seed: Int = Random.nextInt()) {
    private val random = Random(seed)

    /** (r,c) ile (r,c+1) arasi: +1 ise (r,c)'nin sag kulakcigi disa cikar. */
    val hEdge: Array<IntArray> = Array(rows) { IntArray(cols - 1) { if (random.nextBoolean()) 1 else -1 } }
    /** (r,c) ile (r+1,c) arasi: +1 ise (r,c)'nin alt kulakcigi disa cikar. */
    val vEdge: Array<IntArray> = Array(rows - 1) { IntArray(cols) { if (random.nextBoolean()) 1 else -1 } }

    /** Cizim sirasi (sondaki en ustte). */
    val pieces: MutableList<JigsawPiece> = MutableList(rows * cols) { JigsawPiece(it / cols, it % cols, 0f, 0f) }

    val placedCount: Int get() = pieces.count { it.placed }
    val done: Boolean get() = pieces.all { it.placed }

    /** Kenarlar: ust, sag, alt, sol (+1 disa, -1 ice, 0 duz). */
    fun edges(r: Int, c: Int): IntArray = intArrayOf(
        if (r == 0) 0 else -vEdge[r - 1][c],
        if (c == cols - 1) 0 else hEdge[r][c],
        if (r == rows - 1) 0 else vEdge[r][c],
        if (c == 0) 0 else -hEdge[r][c - 1],
    )

    /**
     * Parcalari tahta disina dagit. Tepsi: tahtanin altinda [trayTop]..[trayBottom] (hucre birimi),
     * genislik [width] hucre. Tepsi cok darsa parcalar tahtanin uzerine de dusebilir.
     */
    fun scatter(trayTop: Float, trayBottom: Float, width: Float = cols.toFloat()) {
        val h = (trayBottom - trayTop - 1f).coerceAtLeast(0.5f)
        pieces.shuffle(random)
        pieces.forEach { p ->
            p.placed = false
            p.x = random.nextFloat() * (width - 1f).coerceAtLeast(0f)
            p.y = trayTop + random.nextFloat() * h
        }
    }

    /** Noktadaki en ustteki tasinabilir parca. */
    fun pick(x: Float, y: Float): JigsawPiece? {
        for (i in pieces.indices.reversed()) {
            val p = pieces[i]
            if (p.placed) continue
            if (x >= p.x - 0.12f && x <= p.x + 1.12f && y >= p.y - 0.12f && y <= p.y + 1.12f) {
                pieces.removeAt(i)
                pieces.add(p)
                return p
            }
        }
        return null
    }

    /** Birakildiginda dogru yere yakinsa oturt. */
    fun drop(p: JigsawPiece): Boolean {
        if (abs(p.x - p.c) < SNAP && abs(p.y - p.r) < SNAP) {
            p.x = p.c.toFloat()
            p.y = p.r.toFloat()
            p.placed = true
            // Yerlesenler alta
            pieces.remove(p)
            pieces.add(0, p)
            return true
        }
        return false
    }

    companion object {
        const val SNAP = 0.3f
    }
}
