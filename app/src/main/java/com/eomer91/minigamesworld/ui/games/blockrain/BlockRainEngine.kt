package com.eomer91.minigamesworld.ui.games.blockrain

import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

/*
 * Blok Yagmuru: dusen bloklar (Tetris tarzi). 10x20 tahta, 7 parca, 7'li torba, tutma (hold),
 * hayalet parca, duvar tekmesi, kilitlenme gecikmesi. Android'den bagimsiz; unit test ile test edilir.
 */

/** Parca tipleri; [cells] 4 donus durumu icin (satir, sutun) listesi. */
enum class Tetromino(val color: Long, val cells: List<List<Pair<Int, Int>>>) {
    I(0xFF22D3EE, rotations(listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3), 4)),
    O(0xFFFACC15, List(4) { listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2) }),
    T(0xFFA855F7, rotations(listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2), 3)),
    S(0xFF22C55E, rotations(listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1), 3)),
    Z(0xFFEF4444, rotations(listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2), 3)),
    J(0xFF3B82F6, rotations(listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2), 3)),
    L(0xFFF97316, rotations(listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2), 3)),
}

/** Saat yonunde donusler: (r, c) -> (c, n-1-r) */
private fun rotations(base: List<Pair<Int, Int>>, n: Int): List<List<Pair<Int, Int>>> {
    val out = ArrayList<List<Pair<Int, Int>>>()
    var cur = base
    repeat(4) {
        out += cur
        cur = cur.map { (r, c) -> c to (n - 1 - r) }
    }
    return out
}

class BlockRainEngine(private val random: Random = Random.Default) {

    /** 0 = bos, aksi halde Tetromino.ordinal + 1 */
    val board: Array<IntArray> = Array(ROWS) { IntArray(COLS) }

    var piece: Tetromino = Tetromino.T
        private set
    var rot = 0
        private set
    var row = 0
        private set
    var col = 0
        private set

    var hold: Tetromino? = null
        private set
    private var holdUsed = false

    private val bag = ArrayDeque<Tetromino>()
    /** Siradaki parcalar (ilk 3'u gosterilir). */
    val next: List<Tetromino> get() { refill(); return bag.take(3) }

    var score = 0
        private set
    var lines = 0
        private set
    var level = 1
        private set
    var gameOver = false
        private set

    private var fallTimer = 0f
    private var lockTimer = 0f
    private var lockResets = 0
    private var softDropping = false

    // Olay sayaclari (ses ve animasyon icin)
    var lockCount = 0
        private set
    var lastCleared = 0
        private set
    var clearEvents = 0
        private set
    /** Son temizlenen satirlar (kisa parlama animasyonu icin). */
    var flashRows: List<Int> = emptyList()
        private set
    var flashTimer = 0f
        private set

    init {
        spawn()
    }

    // ================================================================ Parca

    fun cells(t: Tetromino = piece, r: Int = rot, top: Int = row, left: Int = col): List<Pair<Int, Int>> =
        t.cells[r].map { (dr, dc) -> (top + dr) to (left + dc) }

    private fun fits(t: Tetromino, r: Int, top: Int, left: Int): Boolean =
        t.cells[r].all { (dr, dc) ->
            val rr = top + dr
            val cc = left + dc
            cc in 0 until COLS && rr < ROWS && (rr < 0 || board[rr][cc] == 0)
        }

    private fun refill() {
        while (bag.size < 7) bag.addAll(Tetromino.entries.shuffled(random))
    }

    private fun spawn(t: Tetromino? = null) {
        refill()
        piece = t ?: bag.removeFirst()
        rot = 0
        row = if (piece == Tetromino.I) -1 else 0
        col = 3
        fallTimer = 0f
        lockTimer = 0f
        lockResets = 0
        if (!fits(piece, rot, row, col)) gameOver = true
    }

    /** Hayalet parcanin satiri (birakilinca duseceği yer). */
    fun ghostRow(): Int {
        var r = row
        while (fits(piece, rot, r + 1, col)) r++
        return r
    }

    private val onGround: Boolean get() = !fits(piece, rot, row + 1, col)

    private fun touched() {
        if (onGround && lockResets < MAX_LOCK_RESETS) {
            lockTimer = 0f
            lockResets++
        }
    }

    // ================================================================ Girdi

    fun move(dx: Int): Boolean {
        if (gameOver) return false
        if (!fits(piece, rot, row, col + dx)) return false
        col += dx
        touched()
        return true
    }

    /** [dir] 1 = saat yonu, -1 = ters. Duvar/zemin tekmesi dener. */
    fun rotate(dir: Int = 1): Boolean {
        if (gameOver || piece == Tetromino.O) return false
        val nr = (rot + dir + 4) % 4
        val kicks = if (piece == Tetromino.I) I_KICKS else KICKS
        for ((dc, dr) in kicks) {
            if (fits(piece, nr, row + dr, col + dc)) {
                rot = nr
                row += dr
                col += dc
                touched()
                return true
            }
        }
        return false
    }

