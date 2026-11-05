package com.eomer91.minigamesworld.ui.games.mahjong

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Mahjong Solitaire kurallari ve sonsuz seviye uretimi.
 *
 * Koordinatlar yarim tas birimindedir: bir tas [x, x+2) x [y, y+2) alanini kaplar, z kattir.
 * Bir tas "acik"tir: ustunde ona degen tas yoksa ve sol ya da sag yani bossa.
 * Her seviye numarasi kendi tahtasini uretir (ayni seviye her cihazda ayni tahta).
 */

/** Tahtadaki konum. */
data class Slot(val x: Int, val y: Int, val z: Int)

@Serializable
data class MahjongTile(val id: Int, val x: Int, val y: Int, val z: Int, val face: Int)

/** Seviye bilgisi: tema (bolum), dizilim adi, tas sayisi, tur sayisi. */
data class LevelInfo(
    val level: Int,
    val chapter: Int,
    val theme: Int,
    val layout: String,
    val tiles: Int,
    val faceTypes: Int,
)

/** Tekrarlanabilir rastgele sayi ureteci (SplitMix64); cihazdan bagimsiz ayni sonuc. */
class SeededRandom(seed: Long) {
    private var s = seed
    fun nextLong(): Long {
        s += -0x61c8864680b583ebL
        var z = s
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
    fun nextInt(n: Int): Int = ((nextLong() ushr 1) % n).toInt()
    fun nextFloat(): Float = (nextLong() ushr 40).toFloat() / (1L shl 24).toFloat()
    fun chance(p: Float) = nextFloat() < p
    fun <T> pick(list: List<T>): T = list[nextInt(list.size)]
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val t = list[i]; list[i] = list[j]; list[j] = t
        }
    }
}

object MahjongRules {
    /** [a] tasina dokunan/ustunu kapatan tas [b] mi? (b bir ust katta ve alanlari kesisiyor) */
    fun covers(top: Slot, bottom: Slot): Boolean =
        top.z == bottom.z + 1 && abs(top.x - bottom.x) < 2 && abs(top.y - bottom.y) < 2

    fun leftOf(a: Slot, b: Slot): Boolean = a.z == b.z && b.x == a.x - 2 && abs(a.y - b.y) < 2
    fun rightOf(a: Slot, b: Slot): Boolean = a.z == b.z && b.x == a.x + 2 && abs(a.y - b.y) < 2

    /** [present] icinde [s] acik mi? */
    fun isFree(s: Slot, present: Collection<Slot>): Boolean {
        var left = false
        var right = false
        for (o in present) {
            if (o === s) continue
            if (covers(o, s)) return false
            if (leftOf(s, o)) left = true
            if (rightOf(s, o)) right = true
            if (left && right) return false
        }
        return true
    }
}

/** Dizilim (tas konumlari) ureticisi. */
object MahjongLayouts {
    /** Ekrana sigmasi icin en fazla sutun/satir (tam tas). */
    const val MAX_COLS = 8
    const val MAX_ROWS = 10

    private class Mask(val cols: Int, val rows: Int) {
        val on = Array(rows) { BooleanArray(cols) }
        operator fun get(c: Int, r: Int) = c in 0 until cols && r in 0 until rows && on[r][c]
        operator fun set(c: Int, r: Int, v: Boolean) { if (c in 0 until cols && r in 0 until rows) on[r][c] = v }
        fun count() = on.sumOf { row -> row.count { it } }
        fun mirrorX() { for (r in 0 until rows) for (c in 0 until cols / 2) if (on[r][c]) on[r][cols - 1 - c] = true }
        fun mirrorY() { for (r in 0 until rows / 2) for (c in 0 until cols) if (on[r][c]) on[rows - 1 - r][c] = true }
        fun copy() = Mask(cols, rows).also { m -> for (r in 0 until rows) for (c in 0 until cols) m.on[r][c] = on[r][c] }
        /** Kenardaki (4 komsusundan biri bos) hucreleri sil. */
        fun eroded(): Mask {
            val m = Mask(cols, rows)
            for (r in 0 until rows) for (c in 0 until cols)
                m.on[r][c] = this[c, r] && this[c - 1, r] && this[c + 1, r] && this[c, r - 1] && this[c, r + 1]
            return m
        }
    }

