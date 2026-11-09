package com.eomer91.minigamesworld.ui.games.island.engine

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Yeni oyun: harita, kamu binalari, sirketler. */
object IsGen {

    val BOT_NAMES = listOf("Sunwave", "Coral Bay", "Palmira", "Blue Lagoon", "Golden Sands", "Azure Resorts", "Tropicana", "Seabreeze")
    val BOT_COLORS = intArrayOf(0xFFE74C3C.toInt(), 0xFF27AE60.toInt(), 0xFFF39C12.toInt(), 0xFF8E44AD.toInt(), 0xFF16A085.toInt())

    fun newGame(s: IsSettings): IsWorld {
        val w = IsWorld(s, s.mapSize.n)
        genTerrain(w)
        computeWaterDist(w)
        placePublic(w)
        for (c in 0..2) w.demand[c] = s.mapSize.tourists[c].toDouble()
        // Sirketler
        val user = Company(0, s.companyName.ifBlank { "Player" }, s.color, false, Personality.BALANCED)
        user.balance = s.difficulty.startMoney
        w.companies.add(user)
        val names = BOT_NAMES.toMutableList()
        val pers = listOf(Personality.LUXURY, Personality.BUDGET, Personality.FUN, Personality.AGGRESSIVE, Personality.BALANCED)
        for (i in 1..s.bots.coerceIn(1, 5)) {
            val name = names.removeAt(w.rnd.int(names.size))
            val c = Company(i, name, BOT_COLORS[(i - 1) % BOT_COLORS.size], true, pers[(i - 1) % pers.size])
            c.balance = s.difficulty.botMoney
            w.companies.add(c)
        }
        return w
    }

    // ------------------------------------------------------------------ arazi