    fun setSoftDrop(on: Boolean) {
        softDropping = on
    }

    /** Bir satir asagi (yumusak dusus); +1 puan. */
    fun softDrop(): Boolean {
        if (gameOver) return false
        if (!fits(piece, rot, row + 1, col)) return false
        row++
        score += 1
        fallTimer = 0f
        return true
    }

    fun hardDrop() {
        if (gameOver) return
        val target = ghostRow()
        score += (target - row) * 2
        row = target
        lock()
    }

    fun holdPiece(): Boolean {
        if (gameOver || holdUsed) return false
        val current = piece
        val h = hold
        hold = current
        holdUsed = true
        spawn(h)
        return true
    }

    // ================================================================ Zaman

    /** Saniye cinsinden yercekimi araligi (seviye yukseldikce hizlanir). */
    fun gravityInterval(): Float = max(0.03f, 0.8f * (0.8f - (level - 1) * 0.007f).pow(level - 1))

    fun update(dt: Float) {
        if (flashTimer > 0f) flashTimer = max(0f, flashTimer - dt)
        if (gameOver) return
        val interval = if (softDropping) minOf(0.05f, gravityInterval()) else gravityInterval()
        if (onGround) {
            lockTimer += dt
            if (lockTimer >= LOCK_DELAY) lock()
            return
        }
        fallTimer += dt
        while (fallTimer >= interval && !onGround) {
            fallTimer -= interval
            row++
            if (softDropping) score += 1
        }
        if (onGround) fallTimer = 0f
    }

    private fun lock() {
        for ((r, c) in cells()) {
            if (r < 0) {
                gameOver = true
                continue
            }
            board[r][c] = piece.ordinal + 1
        }
        lockCount++
        clearLines()
        holdUsed = false
        if (!gameOver) spawn()
    }

    private fun clearLines() {
        val full = (0 until ROWS).filter { r -> board[r].all { it != 0 } }
        lastCleared = full.size
        if (full.isEmpty()) return
        clearEvents++
        flashRows = full
        flashTimer = 0.25f
        val remaining = (0 until ROWS).filter { it !in full }.map { board[it].copyOf() }
        for (r in 0 until ROWS) {
            val src = r - full.size
            board[r] = if (src >= 0) remaining[src] else IntArray(COLS)
        }
        score += LINE_SCORES[full.size] * level
        lines += full.size
        level = 1 + lines / 10
    }

    /** Odullu reklam: alttaki [n] satiri silip oyuna devam. */
    fun rescue(n: Int = 6) {
        for (r in ROWS - 1 downTo 0) {
            val src = r - n
            board[r] = if (src >= 0) board[src].copyOf() else IntArray(COLS)
        }
        gameOver = false
        holdUsed = false
        spawn()
        if (gameOver) {
            // Hala sigmiyorsa tahtayi tamamen bosalt
            for (r in 0 until ROWS) board[r] = IntArray(COLS)
            gameOver = false
            spawn()
        }
    }

    // ================================================================ Kayit

    fun snapshot() = BlockRainSave(
        board = board.map { it.toList() },
        piece = piece.ordinal,
        bag = bag.map { it.ordinal },
        hold = hold?.ordinal ?: -1,
        score = score,
        lines = lines,
    )

    fun restore(s: BlockRainSave): Boolean {
        if (s.board.size != ROWS || s.board.any { it.size != COLS }) return false
        for (r in 0 until ROWS) for (c in 0 until COLS) board[r][c] = s.board[r][c].coerceIn(0, 7)
        bag.clear()
        s.bag.forEach { i -> Tetromino.entries.getOrNull(i)?.let { bag.addLast(it) } }
        hold = Tetromino.entries.getOrNull(s.hold)
        score = s.score
        lines = s.lines
        level = 1 + lines / 10
        gameOver = false
        holdUsed = false
        spawn(Tetromino.entries.getOrNull(s.piece))
        return !gameOver
    }

    companion object {
        const val ROWS = 20
        const val COLS = 10
        const val LOCK_DELAY = 0.5f
        const val MAX_LOCK_RESETS = 15
        val LINE_SCORES = intArrayOf(0, 100, 300, 500, 800)
        /** (sutun, satir) denemeleri */
        private val KICKS = listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, -1 to -1, 1 to -1, -2 to 0, 2 to 0)
        private val I_KICKS = listOf(0 to 0, -1 to 0, 1 to 0, -2 to 0, 2 to 0, 0 to -1, 0 to -2, 0 to 1)
    }
}