    /** Temel sekil aileleri. */
    val FAMILIES = listOf(
        "rect", "pyramid", "diamond", "circle", "heart", "cross", "frame", "hourglass", "butterfly",
        "turtle", "towers", "bridge", "zigzag", "stairs", "arrow", "flower", "fortress", "wings",
        "triangle", "checker", "columns", "spiral", "blob", "twin",
    )

    private fun shape(family: String, cols: Int, rows: Int, rnd: SeededRandom): Mask {
        val m = Mask(cols, rows)
        val cx = (cols - 1) / 2f
        val cy = (rows - 1) / 2f
        fun fill(f: (Int, Int) -> Boolean) { for (r in 0 until rows) for (c in 0 until cols) if (f(c, r)) m[c, r] = true }
        when (family) {
            "rect", "pyramid", "fortress", "towers" -> fill { _, _ -> true }
            "diamond" -> fill { c, r -> abs(c - cx) / (cols / 2f) + abs(r - cy) / (rows / 2f) <= 1.05f }
            "circle", "flower" -> fill { c, r -> sq((c - cx) / (cols / 2f)) + sq((r - cy) / (rows / 2f)) <= 1.1f }
            "heart" -> fill { c, r ->
                val x = (c - cx) / (cols / 2f) * 1.25f
                val y = -(r - cy) / (rows / 2f) * 1.25f + 0.2f
                val a = x * x + y * y - 1
                a * a * a - x * x * y * y * y <= 0
            }
            "cross" -> fill { c, r -> abs(c - cx) <= cols / 6f + 0.6f || abs(r - cy) <= rows / 6f + 0.6f }
            "frame" -> fill { c, r -> c <= 1 || r <= 1 || c >= cols - 2 || r >= rows - 2 || (abs(c - cx) < 1f && abs(r - cy) < 1f) }
            "hourglass" -> fill { c, r -> abs(c - cx) <= abs(r - cy) * cols / rows.toFloat() + 0.8f }
            "butterfly", "wings" -> fill { c, r -> abs(r - cy) <= abs(c - cx) * rows / cols.toFloat() + 1.2f || abs(c - cx) < 0.6f }
            "turtle" -> {
                fill { c, r -> c in 1 until cols - 1 && r in 1 until rows - 1 }
                m[0, rows / 2] = true; m[cols - 1, rows / 2] = true; m[cols / 2, 0] = true
                m[1, 0] = true; m[cols - 2, 0] = true; m[1, rows - 1] = true; m[cols - 2, rows - 1] = true
            }
            "bridge" -> fill { c, r -> r >= rows - 3 || c <= 1 || c >= cols - 2 || r <= 1 }
            "zigzag" -> fill { c, r -> ((r / 2) % 2 == 0 && c < cols - 1) || ((r / 2) % 2 == 1 && c > 0) }
            "stairs" -> fill { c, r -> c <= r * cols / rows + 1 }
            "arrow" -> fill { c, r -> abs(c - cx) <= (rows - r) * cols / (2f * rows) + 0.4f || (r > rows / 2 && abs(c - cx) < 1.2f) }
            "triangle" -> fill { c, r -> abs(c - cx) <= (r + 1) * cols / (2f * rows) + 0.3f }
            "checker" -> fill { c, r -> (c + r) % 2 == 0 || r == 0 || r == rows - 1 }
            "columns" -> fill { c, _ -> c % 3 != 2 || c == cols - 1 }
            "spiral" -> {
                var x0 = 0; var y0 = 0; var x1 = cols - 1; var y1 = rows - 1
                while (x0 <= x1 && y0 <= y1) {
                    for (c in x0..x1) m[c, y0] = true
                    for (r in y0..y1) m[x1, r] = true
                    for (c in x0..x1) m[c, y1] = true
                    for (r in y0 + 2..y1) m[x0, r] = true
                    x0 += 2; y0 += 2; x1 -= 2; y1 -= 2
                }
            }
            "twin" -> fill { c, r -> sq((c - cols * 0.27f) / (cols * 0.3f)) + sq((r - cy) / (rows / 2f)) <= 1f }
            else -> { // blob: rastgele, sonra aynalanir
                fill { c, r -> sq((c - cx) / (cols / 2f)) + sq((r - cy) / (rows / 2f)) <= 0.6f + rnd.nextFloat() * 0.6f }
            }
        }
        if (family == "twin" || family == "blob") m.mirrorX()
        return m
    }