    private fun hash(x: Int, y: Int, seed: Long): Float {
        var h = seed xor (x * 374761393L) xor (y * 668265263L)
        h = (h xor (h ushr 13)) * 1274126177L
        h = h xor (h ushr 16)
        return ((h and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat())
    }

    private fun valueNoise(x: Float, y: Float, seed: Long): Float {
        val x0 = floor(x).toInt(); val y0 = floor(y).toInt()
        val fx = x - x0; val fy = y - y0
        val sx = fx * fx * (3 - 2 * fx); val sy = fy * fy * (3 - 2 * fy)
        val a = hash(x0, y0, seed); val b = hash(x0 + 1, y0, seed)
        val c = hash(x0, y0 + 1, seed); val d = hash(x0 + 1, y0 + 1, seed)
        return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy
    }

    /** 0..1 fraktal gurultu */
    fun fbm(x: Float, y: Float, seed: Long, oct: Int = 4): Float {
        var amp = 1f; var f = 1f; var sum = 0f; var norm = 0f
        for (o in 0 until oct) {
            sum += amp * valueNoise(x * f, y * f, seed + o * 101)
            norm += amp; amp *= 0.5f; f *= 2f
        }
        return sum / norm
    }

    private fun genTerrain(w: IsWorld) {
        val n = w.n
        val type = if (w.settings.mapType == MapType.RANDOM)
            listOf(MapType.ISLAND, MapType.ARCHIPELAGO, MapType.COAST)[w.rnd.int(3)] else w.settings.mapType
        val seed = w.rnd.nextLong()
        // Takimada icin ada merkezleri
        val centers = ArrayList<FloatArray>()
        if (type == MapType.ARCHIPELAGO) {
            val k = 4 + n / 48
            var guard = 0
            while (centers.size < k && guard++ < 500) {
                val cx = 0.15f + w.rnd.float() * 0.7f; val cy = 0.15f + w.rnd.float() * 0.7f
                if (centers.all { (it[0] - cx) * (it[0] - cx) + (it[1] - cy) * (it[1] - cy) > 0.07f })
                    centers.add(floatArrayOf(cx, cy, 0.13f + w.rnd.float() * 0.09f))
            }
        }
        val bayAngle = w.rnd.float() * 6.28f
        val height = FloatArray(n * n)
        for (y in 0 until n) for (x in 0 until n) {
            val u = x / n.toFloat(); val v = y / n.toFloat()
            val noise = fbm(u * 6f, v * 6f, seed)
            val hgt = when (type) {
                MapType.ISLAND, MapType.RANDOM -> {
                    val dx = u - 0.5f; val dy = v - 0.5f
                    val d = sqrt(dx * dx + dy * dy) / 0.5f
                    1.05f - d * 1.25f + (noise - 0.5f) * 0.75f
                }
                MapType.ARCHIPELAGO -> {
                    var best = -1f
                    for (c in centers) {
                        val dx = u - c[0]; val dy = v - c[1]
                        val d = sqrt(dx * dx + dy * dy) / c[2]
                        best = max(best, 1f - d)
                    }
                    best * 0.9f + (noise - 0.5f) * 0.55f
                }
                MapType.COAST -> {
                    // Kara bir kenardan gelir, korfezler gurultuden
                    val ax = kotlin.math.cos(bayAngle); val ay = kotlin.math.sin(bayAngle)
                    val t = ((u - 0.5f) * ax + (v - 0.5f) * ay) // -0.7..0.7
                    0.25f + t * 1.6f + (noise - 0.5f) * 0.9f
                }
            }
            height[y * n + x] = hgt
        }
        // Kenarlarda her zaman deniz
        for (y in 0 until n) for (x in 0 until n) {
            val e = min(min(x, y), min(n - 1 - x, n - 1 - y))
            if (e < 3) height[y * n + x] = min(height[y * n + x], -0.3f + e * 0.05f)
        }
        val forestSeed = seed + 77
        for (y in 0 until n) for (x in 0 until n) {
            val h = height[y * n + x]
            val t = when {
                h < -0.12f -> Terrain.DEEP
                h < 0.0f -> Terrain.WATER
                h < 0.08f -> Terrain.SAND
                h > 0.62f -> Terrain.HILL
                fbm(x / n.toFloat() * 9f, y / n.toFloat() * 9f, forestSeed, 3) > 0.6f -> Terrain.FOREST
                else -> Terrain.GRASS
            }
            w.terrain[y * n + x] = t.ordinal.toByte()
        }
        // Karaya bitisik ama SAND olmayan kiyilari kumsal yap (daha cok plaj)
        val copy = w.terrain.copyOf()
        for (y in 1 until n - 1) for (x in 1 until n - 1) {
            val t = Terrain.entries[copy[y * n + x].toInt()]
            if (t == Terrain.GRASS || t == Terrain.FOREST) {
                var nearWater = false
                for (dy in -1..1) for (dx in -1..1) {
                    val tt = Terrain.entries[copy[(y + dy) * n + x + dx].toInt()]
                    if (!tt.isLand) nearWater = true
                }
                if (nearWater) w.terrain[y * n + x] = Terrain.SAND.ordinal.toByte()
            }
        }
    }

    /** Her kara karosunun en yakin suya uzakligi (BFS), en fazla 30 */
    fun computeWaterDist(w: IsWorld) {
        val n = w.n
        val dist = IntArray(n * n) { Int.MAX_VALUE }
        val q = IntArray(n * n); var head = 0; var tail = 0
        for (i in 0 until n * n) if (!Terrain.entries[w.terrain[i].toInt()].isLand) { dist[i] = 0; q[tail++] = i }
        while (head < tail) {
            val i = q[head++]; val x = i % n; val y = i / n
            val nd = dist[i] + 1
            if (x > 0 && dist[i - 1] > nd) { dist[i - 1] = nd; q[tail++] = i - 1 }
            if (x < n - 1 && dist[i + 1] > nd) { dist[i + 1] = nd; q[tail++] = i + 1 }
            if (y > 0 && dist[i - n] > nd) { dist[i - n] = nd; q[tail++] = i - n }
            if (y < n - 1 && dist[i + n] > nd) { dist[i + n] = nd; q[tail++] = i + n }
        }
        for (i in 0 until n * n) w.waterDist[i] = min(30, dist[i]).toByte()
    }

    /** En buyuk kara parcasina havalimani, kiyiya iskele */
    private fun placePublic(w: IsWorld) {
        val n = w.n
        // Kara parcalari
        val comp = IntArray(n * n) { -1 }
        var best = -1; var bestSize = 0; var id = 0
        val stack = IntArray(n * n)
        for (i in 0 until n * n) {
            if (comp[i] >= 0 || !Terrain.entries[w.terrain[i].toInt()].isLand) continue
            var sp = 0; stack[sp++] = i; comp[i] = id; var size = 0
            while (sp > 0) {
                val j = stack[--sp]; size++
                val x = j % n; val y = j / n
                for ((dx, dy) in DIRS) {
                    val nx = x + dx; val ny = y + dy
                    if (!w.inside(nx, ny)) continue
                    val k = ny * n + nx
                    if (comp[k] < 0 && Terrain.entries[w.terrain[k].toInt()].isLand) { comp[k] = id; stack[sp++] = k }
                }
            }
            if (size > bestSize) { bestSize = size; best = id }
            id++
        }
        // Havalimani: en buyuk parcada, sudan uzak, merkeze yakin duz yer
        val ap = IsCatalog.AIRPORT
        var bx = -1; var by = -1; var bs = -1f
        for (y in 2 until n - ap.h - 2) for (x in 2 until n - ap.w - 2) {
            if (comp[y * n + x] != best) continue
            var ok = true; var minWd = 99
            for (yy in y until y + ap.h) for (xx in x until x + ap.w) {
                val t = w.ter(xx, yy)
                if (t != Terrain.GRASS && t != Terrain.FOREST) ok = false
                minWd = min(minWd, w.waterDist[w.idx(xx, yy)].toInt())
            }
            if (!ok) continue
            // Kiyiyi sahillere birakmak icin orta mesafe tercih
            val score = -abs(minWd - 6f) - 0.02f * (abs(x - n / 2f) + abs(y - n / 2f))
            if (score > bs) { bs = score; bx = x; by = y }
        }
        if (bx >= 0) IsSim.placePublic(w, ap, bx, by)
        // Iskele: kumsal karolarinda, denize bitisik
        val hb = IsCatalog.HARBOR
        var hx = -1; var hy = -1; var hs = -1f
        for (y in 2 until n - hb.h - 2) for (x in 2 until n - hb.w - 2) {
            var ok = true; var sand = 0
            for (yy in y until y + hb.h) for (xx in x until x + hb.w) {
                val t = w.ter(xx, yy)
                if (!t.isLand || w.bAt[w.idx(xx, yy)] >= 0) ok = false
                if (t == Terrain.SAND) sand++
            }
            if (!ok || sand == 0 || comp[y * n + x] != best) continue
            val d = if (bx >= 0) abs(x - bx) + abs(y - by) else 0
            val score = sand + d * 0.05f
            if (score > hs) { hs = score; hx = x; hy = y }
        }
        if (hx >= 0) IsSim.placePublic(w, hb, hx, hy)
    }

    val DIRS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
}
