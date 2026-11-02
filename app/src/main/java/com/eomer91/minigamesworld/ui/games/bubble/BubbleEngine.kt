package com.eomer91.minigamesworld.ui.games.bubble

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Basit 2D nokta (birim: balon capi = 1). Android'den bagimsiz. */
data class Vec(val x: Float, val y: Float) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec(x * k, y * k)
    fun dist(o: Vec) = hypot(x - o.x, y - o.y)
}

data class Bubble(val id: Long, val color: Int)

/** Satir, sutun. */
data class Slot(val row: Int, val col: Int)

/**
 * Bal petegi tahta. Satirlar sirayla yarim balon kayiktir; hangi satirin kayik oldugu
 * [shift] ile belirlenir (ustten yeni sira inince tum satirlar kayar, parite korunur).
 */
data class BubbleBoard(val cells: Map<Slot, Bubble>, val shift: Int = 0) {
    val isEmpty: Boolean get() = cells.isEmpty()
    val lowestRow: Int get() = cells.keys.maxOfOrNull { it.row } ?: -1
    val colors: Set<Int> get() = cells.values.map { it.color }.toSet()
}

data class ShotResult(
    /** Ucusun kirik cizgisi (atis noktasi, sekme noktalari, varis). */
    val path: List<Vec>,
    val landed: Slot,
    val popped: Map<Slot, Bubble>,
    val dropped: Map<Slot, Bubble>,
    /** Patlama/dusme sonrasi tahta (yeni sira eklenmeden). */
    val board: BubbleBoard,
    val points: Int,
)