    private fun sq(v: Float) = v * v

    /**
     * Seviye icin dizilim. Tas sayisi yaklasik [target] olur, her zaman cift sayidir.
     */
    fun build(level: Int, target: Int, maxLayers: Int, rnd: SeededRandom): Pair<String, List<Slot>> {
        repeat(30) {
            val family = FAMILIES[(level * 7 + rnd.nextInt(FAMILIES.size)) % FAMILIES.size]
            val slots = tryBuild(family, target, maxLayers, rnd)
            if (slots != null) return family to slots
        }
        return "rect" to tryBuild("rect", target, maxLayers, rnd)!!
    }

    private fun tryBuild(family: String, target: Int, maxLayers: Int, rnd: SeededRandom): List<Slot>? {
        // Tabani hedef sayinin ~%55-75'i olacak sekilde boyutla.
        val baseShare = when {
            maxLayers <= 1 -> 1f
            maxLayers == 2 -> 0.68f
            else -> 0.56f
        }
        var cols = 4
        var rows = 4
        var base: Mask = shape(family, cols, rows, rnd)
        var guard = 0
        while (base.count() < target * baseShare && guard++ < 40) {
            if (cols < MAX_COLS && (cols * 1.3f <= rows || rows >= MAX_ROWS || rnd.chance(0.5f))) cols++
            else if (rows < MAX_ROWS) rows++
            else break
            base = shape(family, cols, rows, rnd)
        }
        if (base.count() < 4) return null
        val stagger = family in setOf("zigzag", "wings", "blob") || (family !in setOf("checker", "columns", "spiral") && rnd.chance(0.25f))

        val slots = ArrayList<Slot>()
        fun addLayer(mask: Mask, z: Int, half: Boolean) {
            for (r in 0 until mask.rows) for (c in 0 until mask.cols) if (mask[c, r]) {
                val sx = if (stagger && r % 2 == 1) 1 else 0
                slots.add(Slot(c * 2 + sx + if (half) 1 else 0, r * 2 + if (half) 1 else 0, z))
            }
        }
        addLayer(base, 0, false)

        // Ust katlar: asindirma (erozyon) ile kuculen katlar; bazen yarim kaydirmali.
        var layer = base
        var z = 1
        while (z < maxLayers && slots.size < target) {
            val next: Mask = when (family) {
                "towers", "columns" -> Mask(layer.cols, layer.rows).also { t ->
                    for (r in 0 until layer.rows) for (c in 0 until layer.cols)
                        if (layer[c, r] && (c + r) % 3 == 0) t[c, r] = true
                }
                "fortress" -> Mask(layer.cols, layer.rows).also { t ->
                    for (r in 0 until layer.rows) for (c in 0 until layer.cols)
                        if (layer[c, r] && (c == 0 || r == 0 || c == layer.cols - 1 || r == layer.rows - 1 || (c == layer.cols / 2 && r == layer.rows / 2))) t[c, r] = true
                }
                "flower" -> layer.eroded().also { e -> if (e.count() == 0) e[layer.cols / 2, layer.rows / 2] = layer[layer.cols / 2, layer.rows / 2] }
                else -> layer.eroded()
            }
            if (next.count() == 0) break
            val room = target - slots.size
            if (next.count() > room + 4) {
                // Fazlaysa merkezden uzak hucreleri at.
                trimTo(next, room)
            }
            if (next.count() == 0) break
            addLayer(next, z, half = false)
            layer = next
            z++
        }
        // Tek tas en ustte kalsin diye bazen tepe tasi
        if (maxLayers >= 3 && rnd.chance(0.35f)) {
            val top = slots.filter { it.z == z - 1 }
            if (top.size >= 4) {
                val minX = top.minOf { it.x }; val maxX = top.maxOf { it.x }
                val minY = top.minOf { it.y }; val maxY = top.maxOf { it.y }
                val cxh = (minX + maxX) / 2; val cyh = (minY + maxY) / 2
                val supported = top.count { abs(it.x - cxh) < 2 && abs(it.y - cyh) < 2 }
                if (supported >= 2) slots.add(Slot(cxh, cyh, z))
            }
        }
        // Cift sayi ve sinir
        while (slots.size > target + 6) removeTop(slots)
        if (slots.size % 2 == 1) removeTop(slots)
        return if (slots.size >= 8) slots else null
    }

