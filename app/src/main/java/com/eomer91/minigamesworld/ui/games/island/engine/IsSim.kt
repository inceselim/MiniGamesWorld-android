package com.eomer91.minigamesworld.ui.games.island.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Insa kontrolu sonucu */
enum class BuildError { OK, OUTSIDE, WATER, OCCUPIED, NOT_YOURS, NEEDS_BEACH, NO_MONEY, NOT_BUILDABLE, LIMIT }

object IsSim {
    /** Konaklamada "baska yere gitme" secenegi; amenity'lerde "gitmeme" */
    const val U0_STAY = 2.0
    const val U0_AMENITY = 1.5
    /** Turistin binadan amenity'ye gidebilecegi en uzak mesafe (karo) */
    const val RADIUS = 22
    const val DIST_K = 0.12
    /** Aylik turist artisi (binde 1) */
    const val GROWTH = 1.001
    val SEASON = floatArrayOf(0.45f, 0.5f, 0.6f, 0.75f, 0.9f, 1.15f, 1.4f, 1.45f, 1.1f, 0.8f, 0.55f, 0.5f)

    private val DECAY = DoubleArray(RADIUS + 2) { exp(-DIST_K * it) }

    // ------------------------------------------------------------------ arsa ve insaat

    /** Bir arsa karosunun belediyeden satin alma fiyati */
    fun landPrice(w: IsWorld, x: Int, y: Int): Long {
        val t = w.ter(x, y)
        var p = when (t) { Terrain.SAND -> 9_000.0; Terrain.GRASS -> 6_000.0; Terrain.FOREST -> 7_000.0; Terrain.HILL -> 4_000.0; else -> 0.0 }
        val wd = w.waterDist[w.idx(x, y)].toInt()
        p *= when { wd <= 2 -> 2.5; wd <= 5 -> 1.7; wd <= 10 -> 1.2; else -> 1.0 }
        // Cevrede bina coksa arsa pahalanir
        var cnt = 0
        for (yy in max(0, y - 6)..min(w.n - 1, y + 6)) for (xx in max(0, x - 6)..min(w.n - 1, x + 6)) {
            val b = w.bAt[w.idx(xx, yy)]
            if (b >= 0 && w.buildings[b]?.type?.cat?.let { it != Cat.ROAD && it != Cat.NATURE } == true) cnt++
        }
        p *= min(2.5, 1.0 + 0.012 * cnt)
        // Zaman icinde (turist arttikca) hafif artis
        p *= w.demand[1] / w.settings.mapSize.tourists[1]
        return (p / 100).toLong() * 100
    }