/** Bubble Shooter kurallari. Unit test ile test edilir. */
class BubbleEngine(
    private val random: Random = Random.Default,
    val cols: Int = 8,
    val colorCount: Int = 5,
) {
    private var nextId = 1L
    private fun bubble(color: Int) = Bubble(nextId++, color)

    /** Oyun alaninin genisligi ve yuksekligi (birim). */
    val width: Float get() = cols.toFloat()
    val height: Float get() = 0.5f + (DANGER_ROW + 1) * ROW_H + 1.6f

    /** Atici: alt ortada. */
    val shooter: Vec get() = Vec(width / 2f, height - 0.75f)

    fun isShifted(board: BubbleBoard, row: Int) = (row + board.shift) % 2 != 0
    fun colsIn(board: BubbleBoard, row: Int) = if (isShifted(board, row)) cols - 1 else cols
    fun isValid(board: BubbleBoard, s: Slot) = s.row >= 0 && s.col >= 0 && s.col < colsIn(board, s.row)

    fun center(board: BubbleBoard, s: Slot) =
        Vec(s.col + if (isShifted(board, s.row)) 1f else 0.5f, 0.5f + s.row * ROW_H)

    fun neighbors(board: BubbleBoard, s: Slot): List<Slot> {
        val (r, c) = s
        val d = if (isShifted(board, r)) 0 else -1
        return listOf(
            Slot(r, c - 1), Slot(r, c + 1),
            Slot(r - 1, c + d), Slot(r - 1, c + d + 1),
            Slot(r + 1, c + d), Slot(r + 1, c + d + 1),
        ).filter { isValid(board, it) }
    }

    fun newBoard(rows: Int): BubbleBoard {
        var board = BubbleBoard(emptyMap())
        val cells = HashMap<Slot, Bubble>()
        for (r in 0 until rows) {
            for (c in 0 until colsIn(board, r)) {
                // Komsuyla ayni renk olma ihtimali biraz yuksek: kucuk gruplar olusur, oyun keyifli olur.
                val left = cells[Slot(r, c - 1)]
                val color = if (left != null && random.nextFloat() < 0.35f) left.color else random.nextInt(colorCount)
                cells[Slot(r, c)] = bubble(color)
            }
            board = BubbleBoard(cells.toMap())
        }
        return BubbleBoard(cells)
    }

    /** Ustten yeni bir sira indirir. */
    fun addRow(board: BubbleBoard): BubbleBoard {
        val moved = board.cells.mapKeys { (s, _) -> Slot(s.row + 1, s.col) }.toMutableMap()
        val next = BubbleBoard(moved, shift = board.shift + 1)
        val palette = board.colors.ifEmpty { (0 until colorCount).toSet() }.toList()
        for (c in 0 until colsIn(next, 0)) moved[Slot(0, c)] = bubble(palette[random.nextInt(palette.size)])
        return next.copy(cells = moved)
    }

    /** Siradaki atis rengi: tahtada kalan renklerden. */
    fun nextColor(board: BubbleBoard): Int {
        val palette = board.colors.ifEmpty { (0 until colorCount).toSet() }.toList()
        return palette[random.nextInt(palette.size)]
    }

    /** Nisan acisini sinirlar (yataya cok yakin atislar engellenir). [angle]: radyan, 0 = saga, PI/2 = yukari. */
    fun clampAngle(angle: Float): Float = angle.coerceIn(MIN_ANGLE, (PI - MIN_ANGLE).toFloat())

    /** Nisan cizgisi icin yol (ilk carpismaya kadar). */
    fun aimPath(board: BubbleBoard, angle: Float): List<Vec> = trace(board, clampAngle(angle)).first

    fun shoot(board: BubbleBoard, angle: Float, color: Int): ShotResult {
        val (path, stop) = trace(board, clampAngle(angle))
        val landed = landingSlot(board, stop)
        val placed = board.copy(cells = board.cells + (landed to bubble(color)))

        val group = sameColorGroup(placed, landed)
        if (group.size < MIN_GROUP) {
            return ShotResult(path + center(placed, landed), landed, emptyMap(), emptyMap(), placed, 0)
        }

        val popped = group.associateWith { placed.cells.getValue(it) }
        val afterPop = placed.copy(cells = placed.cells - group)
        val attached = attachedToTop(afterPop)
        val dropped = afterPop.cells.filterKeys { it !in attached }
        val finalBoard = afterPop.copy(cells = afterPop.cells - dropped.keys)
        val points = popped.size * POINTS_POP + dropped.size * POINTS_DROP
        return ShotResult(path + center(placed, landed), landed, popped, dropped, finalBoard, points)
    }

    fun sameColorGroup(board: BubbleBoard, start: Slot): Set<Slot> {
        val color = board.cells[start]?.color ?: return emptySet()
        val seen = HashSet<Slot>()
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            if (!seen.add(s)) continue
            neighbors(board, s).filter { board.cells[it]?.color == color && it !in seen }.forEach(queue::addLast)
        }
        return seen
    }

    /** Tavana (0. satir) bagli balonlar. */
    fun attachedToTop(board: BubbleBoard): Set<Slot> {
        val seen = HashSet<Slot>()
        val queue = ArrayDeque(board.cells.keys.filter { it.row == 0 })
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            if (!seen.add(s)) continue
            neighbors(board, s).filter { it in board.cells && it !in seen }.forEach(queue::addLast)
        }
        return seen
    }

    fun isGameOver(board: BubbleBoard) = board.lowestRow >= DANGER_ROW

    /** Odullu reklamla devam: tehlike cizgisine yakin alt 3 sirayi siler, kopanlari dusurur. */
    fun clearBottomRows(board: BubbleBoard, rows: Int = 3): BubbleBoard {
        val limit = board.lowestRow - rows + 1
        val kept = board.copy(cells = board.cells.filterKeys { it.row < limit })
        val attached = attachedToTop(kept)
        return kept.copy(cells = kept.cells.filterKeys { it in attached })
    }

    /** Atis yolunu adim adim izler: yan duvarlardan seker, tavana ya da bir balona degince durur. */
    private fun trace(board: BubbleBoard, angle: Float): Pair<List<Vec>, Vec> {
        var pos = shooter
        var dir = Vec(cos(angle), -sin(angle))
        val path = mutableListOf(pos)
        val centers = board.cells.keys.map { center(board, it) }
        repeat(MAX_STEPS) {
            var next = pos + dir * STEP
            if (next.x < 0.5f || next.x > width - 0.5f) {
                next = Vec(next.x.coerceIn(0.5f, width - 0.5f), next.y)
                path += next
                dir = Vec(-dir.x, dir.y)
            }
            pos = next
            if (pos.y <= 0.5f || centers.any { it.dist(pos) < HIT_DISTANCE }) return path + pos to pos
        }
        return path + pos to pos
    }

    /** Durma noktasina en yakin, bos ve tutunabilecek (tavan ya da komsu balon) hucre. */
    private fun landingSlot(board: BubbleBoard, stop: Vec): Slot {
        val maxRow = board.lowestRow + 2
        val candidates = (0..maxRow).flatMap { r ->
            (0 until colsIn(board, r)).map { Slot(r, it) }
        }.filter { it !in board.cells && (it.row == 0 || neighbors(board, it).any { n -> n in board.cells }) }
        return candidates.minByOrNull { center(board, it).dist(stop) } ?: Slot(0, cols / 2)
    }

    companion object {
        val ROW_H: Float = (sqrt(3.0) / 2.0).toFloat()
        const val DANGER_ROW = 11
        const val START_ROWS = 5
        const val MISSES_PER_NEW_ROW = 5
        const val MIN_GROUP = 3
        const val POINTS_POP = 10
        const val POINTS_DROP = 20
        const val CLEAR_BONUS = 500
        private const val STEP = 0.05f
        private const val MAX_STEPS = 2000
        private const val HIT_DISTANCE = 0.88f
        private val MIN_ANGLE = (10.0 * PI / 180.0).toFloat()
    }
}