    private fun trimTo(m: Mask, n: Int) {
        val cx = (m.cols - 1) / 2f
        val cy = (m.rows - 1) / 2f
        val cells = ArrayList<Pair<Int, Int>>()
        for (r in 0 until m.rows) for (c in 0 until m.cols) if (m[c, r]) cells.add(c to r)
        cells.sortByDescending { (c, r) -> abs(c - cx) + abs(r - cy) }
        var k = 0
        while (m.count() > max(0, n) && k < cells.size) {
            val (c, r) = cells[k++]
            m[c, r] = false
            // simetri: karsiligini da sil
            m[m.cols - 1 - c, r] = false
        }
    }

    /** En ust kattan, sirtinda tas olmayan birini sil. */
    private fun removeTop(slots: MutableList<Slot>) {
        val zMax = slots.maxOf { it.z }
        val candidates = slots.filter { it.z == zMax }
        // Simetriyi bozmamak icin merkeze en uzak olani sil.
        val cx = slots.sumOf { it.x }.toFloat() / slots.size
        val victim = candidates.maxBy { abs(it.x - cx) * 10 + it.y }
        slots.remove(victim)
    }
}

object MahjongLevels {
    const val CHAPTER_SIZE = 20
    const val THEME_COUNT = 6

    fun theme(level: Int) = ((level - 1) / CHAPTER_SIZE) % THEME_COUNT
    fun chapter(level: Int) = (level - 1) / CHAPTER_SIZE

    /** Zorluk egrisi: tas sayisi, kat sayisi. Arada rahatlatici kolay seviyeler. */
    fun params(level: Int): Pair<Int, Int> {
        val l = level.coerceAtLeast(1)
        val inChapter = (l - 1) % CHAPTER_SIZE
        val ramp = min(1f, (l - 1) / 160f)                  // 0 -> 1, ilk 160 seviyede
        var tiles = (24 + ramp * 96).toInt() + (inChapter % 5) * 4  // 24 .. ~136
        var layers = 1 + (ramp * 4).toInt() + (if (inChapter % 4 == 3) 1 else 0)
        if (inChapter % 7 == 6) { tiles = (tiles * 0.65f).toInt(); layers = max(1, layers - 1) } // kolay mola
        if (l <= 3) { tiles = 16 + l * 4; layers = 1 }
        return tiles.coerceIn(16, 144) to layers.coerceIn(1, 5)
    }

    fun faceTypes(theme: Int, tiles: Int, level: Int): Int {
        val setSize = if (theme == 0) 36 else 18
        val pairs = tiles / 2
        // Her turden genelde 4 tas (2 cift); erken seviyelerde daha az tur.
        val wanted = (pairs / 2.0).toInt().coerceAtLeast(4) + min(6, level / 40)
        return wanted.coerceIn(4, setSize)
    }
}

/**
 * Bir seviyenin oyun durumu (Vita tarzi tepsi): acik bir tasa dokununca tas tahtadan
 * [TRAY_SIZE] kutucuklu tepsiye gider; tepside ayni yuzden iki tas olunca ikisi de kaybolur.
 * Tepsi eslesmeyen taslarla dolarsa oyuncu takilir (geri al ile kurtulabilir).
 */