    /** Bu konuma insa etmenin toplam maliyeti (bina + gerekli arsa + agac temizleme) */
    fun buildCost(w: IsWorld, c: Int, type: BType, x: Int, y: Int): Long {
        var cost = if (type.adOnly) 0L else type.cost
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            if (!w.inside(xx, yy)) continue
            val i = w.idx(xx, yy)
            if (w.owner[i] == -1) cost += roadLand(type, landPrice(w, xx, yy))
            if (w.ter(xx, yy) == Terrain.FOREST && type.cat != Cat.NATURE) cost += 2_000
        }
        return cost
    }

    /** Yol kamu yoludur: arsa parasi alinmaz, karo kimsenin mulku olmaz (yalnizca yapim bedeli) */
    private fun roadLand(type: BType, p: Long) = if (type.cat == Cat.ROAD) 0L else p

    fun countOwned(w: IsWorld, c: Int, type: BType) = w.buildings.count { it != null && it.owner == c && it.type === type }

    fun canBuild(w: IsWorld, c: Int, type: BType, x: Int, y: Int): BuildError {
        if (!type.buildable) return BuildError.NOT_BUILDABLE
        if (type.limit > 0 && countOwned(w, c, type) >= type.limit) return BuildError.LIMIT
        var sand = false; var shore = false
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            if (!w.inside(xx, yy)) return BuildError.OUTSIDE
            val i = w.idx(xx, yy)
            val t = w.ter(xx, yy)
            if (!t.isLand) return BuildError.WATER
            if (w.bAt[i] >= 0) return BuildError.OCCUPIED
            if (w.owner[i] != -1 && w.owner[i] != c) return BuildError.NOT_YOURS
            if (t == Terrain.SAND) sand = true
            if (w.waterDist[i].toInt() == 1) shore = true
        }
        if (type.needsBeach && !(sand && shore)) return BuildError.NEEDS_BEACH
        if (w.companies[c].balance < buildCost(w, c, type, x, y)) return BuildError.NO_MONEY
        return BuildError.OK
    }

    fun build(w: IsWorld, c: Int, type: BType, x: Int, y: Int): Building? {
        if (canBuild(w, c, type, x, y) != BuildError.OK) return null
        val co = w.companies[c]
        var land = 0L
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            val i = w.idx(xx, yy)
            if (w.owner[i] == -1 && type.cat != Cat.ROAD) { land += landPrice(w, xx, yy); w.owner[i] = c }
        }
        val structure = if (type.adOnly) 0L else type.cost
        val clear = buildCost(w, c, type, x, y) - structure - land
        co.book(Fin.LAND, land)
        co.book(Fin.BUILD, structure + max(0, clear))
        val b = Building(w.buildings.size, type, c, x, y)
        w.buildings.add(b)
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            val i = w.idx(xx, yy)
            w.bAt[i] = b.id
            if (w.ter(xx, yy) == Terrain.FOREST) { w.terrain[i] = Terrain.GRASS.ordinal.toByte(); w.terrainVersion++ }
        }
        if (b.buildLeft <= 0) refreshLocation(w, b)
        return b
    }

    fun placePublic(w: IsWorld, type: BType, x: Int, y: Int) {
        val b = Building(w.buildings.size, type, -2, x, y)
        b.buildLeft = 0
        w.buildings.add(b)
        for (yy in y until y + type.h) for (xx in x until x + type.w) {
            val i = w.idx(xx, yy)
            w.bAt[i] = b.id; w.owner[i] = -2
            if (w.ter(xx, yy) == Terrain.FOREST) { w.terrain[i] = Terrain.GRASS.ordinal.toByte(); w.terrainVersion++ }
        }
    }

    /** Insaati/yenilemeyi odullu reklamla hizlandir: kalan sure yariya iner (en az 1 gun kalir, ertesi gun acilir) */
    fun rushConstruction(b: Building): Boolean {
        if (b.buildLeft <= 1) return false
        b.buildLeft = max(1, b.buildLeft / 2)
        return true
    }

    // ------------------------------------------------------------------ parseller

    /** Arsalar PARCEL x PARCEL karoluk parseller halinde satilir */
    const val PARCEL = 8

    /** Karonun parselinin sol-ust kosesi */
    fun parcelOrigin(x: Int, y: Int) = Pair(x / PARCEL * PARCEL, y / PARCEL * PARCEL)

    /** Parselin satin alinabilir (bos, kara) karolari */
    fun parcelFreeTiles(w: IsWorld, x: Int, y: Int): List<Int> {
        val (px, py) = parcelOrigin(x, y)
        val out = ArrayList<Int>()
        for (yy in py until min(w.n, py + PARCEL)) for (xx in px until min(w.n, px + PARCEL)) {
            val i = w.idx(xx, yy)
            if (w.owner[i] == -1 && w.bAt[i] < 0 && w.ter(xx, yy).isLand) out.add(i)
        }
        return out
    }

    fun parcelPrice(w: IsWorld, x: Int, y: Int): Long =
        parcelFreeTiles(w, x, y).sumOf { landPrice(w, it % w.n, it / w.n) }

    /** Parselin tum bos kara karolarini al (baskasinin karolarina dokunmaz) */
    fun buyParcel(w: IsWorld, c: Int, x: Int, y: Int): Boolean {
        if (!w.inside(x, y)) return false
        val tiles = parcelFreeTiles(w, x, y)
        if (tiles.isEmpty()) return false
        val p = tiles.sumOf { landPrice(w, it % w.n, it / w.n) }
        val co = w.companies[c]
        if (co.balance < p) return false
        co.book(Fin.LAND, p)
        for (i in tiles) w.owner[i] = c
        return true
    }

    /** Yalnizca arsa satin al (rakibi engellemek ya da sonrasi icin) */
    fun buyLand(w: IsWorld, c: Int, x: Int, y: Int): Boolean {
        if (!w.inside(x, y)) return false
        val i = w.idx(x, y)
        if (w.owner[i] != -1 || !w.ter(x, y).isLand) return false
        val p = landPrice(w, x, y)
        val co = w.companies[c]
        if (co.balance < p) return false
        co.book(Fin.LAND, p); w.owner[i] = c
        return true
    }

    /** Binayi yik: insaat bedelinin %20'si geri gelir; arsa sirkette kalir */
    fun demolish(w: IsWorld, b: Building, refund: Boolean = true) {
        if (b.owner < 0) return
        val co = w.companies[b.owner]
        if (refund && b.buildLeft <= 0 && !b.type.adOnly) co.book(Fin.SALES, b.type.cost / 5)
        for (yy in b.y until b.y + b.type.h) for (xx in b.x until b.x + b.type.w) w.bAt[w.idx(xx, yy)] = -1
        w.buildings[b.id] = null
        if (b.type.cat == Cat.ROAD) w.roadsDirty = true
    }

    /** Iyilestirme: kalite +0.5, kapasite +%25 */
    fun upgradeCost(b: Building): Long = (b.type.cost * (0.45 + 0.25 * b.level)).toLong()
    fun upgrade(w: IsWorld, b: Building): Boolean {
        if (b.level >= 2 || b.buildLeft > 0 || b.type.cap == 0 || b.owner < 0) return false
        val co = w.companies[b.owner]
        val cost = upgradeCost(b)
        if (co.balance < cost) return false
        co.book(Fin.BUILD, cost)
        b.level++
        b.buildLeft = max(3, b.type.days / 3)
        return true
    }

    fun setPrice(b: Building, p: Float) {
        b.price = p.coerceIn(b.type.price * 0.2f, b.type.price * 4f)
    }

    // ------------------------------------------------------------------ konum

    /** Sinif basina konum puani: deniz yakinligi, guzellik, gurultu, cevredeki eglence */
    fun refreshLocation(w: IsWorld, b: Building, large: List<Building> = largeBuildings(w)) {
        val n = w.n
        var minWd = 99
        for (yy in b.y until b.y + b.type.h) for (xx in b.x until b.x + b.type.w) minWd = min(minWd, w.waterDist[w.idx(xx, yy)].toInt())
        var beauty = 0f; var noise = 0f; var attractions = 0
        val r = 6
        for (yy in max(0, b.y - r) until min(n, b.y + b.type.h + r)) for (xx in max(0, b.x - r) until min(n, b.x + b.type.w + r)) {
            val i = w.idx(xx, yy)
            val t = w.ter(xx, yy)
            val d = max(abs(xx - b.cx), abs(yy - b.cy))
            if (t == Terrain.FOREST && d <= 4) beauty += 0.15f
            if (!t.isLand && d <= 4) beauty += 0.05f
            val oid = w.bAt[i]
            if (oid < 0 || oid == b.id) continue
            val o = w.buildings[oid] ?: continue
            // Bir binayi karo sayisi kadar saymamak icin yalnizca sol ust karosunda say
            if (o.x != xx || o.y != yy) continue
            if (o.type.w >= LARGE || o.type.h >= LARGE) continue
            val od = max(abs(o.cx - b.cx), abs(o.cy - b.cy))
            if (od <= 5) { beauty += o.type.beauty; if (o.buildLeft <= 0) noise += o.type.noise }
            if (o.type.cat.isAmenity && od <= 10) attractions++
        }
        // Buyuk yapilar (buyuk park): kenarina 4 karo icindekileri etkiler
        for (o in large) {
            if (o.id == b.id) continue
            if (rectGap(b, o) <= 4) { beauty += o.type.beauty; if (o.buildLeft <= 0) noise += o.type.noise }
        }
        b.road = roadStatus(w, b)
        val beachBonus = when { minWd <= 2 -> 0.6f; minWd <= 5 -> 0.3f; minWd <= 9 -> 0.1f; else -> 0f }
        val beautyBonus = min(0.6f, beauty * 0.05f)
        val attrBonus = if (b.type.cat == Cat.STAY) min(0.5f, attractions * 0.05f) else 0f
        val noiseW = floatArrayOf(0.15f, 0.3f, 0.6f)
        for (c in 0..2) {
            val beachW = if (c == 2) 1.5f else 1f
            val noisePenalty = if (b.type.cat == Cat.STAY) noiseW[c] * min(2f, noise) else 0f
            b.loc[c] = beachBonus * beachW + beautyBonus + attrBonus - noisePenalty + (if (b.type.cat == Cat.STAY) STAY_ROAD[b.road] else 0f)
        }
    }

    /** Yol durumuna gore konaklama konum puani (yolsuz / bitisik / havalimani-limana bagli) */
    val STAY_ROAD = floatArrayOf(-0.8f, -0.1f, 0.25f)
    /** Mekanlar (yeme-icme, eglence...) icin dogrudan cekicilik etkisi */
    val AMENITY_ROAD = doubleArrayOf(-1.2, 0.0, 0.3)
    fun largeBuildings(w: IsWorld): List<Building> = w.buildings.filter { it != null && (it.type.w >= LARGE || it.type.h >= LARGE) }.map { it!! }

    /** Bu boyuttan buyuk yapilar genis alani etkiler */
    private const val LARGE = 6

    private fun rectGap(a: Building, b: Building): Int {
        val dx = max(0, max(b.x - (a.x + a.type.w - 1), a.x - (b.x + b.type.w - 1)))
        val dy = max(0, max(b.y - (a.y + a.type.h - 1), a.y - (b.y + b.type.h - 1)))
        return max(dx, dy)
    }

    // ------------------------------------------------------------------ yollar

    private fun isRoadAt(w: IsWorld, i: Int, builtOnly: Boolean = true): Boolean {
        val id = w.bAt[i]; if (id < 0) return false
        val o = w.buildings[id] ?: return false
        return o.type.cat == Cat.ROAD && (!builtOnly || o.buildLeft <= 0)
    }

    private fun isPublicAt(w: IsWorld, i: Int): Boolean = w.owner[i] == -2 && w.bAt[i] >= 0

    /** Havalimani/limana bitisik yol karolarindan baslayan yol agi */
    fun computeRoadNet(w: IsWorld): BooleanArray {
        val n = w.n
        val conn = BooleanArray(n * n)
        val q = ArrayDeque<Int>()
        for (i in 0 until n * n) {
            if (!isRoadAt(w, i)) continue
            val x = i % n; val y = i / n
            if (NB4.any { (dx, dy) -> w.inside(x + dx, y + dy) && isPublicAt(w, w.idx(x + dx, y + dy)) }) { conn[i] = true; q.add(i) }
        }
        while (q.isNotEmpty()) {
            val i = q.removeFirst(); val x = i % n; val y = i / n
            for ((dx, dy) in NB4) {
                val xx = x + dx; val yy = y + dy
                if (!w.inside(xx, yy)) continue
                val j = w.idx(xx, yy)
                if (!conn[j] && isRoadAt(w, j)) { conn[j] = true; q.add(j) }
            }
        }
        w.roadConn = conn
        return conn
    }

    /** 0 yolsuz, 1 yola bitisik, 2 bitisik yol agi havalimani/limana bagli. Yollar ve susler icin 2. */
    fun roadStatus(w: IsWorld, b: Building): Int {
        if (b.owner < 0 || b.type.cat == Cat.ROAD || b.type.cat == Cat.NATURE) return 2
        val conn = w.roadConn ?: computeRoadNet(w)
        var best = 0
        forEachEdge(w, b) { i ->
            if (isRoadAt(w, i)) best = max(best, if (conn[i]) 2 else 1)
            else if (isPublicAt(w, i)) best = 2
        }
        return best
    }

    /** Binanin ayak izine 4-komsu (disaridaki) karolar */
    private inline fun forEachEdge(w: IsWorld, b: Building, f: (Int) -> Unit) {
        for (xx in b.x until b.x + b.type.w) { if (w.inside(xx, b.y - 1)) f(w.idx(xx, b.y - 1)); if (w.inside(xx, b.y + b.type.h)) f(w.idx(xx, b.y + b.type.h)) }
        for (yy in b.y until b.y + b.type.h) { if (w.inside(b.x - 1, yy)) f(w.idx(b.x - 1, yy)); if (w.inside(b.x + b.type.w, yy)) f(w.idx(b.x + b.type.w, yy)) }
    }

    private val NB4 = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    /**
     * Binayi yol agina (ya da dogrudan havalimani/limana) baglayan en ucuz yol: yeni yol gereken karolar.
     * Var olan yollardan ucretsiz gecer; yeni yol yalnizca bos ya da sirketin kendi arsasina dosenir.
     * Zaten bagliysa bos liste, ulasilamazsa null.
     */
    fun roadPath(w: IsWorld, c: Int, b: Building, maxNew: Int = 40): List<Int>? {
        val n = w.n
        val conn = w.roadConn ?: computeRoadNet(w)
        if (roadStatus(w, b) == 2) return emptyList()
        val dist = IntArray(n * n) { Int.MAX_VALUE }
        val prev = IntArray(n * n) { -1 }
        val dq = ArrayDeque<Int>()
        fun passable(i: Int): Boolean {
            val xx = i % n; val yy = i / n
            if (!w.ter(xx, yy).isLand) return false
            if (isRoadAt(w, i, builtOnly = false)) return true
            return w.bAt[i] < 0 && (w.owner[i] == -1 || w.owner[i] == c)
        }
        fun stepCost(i: Int) = if (isRoadAt(w, i, builtOnly = false)) 0 else 1
        forEachEdge(w, b) { i -> if (passable(i)) { val d = stepCost(i); if (d < dist[i]) { dist[i] = d; if (d == 0) dq.addFirst(i) else dq.addLast(i) } } }
        var goal = -1
        while (dq.isNotEmpty()) {
            val i = dq.removeFirst()
            if (dist[i] > maxNew) break
            val x = i % n; val y = i / n
            // Hedef: bagli yol ya da havalimani/limana bitisik karo
            if ((isRoadAt(w, i) && conn[i]) || NB4.any { (dx, dy) -> w.inside(x + dx, y + dy) && isPublicAt(w, w.idx(x + dx, y + dy)) }) { goal = i; break }
            for ((dx, dy) in NB4) {
                val xx = x + dx; val yy = y + dy
                if (!w.inside(xx, yy)) continue
                val j = w.idx(xx, yy)
                if (!passable(j)) continue
                val nd = dist[i] + stepCost(j)
                if (nd < dist[j]) { dist[j] = nd; prev[j] = i; if (stepCost(j) == 0) dq.addFirst(j) else dq.addLast(j) }
            }
        }
        if (goal < 0) return null
        val out = ArrayList<Int>()
        var i = goal
        while (i >= 0) { if (!isRoadAt(w, i, builtOnly = false)) out.add(i); i = prev[i] }
        return out
    }

    fun roadPathCost(w: IsWorld, c: Int, path: List<Int>): Long = path.sumOf { buildCost(w, c, IsCatalog.ROAD, it % w.n, it / w.n) }

    /** Yolu dose (para yetmezse yetene kadar). Dosenen karo sayisi. */
    fun buildRoadPath(w: IsWorld, c: Int, path: List<Int>): Int {
        var k = 0
        for (i in path) if (build(w, c, IsCatalog.ROAD, i % w.n, i / w.n) != null) k++ else break
        return k
    }

    fun refreshAllLocations(w: IsWorld) {
        computeRoadNet(w)
        w.roadsDirty = false
        val large = largeBuildings(w)
        for (b in w.buildings) if (b != null && b.owner >= 0) refreshLocation(w, b, large)
    }

    // ------------------------------------------------------------------ gunluk dongu

    private fun repTerm(w: IsWorld, owner: Int) = 0.03 * (w.companies[owner].reputation - 50f)
    private fun adTerm(w: IsWorld, owner: Int, c: Int) = if (w.companies[owner].adUntil[c] >= w.day) 0.5 else 0.0

    private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

    /** Bir gun ilerlet */
    fun step(w: IsWorld) {
        if (w.gameOver) return
        // Insaat ve kapali binalar
        for (b in w.buildings) {
            if (b == null || b.owner < 0) continue
            b.today.fill(0f)
            if (b.closedDays > 0) b.closedDays--
            if (b.buildLeft > 0) {
                b.buildLeft--
                if (b.buildLeft == 0) {
                    if (b.type.cat == Cat.ROAD) w.roadsDirty = true
                    refreshLocation(w, b)
                    w.addNews(IsNews(w.day, NewsKind.BUILT, b.owner, type = b.type.id))
                }
            }
        }
        if (w.roadsDirty) refreshAllLocations(w)
        val season = SEASON[w.month]
        val want = DoubleArray(3) { w.demand[it] * season }
        allocateStays(w, want)
        for (cat in Cat.entries) if (cat.isAmenity) allocateAmenity(w, cat)
        if (w.day % 7 == 6) refreshAllLocations(w)
        IsRivals.dailyAuctions(w)
        IsBots.daily(w)
        if (w.dayOfMonth == 30) monthEnd(w)
        w.day++
    }

    private fun allocateStays(w: IsWorld, want: DoubleArray) {
        val stays = w.buildings.filter { it != null && it.owner >= 0 && it.open && it.type.cat == Cat.STAY && w.companies[it.owner].alive }.map { it!! }
        val m = stays.size
        val e = Array(3) { DoubleArray(m) }
        for (c in 0..2) {
            val cl = TClass.entries[c]
            for (i in 0 until m) {
                val b = stays[i]
                val ap = b.type.appeal[c]
                if (ap <= 0f) { e[c][i] = 0.0; continue }
                val u = 3.0 * ap + 0.3 * cl.qualityW * b.quality + repTerm(w, b.owner) + adTerm(w, b.owner, c) + b.loc[c] - cl.sens * b.price / cl.stayBudget
                e[c][i] = exp(u)
            }
        }
        val capLeft = DoubleArray(m) { stays[it].capacity.toDouble() }
        val full = BooleanArray(m)
        val assign = Array(3) { DoubleArray(m) }
        val remaining = want.copyOf()
        val e0 = exp(U0_STAY)
        val lost = DoubleArray(3)
        repeat(4) {
            val wants = Array(3) { DoubleArray(m) }
            for (c in 0..2) {
                if (remaining[c] <= 0.0) continue
                var denom = e0
                for (i in 0 until m) if (!full[i]) denom += e[c][i]
                for (i in 0 until m) if (!full[i]) wants[c][i] = remaining[c] * e[c][i] / denom
                lost[c] += remaining[c] * e0 / denom
                remaining[c] = 0.0
            }
            for (i in 0 until m) {
                if (full[i]) continue
                val tw = wants[0][i] + wants[1][i] + wants[2][i]
                if (tw <= 0.0) continue
                val f = if (tw > capLeft[i]) { full[i] = true; capLeft[i] / tw } else 1.0
                for (c in 0..2) {
                    assign[c][i] += wants[c][i] * f
                    remaining[c] += wants[c][i] * (1 - f)
                }
                capLeft[i] -= tw * f
            }
        }
        for (c in 0..2) {
            lost[c] += remaining[c]
            w.lost[c] = lost[c].toFloat()
            var p = 0.0
            for (i in 0 until m) p += assign[c][i]
            w.present[c] = p.toFloat()
        }
        // Gelir, gider, memnuniyet
        for (i in 0 until m) {
            val b = stays[i]
            val co = w.companies[b.owner]
            var guests = 0.0
            for (c in 0..2) {
                val g = assign[c][i]
                if (g <= 0.0) continue
                b.today[c] = g.toFloat()
                guests += g
                val cl = TClass.entries[c]
                val v = 3.0 * b.type.appeal[c] + 0.3 * cl.qualityW * b.quality + b.loc[c] - cl.sens * b.price / cl.stayBudget
                co.satSum += g * sigmoid(1.2 * (v - 1.0)); co.satW += g
            }
            if (guests <= 0.0) continue
            val rev = (guests * b.price).toLong()
            co.book(Fin.STAY, rev)
            co.book(Fin.OPERATING, (guests * b.type.varCost).toLong())
            b.monthRevenue += rev; b.monthUse += guests.toFloat()
        }
    }

    private fun allocateAmenity(w: IsWorld, cat: Cat) {
        val ams = w.buildings.filter { it != null && it.owner >= 0 && it.open && it.type.cat == cat && w.companies[it.owner].alive }.map { it!! }
        if (ams.isEmpty()) return
        val rate = cat.visitRate!!; val budget = cat.visitBudget!!
        val m = ams.size
        // Mesafeden bagimsiz cekicilik
        val base = Array(3) { DoubleArray(m) }
        for (c in 0..2) {
            val cl = TClass.entries[c]
            for (i in 0 until m) {
                val b = ams[i]
                val ap = b.type.appeal[c]
                if (ap <= 0f) continue
                val u = 3.0 * ap + 0.3 * cl.qualityW * b.quality + repTerm(w, b.owner) + adTerm(w, b.owner, c) +
                    0.3 * (b.loc[c].coerceAtMost(0.6f)) - cl.sens * b.price / budget[c] + AMENITY_ROAD[b.road]
                base[c][i] = exp(u)
            }
        }
        val wantV = Array(3) { DoubleArray(m) }
        val e0 = exp(U0_AMENITY)
        val ew = DoubleArray(m)
        for (h in w.buildings) {
            if (h == null || h.owner < 0 || h.type.cat != Cat.STAY) continue
            val g = h.today
            if (g[0] + g[1] + g[2] <= 0f) continue
            for (i in 0 until m) {
                val d = max(abs(ams[i].cx - h.cx), abs(ams[i].cy - h.cy)).toInt()
                ew[i] = if (d > RADIUS) -1.0 else DECAY[d]
            }
            for (c in 0..2) {
                if (g[c] <= 0f) continue
                var denom = e0
                for (i in 0 until m) if (ew[i] > 0) denom += base[c][i] * ew[i]
                if (denom <= e0) continue
                val v = g[c] * rate[c]
                for (i in 0 until m) if (ew[i] > 0) wantV[c][i] += v * base[c][i] * ew[i] / denom
            }
        }
        for (i in 0 until m) {
            val b = ams[i]
            val tw = wantV[0][i] + wantV[1][i] + wantV[2][i]
            if (tw <= 0.0) continue
            val served = min(tw, b.capacity.toDouble())
            val f = served / tw
            val co = w.companies[b.owner]
            for (c in 0..2) {
                val v = wantV[c][i] * f
                b.today[c] = v.toFloat()
                if (v <= 0.0) continue
                val cl = TClass.entries[c]
                val val0 = 3.0 * b.type.appeal[c] + 0.3 * cl.qualityW * b.quality - cl.sens * b.price / budget[c]
                // Kalabalik (kapasite dolu) memnuniyeti dusurur
                val crowd = if (f < 1.0) 0.6 * (1 - f) else 0.0
                co.satSum += 0.3 * v * sigmoid(1.2 * (val0 - 1.0) - crowd); co.satW += 0.3 * v
            }
            val rev = (served * b.price).toLong()
            co.book(cat.fin!!, rev)
            co.book(Fin.OPERATING, (served * b.type.varCost).toLong())
            b.monthRevenue += rev; b.monthUse += served.toFloat()
        }
    }

    // ------------------------------------------------------------------ ay sonu

    private fun monthEnd(w: IsWorld) {
        for (co in w.companies) {
            if (!co.alive) continue
            // Bakim
            var upkeep = 0L
            for (b in w.buildings) if (b != null && b.owner == co.id) upkeep += if (b.buildLeft > 0 && b.level == 0) b.type.upkeep / 4 else (b.type.upkeep * (1 + 0.2 * b.level)).toLong()
            co.book(Fin.UPKEEP, upkeep)
            // Kredi taksiti
            if (co.loanMonthsLeft > 0 && co.loanLeft > 0) {
                val interest = (co.loanLeft * co.loanRate / 100.0 / 12.0).toLong()
                var principal = co.loanPayment - interest
                if (co.loanMonthsLeft <= 1 || principal >= co.loanLeft) principal = co.loanLeft
                co.book(Fin.INTEREST, interest)
                co.balance -= principal; co.monthOut += principal
                co.loanLeft -= principal; co.loanMonthsLeft--
                if (co.loanLeft <= 0 || co.loanMonthsLeft <= 0) clearLoan(co)
            }
            // Eksi bakiye faizi (%1.5/ay)
            if (co.balance < 0) co.book(Fin.INTEREST, (-co.balance * 0.015).toLong())
            // Itibar
            if (co.satW > 0) {
                val target = (co.satSum / co.satW * 100).toFloat()
                co.reputation += 0.25f * (target - co.reputation)
            } else co.reputation += 0.1f * (50f - co.reputation)
            co.reputation = co.reputation.coerceIn(0f, 100f)
            co.satSum = 0.0; co.satW = 0.0
            // Iflas takibi
            co.redMonths = if (co.balance < 0) co.redMonths + 1 else 0
            co.lastMonthIn = co.monthIn; co.lastMonthOut = co.monthOut
            co.monthIn = 0; co.monthOut = 0
        }
        for (b in w.buildings) if (b != null) {
            b.lastMonthRevenue = b.monthRevenue; b.monthRevenue = 0
            b.lastMonthUse = b.monthUse; b.monthUse = 0f
        }
        for (c in 0..2) w.demand[c] *= GROWTH
        // Mali yil: her 12 ayda bir defter devri
        if ((w.day + 1) % 360 == 0) for (co in w.companies) {
            co.ledger.copyInto(co.lastLedger); co.ledger.fill(0)
        }
        IsRivals.monthEnd(w)
    }

    // ------------------------------------------------------------------ kredi

    fun annualRevenue(co: Company): Long {
        val last = co.lastLedger.withIndex().filter { Fin.entries[it.index].income && it.index != Fin.SALES.ordinal }.sumOf { it.value }
        return if (last > 0) last else co.lastMonthIn * 12
    }

    /** Binanin tahmini degeri (insaat bedelinin %60'i, iyilestirmelerle artar) */
    fun buildingValue(b: Building): Long = (b.type.cost * (0.6 + 0.15 * b.level)).toLong()

    /** Sirketin binalarinin tahmini degeri */
    fun assets(w: IsWorld, c: Int): Long {
        var v = 0L
        for (b in w.buildings) if (b != null && b.owner == c) v += buildingValue(b)
        return v
    }

    fun netWorth(w: IsWorld, c: Int): Long { val co = w.companies[c]; return co.balance + assets(w, c) - co.loanLeft }

    fun creditLimit(w: IsWorld, c: Int): Long {
        val co = w.companies[c]
        val base = max(annualRevenue(co) / 2, assets(w, c) / 3)
        var lim = max(500_000L, base) - co.loanLeft
        if (co.balance < 0) lim += co.balance / 2
        return max(0L, lim / 10_000 * 10_000)
    }

    fun loanRate(w: IsWorld, c: Int, years: Int): Float {
        val co = w.companies[c]
        var r = 6f + (1f - co.reputation / 100f) * 6f + (years - 1) * 0.5f
        if (co.balance < 0) r += 3f
        return (r * 10).toInt() / 10f
    }

    fun monthlyPayment(amount: Long, rate: Float, months: Int): Long {
        val r = rate / 100.0 / 12.0
        if (r <= 0) return amount / months
        return (amount * r / (1 - (1 + r).pow(-months))).toLong()
    }

    fun takeLoan(w: IsWorld, c: Int, amount: Long, years: Int): Boolean {
        val co = w.companies[c]
        if (co.loanMonthsLeft > 0 || amount <= 0 || amount > creditLimit(w, c)) return false
        co.loanRate = loanRate(w, c, years)
        co.loanLeft = amount
        co.loanMonthsLeft = years * 12
        co.loanPayment = monthlyPayment(amount, co.loanRate, years * 12)
        co.balance += amount
        return true
    }

    fun repayCost(co: Company) = co.loanLeft + co.loanLeft / 100

    fun repayLoan(w: IsWorld, c: Int): Boolean {
        val co = w.companies[c]
        if (co.loanMonthsLeft <= 0) return false
        val cost = repayCost(co)
        if (co.balance < cost) return false
        co.balance -= co.loanLeft
        co.book(Fin.INTEREST, cost - co.loanLeft)
        clearLoan(co)
        return true
    }

    private fun clearLoan(co: Company) { co.loanLeft = 0; co.loanMonthsLeft = 0; co.loanPayment = 0; co.loanRate = 0f }
}