class MahjongGame private constructor(
    val info: LevelInfo,
    tiles: List<MahjongTile>,
    removedIds: Set<Int>,
    trayIds: List<Int>,
) {
    val tiles: List<MahjongTile> = tiles
    private val slots: List<Slot> = tiles.map { Slot(it.x, it.y, it.z) }
    /** Eslesip tamamen giden taslar. */
    private val removed = BooleanArray(tiles.size).also { r -> removedIds.forEach { r[it] = true } }
    /** Tepsideki taslar (soldan saga). */
    private val trayList = ArrayList(trayIds)
    private val history = ArrayList<Move>()

    private sealed interface Move {
        data class ToTray(val id: Int) : Move
        data class Paired(val id: Int, val partner: Int, val partnerIndex: Int) : Move
    }

    var hintsUsed = 0
        private set
    var shufflesUsed = 0
        private set

    val tray: List<Int> get() = trayList
    /** Tahtada veya tepside kalan tas sayisi. */
    val remaining: Int get() = removed.count { !it }
    val boardCount: Int get() = remaining - trayList.size
    val isCleared: Boolean get() = remaining == 0
    val canUndo: Boolean get() = history.isNotEmpty()
    val isTrayFull: Boolean get() = trayList.size >= TRAY_SIZE

    fun isRemoved(id: Int) = removed[id]
    fun inTray(id: Int) = trayList.contains(id)
    fun onBoard(id: Int) = !removed[id] && !trayList.contains(id)

    fun isFree(id: Int): Boolean {
        if (!onBoard(id)) return false
        val s = slots[id]
        var left = false
        var right = false
        for (i in slots.indices) {
            if (i == id || !onBoard(i)) continue
            val o = slots[i]
            if (MahjongRules.covers(o, s)) return false
            if (MahjongRules.leftOf(s, o)) left = true
            if (MahjongRules.rightOf(s, o)) right = true
            if (left && right) return false
        }
        return true
    }

    fun freeIds(): List<Int> = tiles.indices.filter { isFree(it) }

    sealed interface TapResult {
        data object Blocked : TapResult
        data object TrayFull : TapResult
        data class ToTray(val id: Int, val slot: Int) : TapResult
        /** [id] tepsiye geldi ve [partner] ile eslesti; ikisi de gitti. */
        data class Paired(val id: Int, val partner: Int, val partnerIndex: Int) : TapResult
    }

    fun tap(id: Int): TapResult {
        if (!isFree(id)) return TapResult.Blocked
        if (isTrayFull) return TapResult.TrayFull
        val pi = trayList.indexOfFirst { tiles[it].face == tiles[id].face }
        return if (pi >= 0) {
            val partner = trayList.removeAt(pi)
            removed[id] = true; removed[partner] = true
            history.add(Move.Paired(id, partner, pi))
            TapResult.Paired(id, partner, pi)
        } else {
            trayList.add(id)
            history.add(Move.ToTray(id))
            TapResult.ToTray(id, trayList.size - 1)
        }
    }

    /** Son hamleyi geri alir; geri donen taslarin id'leri. */
    fun undo(): List<Int>? {
        return when (val m = history.removeLastOrNull() ?: return null) {
            is Move.ToTray -> { trayList.remove(m.id); listOf(m.id) }
            is Move.Paired -> {
                removed[m.id] = false; removed[m.partner] = false
                trayList.add(m.partnerIndex.coerceAtMost(trayList.size), m.partner)
                listOf(m.id, m.partner)
            }
        }
    }

    /**
     * Ipucu: tepsideki bir tasin esi acikta ise o tas; degilse acik bir cift; o da yoksa
     * esi tahtada olan acik bir tas (tepside yer varsa). Bos liste: oneri yok (karistir).
     */
    fun findHint(): List<Int> {
        val free = freeIds()
        val trayFaces = trayList.map { tiles[it].face }.toSet()
        free.firstOrNull { tiles[it].face in trayFaces }?.let { return listOf(it) }
        if (trayList.size <= TRAY_SIZE - 2) {
            for (i in free.indices) for (j in i + 1 until free.size)
                if (tiles[free[i]].face == tiles[free[j]].face) return listOf(free[i], free[j])
        }
        if (trayList.size <= TRAY_SIZE - 2) free.firstOrNull()?.let { return listOf(it) }
        return emptyList()
    }

    fun useHint(): List<Int> = findHint().also { if (it.isNotEmpty()) hintsUsed++ }

    /**
     * Tahtadaki taslarin yuzlerini cozulebilir olacak sekilde yeniden dagitir:
     * tahtayi tek tek bosaltan bir sira bulunur; siranin ilk taslari tepsidekilerin esleri olur,
     * kalanlar ardisik ciftler halinde ayni yuzu alir. Gecmis sifirlanir.
     */
    fun shuffle(rnd: SeededRandom): MahjongGame {
        val board = tiles.indices.filter { onBoard(it) }
        if (board.isEmpty()) return this
        val trayFaces = trayList.map { tiles[it].face }
        val counts = HashMap<Int, Int>()
        board.forEach { counts[tiles[it].face] = (counts[tiles[it].face] ?: 0) + 1 }
        trayFaces.forEach { counts[it] = (counts[it] ?: 0) - 1 }
        if (counts.values.any { it < 0 || it % 2 != 0 }) return this
        // Tek tek bosaltma sirasi (her zaman bulunur: en ust-sol tas hep aciktir).
        val alive = BooleanArray(tiles.size) { onBoard(it) }
        val order = ArrayList<Int>()
        while (order.size < board.size) {
            val free = board.filter { alive[it] && isFreeAmong(it, alive) }
            val pick = free[rnd.nextInt(free.size)]
            alive[pick] = false; order.add(pick)
        }
        val pairFaces = ArrayList<Int>()
        counts.forEach { (f, n) -> repeat(n / 2) { pairFaces.add(f) } }
        rnd.shuffle(pairFaces)
        val partners = trayFaces.toMutableList().also { rnd.shuffle(it) }
        val newFace = HashMap<Int, Int>()
        var k = 0
        for (f in partners) newFace[order[k++]] = f
        for (f in pairFaces) { newFace[order[k++]] = f; newFace[order[k++]] = f }
        val newTiles = tiles.map { tl -> newFace[tl.id]?.let { tl.copy(face = it) } ?: tl }
        return MahjongGame(info, newTiles, tiles.indices.filter { removed[it] }.toSet(), trayList.toList()).also {
            it.hintsUsed = hintsUsed
            it.shufflesUsed = shufflesUsed + 1
        }
    }

    private fun isFreeAmong(id: Int, alive: BooleanArray): Boolean {
        val s = slots[id]
        var left = false
        var right = false
        for (i in slots.indices) {
            if (i == id || !alive[i]) continue
            val o = slots[i]
            if (MahjongRules.covers(o, s)) return false
            if (MahjongRules.leftOf(s, o)) left = true
            if (MahjongRules.rightOf(s, o)) right = true
            if (left && right) return false
        }
        return true
    }

    fun snapshot(): MahjongSave = MahjongSave(
        level = info.level,
        tiles = tiles,
        removed = tiles.indices.filter { removed[it] },
        tray = trayList.toList(),
        hintsUsed = hintsUsed,
        shufflesUsed = shufflesUsed,
    )

    companion object {
        const val TRAY_SIZE = 4

        fun forLevel(level: Int): MahjongGame = MahjongGame(MahjongDealer.levelInfo(level), MahjongDealer.deal(level), emptySet(), emptyList())

        fun restore(save: MahjongSave): MahjongGame? {
            if (save.tiles.isEmpty()) return null
            val info = MahjongDealer.levelInfo(save.level)
            return MahjongGame(info, save.tiles, save.removed.toSet(), save.tray).also {
                it.hintsUsed = save.hintsUsed
                it.shufflesUsed = save.shufflesUsed
            }
        }
    }
}

@Serializable
data class MahjongSave(
    val level: Int = 1,
    val tiles: List<MahjongTile> = emptyList(),
    val removed: List<Int> = emptyList(),
    val tray: List<Int> = emptyList(),
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
    val elapsedMs: Long = 0,
)

/** Seviyeyi kurar: dizilim + cozulebilir yuz dagitimi. */
object MahjongDealer {
    private fun seed(level: Int) = level.toLong() * 0x9E3779B97F4A7C15uL.toLong() + 12345L

    class Generated(val info: LevelInfo, val tiles: List<MahjongTile>, val solution: IntArray)

    private val cache = HashMap<Int, Generated>()

    fun levelInfo(level: Int): LevelInfo = generate(level).info
    fun deal(level: Int): List<MahjongTile> = generate(level).tiles

    /** Seviye + bilinen bir cozum sirasi (testler icin). */
    @Synchronized
    fun generate(level: Int): Generated = cache.getOrPut(level) {
        if (cache.size > 8) cache.clear()
        val rnd = SeededRandom(seed(level))
        val (target, layers) = MahjongLevels.params(level)
        val theme = MahjongLevels.theme(level)
        val (family, slots) = MahjongLayouts.build(level, target, layers, rnd)
        val sorted = slots.sortedWith(compareBy({ it.z }, { it.y }, { it.x }))
        val types = MahjongLevels.faceTypes(theme, sorted.size, level)
        val setSize = if (theme == 0) 36 else 18
        // Bu seviyede kullanilacak yuzler (setten rastgele secim).
        val pool = (0 until setSize).toMutableList().also { rnd.shuffle(it) }.take(types)
        var order: IntArray? = null
        while (order == null) order = removalOrder(sorted, rnd)
        val pairs = sorted.size / 2
        val pairFaces = MutableList(pairs) { pool[it % types] }
        rnd.shuffle(pairFaces)
        val face = IntArray(sorted.size)
        for (k in 0 until pairs) {
            face[order[2 * k]] = pairFaces[k]
            face[order[2 * k + 1]] = pairFaces[k]
        }
        val tiles = sorted.mapIndexed { i, s -> MahjongTile(i, s.x, s.y, s.z, face[i]) }
        Generated(LevelInfo(level, MahjongLevels.chapter(level), theme, family, tiles.size, types), tiles, order)
    }

    /**
     * Tersten cozum: acik taslardan rastgele ciftler cikarilarak tum tahta bosaltilir.
     * Donen dizi cikarilma sirasidir (2k, 2k+1 ayni cift). Bu sira gecerli bir cozumdur,
     * dolayisiyla ayni ciftlere ayni yuz verilirse tahta mutlaka cozulebilir.
     */
    fun removalOrder(slots: List<Slot>, rnd: SeededRandom): IntArray? {
        if (slots.size % 2 != 0) return null
        repeat(60) {
            val alive = BooleanArray(slots.size) { true }
            val out = IntArray(slots.size)
            var n = 0
            var ok = true
            while (n < slots.size) {
                val free = ArrayList<Int>()
                for (i in slots.indices) if (alive[i] && isFreeAmong(i, slots, alive)) free.add(i)
                if (free.size < 2) { ok = false; break }
                // Iki tas da (birbiri dahil her sey yerindeyken) acik olmali.
                val a = free[rnd.nextInt(free.size)]
                var b = free[rnd.nextInt(free.size - 1)]
                if (b == a) b = free[free.size - 1]
                alive[a] = false; alive[b] = false
                out[n++] = a; out[n++] = b
            }
            if (ok) return out
        }
        return null
    }

    private fun isFreeAmong(id: Int, slots: List<Slot>, alive: BooleanArray): Boolean {
        val s = slots[id]
        var left = false
        var right = false
        for (i in slots.indices) {
            if (i == id || !alive[i]) continue
            val o = slots[i]
            if (MahjongRules.covers(o, s)) return false
            if (MahjongRules.leftOf(s, o)) left = true
            if (MahjongRules.rightOf(s, o)) right = true
            if (left && right) return false
        }
        return true
    }
}
