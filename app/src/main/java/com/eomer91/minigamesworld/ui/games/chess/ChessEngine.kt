package com.eomer91.minigamesworld.ui.games.chess

import kotlin.math.abs
import kotlin.random.Random

/*
 * Satranc kurallari ve yapay zeka. Android'den bagimsiz; unit test ile test edilir.
 * Tahta: IntArray(64), indeks = satir*8 + sutun, satir 0 = 8. yatay (siyahin tarafi).
 * Tas kodlari: 1 piyon, 2 at, 3 fil, 4 kale, 5 vezir, 6 sah; beyaz pozitif, siyah negatif.
 * Hamle bir Int'e paketlenir: from | to<<6 | promo<<12 | flag<<16.
 */

object Chess {
    const val PAWN = 1
    const val KNIGHT = 2
    const val BISHOP = 3
    const val ROOK = 4
    const val QUEEN = 5
    const val KING = 6
    const val WHITE = 1
    const val BLACK = -1

    const val FLAG_NONE = 0
    const val FLAG_EP = 1
    const val FLAG_CASTLE = 2
    const val FLAG_DOUBLE = 3

    fun move(from: Int, to: Int, promo: Int = 0, flag: Int = 0) = from or (to shl 6) or (promo shl 12) or (flag shl 16)
    fun from(m: Int) = m and 63
    fun to(m: Int) = (m shr 6) and 63
    fun promo(m: Int) = (m shr 12) and 7
    fun flag(m: Int) = (m shr 16) and 7

    fun sq(name: String): Int = (8 - (name[1] - '0')) * 8 + (name[0] - 'a')
    fun name(i: Int): String = "${'a' + i % 8}${8 - i / 8}"
}

class Undo(val captured: Int, val castle: Int, val ep: Int, val half: Int)

class ChessPosition(
    val b: IntArray = initial(),
    var side: Int = Chess.WHITE,
    /** 1 beyaz kisa, 2 beyaz uzun, 4 siyah kisa, 8 siyah uzun */
    var castle: Int = 15,
    var ep: Int = -1,
    var half: Int = 0,
) {
    fun copy() = ChessPosition(b.copyOf(), side, castle, ep, half)

    /** Tekrar kontrolu icin pozisyon anahtari. */
    fun key(): String = b.joinToString(",") + "|$side|$castle|$ep"

    fun kingSquare(s: Int): Int {
        val k = Chess.KING * s
        for (i in 0 until 64) if (b[i] == k) return i
        return -1
    }

    // ================================================================ Saldiri

    fun attacked(sq: Int, by: Int): Boolean {
        val r = sq / 8
        val c = sq % 8
        // Piyon: beyaz piyon asagidan (r+1) saldirir
        val pr = r + by
        if (pr in 0..7) {
            if (c > 0 && b[pr * 8 + c - 1] == Chess.PAWN * by) return true
            if (c < 7 && b[pr * 8 + c + 1] == Chess.PAWN * by) return true
        }
        for ((dr, dc) in KNIGHT_D) {
            val rr = r + dr
            val cc = c + dc
            if (rr in 0..7 && cc in 0..7 && b[rr * 8 + cc] == Chess.KNIGHT * by) return true
        }
        for ((dr, dc) in KING_D) {
            val rr = r + dr
            val cc = c + dc
            if (rr in 0..7 && cc in 0..7 && b[rr * 8 + cc] == Chess.KING * by) return true
        }
        for ((dr, dc) in DIAG) {
            var rr = r + dr
            var cc = c + dc
            while (rr in 0..7 && cc in 0..7) {
                val v = b[rr * 8 + cc]
                if (v != 0) {
                    if (v == Chess.BISHOP * by || v == Chess.QUEEN * by) return true
                    break
                }
                rr += dr
                cc += dc
            }
        }
        for ((dr, dc) in ORTHO) {
            var rr = r + dr
            var cc = c + dc
            while (rr in 0..7 && cc in 0..7) {
                val v = b[rr * 8 + cc]
                if (v != 0) {
                    if (v == Chess.ROOK * by || v == Chess.QUEEN * by) return true
                    break
                }
                rr += dr
                cc += dc
            }
        }
        return false
    }

    fun inCheck(s: Int = side): Boolean {
        val k = kingSquare(s)
        return k >= 0 && attacked(k, -s)
    }

    // ================================================================ Hamle uretimi

    fun pseudoMoves(out: MutableList<Int>, capturesOnly: Boolean = false) {
        val s = side
        for (i in 0 until 64) {
            val v = b[i]
            if (v * s <= 0) continue
            val r = i / 8
            val c = i % 8
            when (abs(v)) {
                Chess.PAWN -> {
                    val dir = -s // beyaz yukari (satir azalir)
                    val r1 = r + dir
                    if (r1 !in 0..7) continue
                    val lastRow = if (s == Chess.WHITE) 0 else 7
                    val startRow = if (s == Chess.WHITE) 6 else 1
                    val one = r1 * 8 + c
                    if (b[one] == 0 && (!capturesOnly || r1 == lastRow)) {
                        addPawn(out, i, one, r1 == lastRow, Chess.FLAG_NONE)
                        val two = (r + 2 * dir) * 8 + c
                        if (r == startRow && b[two] == 0 && !capturesOnly) out += Chess.move(i, two, 0, Chess.FLAG_DOUBLE)
                    }
                    for (dc in intArrayOf(-1, 1)) {
                        val cc = c + dc
                        if (cc !in 0..7) continue
                        val t = r1 * 8 + cc
                        if (b[t] * s < 0) addPawn(out, i, t, r1 == lastRow, Chess.FLAG_NONE)
                        else if (t == ep) out += Chess.move(i, t, 0, Chess.FLAG_EP)
                    }
                }
                Chess.KNIGHT -> steps(out, i, r, c, KNIGHT_D, capturesOnly)
                Chess.KING -> {
                    steps(out, i, r, c, KING_D, capturesOnly)
                    if (!capturesOnly) castles(out, i)
                }
                Chess.BISHOP -> slides(out, i, r, c, DIAG, capturesOnly)
                Chess.ROOK -> slides(out, i, r, c, ORTHO, capturesOnly)
                Chess.QUEEN -> {
                    slides(out, i, r, c, DIAG, capturesOnly)
                    slides(out, i, r, c, ORTHO, capturesOnly)
                }
            }
        }
    }

    private fun addPawn(out: MutableList<Int>, from: Int, to: Int, promote: Boolean, flag: Int) {
        if (promote) {
            for (p in intArrayOf(Chess.QUEEN, Chess.KNIGHT, Chess.ROOK, Chess.BISHOP)) out += Chess.move(from, to, p, flag)
        } else {
            out += Chess.move(from, to, 0, flag)
        }
    }

    private fun steps(out: MutableList<Int>, i: Int, r: Int, c: Int, dirs: Array<IntArray>, capturesOnly: Boolean) {
        for ((dr, dc) in dirs) {
            val rr = r + dr
            val cc = c + dc
            if (rr !in 0..7 || cc !in 0..7) continue
            val t = rr * 8 + cc
            val v = b[t]
            if (v * side > 0) continue
            if (capturesOnly && v == 0) continue
            out += Chess.move(i, t)
        }
    }

    private fun slides(out: MutableList<Int>, i: Int, r: Int, c: Int, dirs: Array<IntArray>, capturesOnly: Boolean) {
        for ((dr, dc) in dirs) {
            var rr = r + dr
            var cc = c + dc
            while (rr in 0..7 && cc in 0..7) {
                val t = rr * 8 + cc
                val v = b[t]
                if (v * side > 0) break
                if (v != 0) {
                    out += Chess.move(i, t)
                    break
                }
                if (!capturesOnly) out += Chess.move(i, t)
                rr += dr
                cc += dc
            }
        }
    }

    private fun castles(out: MutableList<Int>, k: Int) {
        val s = side
        val home = if (s == Chess.WHITE) 60 else 4
        if (k != home || attacked(home, -s)) return
        val shortBit = if (s == Chess.WHITE) 1 else 4
        val longBit = if (s == Chess.WHITE) 2 else 8
        if (castle and shortBit != 0 && b[home + 1] == 0 && b[home + 2] == 0 && b[home + 3] == Chess.ROOK * s &&
            !attacked(home + 1, -s) && !attacked(home + 2, -s)
        ) out += Chess.move(home, home + 2, 0, Chess.FLAG_CASTLE)
        if (castle and longBit != 0 && b[home - 1] == 0 && b[home - 2] == 0 && b[home - 3] == 0 && b[home - 4] == Chess.ROOK * s &&
            !attacked(home - 1, -s) && !attacked(home - 2, -s)
        ) out += Chess.move(home, home - 2, 0, Chess.FLAG_CASTLE)
    }

    fun legalMoves(): List<Int> {
        val pseudo = ArrayList<Int>(48)
        pseudoMoves(pseudo)
        val s = side
        return pseudo.filter { m ->
            val u = make(m)
            val ok = !inCheck(s)
            unmake(m, u)
            ok
        }
    }

    // ================================================================ Yap / geri al

    fun make(m: Int): Undo {
        val from = Chess.from(m)
        val to = Chess.to(m)
        val flag = Chess.flag(m)
        val piece = b[from]
        var captured = b[to]
        val undo0 = Undo(captured, castle, ep, half)
        b[from] = 0
        if (flag == Chess.FLAG_EP) {
            val cap = to + 8 * side // beyaz icin alinan piyon bir alt satirda
            captured = b[cap]
            b[cap] = 0
        }
        val promo = Chess.promo(m)
        b[to] = if (promo != 0) promo * side else piece
        if (flag == Chess.FLAG_CASTLE) {
            if (to > from) {
                b[to - 1] = b[to + 1]
                b[to + 1] = 0
            } else {
                b[to + 1] = b[to - 2]
                b[to - 2] = 0
            }
        }
        // Rok haklari
        if (abs(piece) == Chess.KING) castle = castle and (if (side == Chess.WHITE) 3.inv() else 12.inv())
        for (sq in intArrayOf(from, to)) {
            when (sq) {
                63 -> castle = castle and 1.inv()
                56 -> castle = castle and 2.inv()
                7 -> castle = castle and 4.inv()
                0 -> castle = castle and 8.inv()
            }
        }
        ep = if (flag == Chess.FLAG_DOUBLE) (from + to) / 2 else -1
        half = if (abs(piece) == Chess.PAWN || captured != 0) 0 else half + 1
        side = -side
        return if (flag == Chess.FLAG_EP) Undo(captured, undo0.castle, undo0.ep, undo0.half) else undo0
    }

    fun unmake(m: Int, u: Undo) {
        side = -side
        val from = Chess.from(m)
        val to = Chess.to(m)
        val flag = Chess.flag(m)
        val piece = if (Chess.promo(m) != 0) Chess.PAWN * side else b[to]
        b[from] = piece
        if (flag == Chess.FLAG_EP) {
            b[to] = 0
            b[to + 8 * side] = u.captured
        } else {
            b[to] = u.captured
        }
        if (flag == Chess.FLAG_CASTLE) {
            if (to > from) {
                b[to + 1] = b[to - 1]
                b[to - 1] = 0
            } else {
                b[to - 2] = b[to + 1]
                b[to + 1] = 0
            }
        }
        castle = u.castle
        ep = u.ep
        half = u.half
    }

    /** Yetersiz materyal: sah-sah, sah+fil/at - sah. */
    fun insufficientMaterial(): Boolean {
        var minor = 0
        for (v in b) {
            when (abs(v)) {
                0, Chess.KING -> Unit
                Chess.KNIGHT, Chess.BISHOP -> minor++
                else -> return false
            }
        }
        return minor <= 1
    }

    companion object {
        /** FEN'den pozisyon (yalnizca ilk 4 alan). */
        fun fromFen(fen: String): ChessPosition {
            val parts = fen.trim().split(" ")
            val b = IntArray(64)
            var i = 0
            for (ch in parts[0]) {
                when {
                    ch == '/' -> Unit
                    ch.isDigit() -> i += ch - '0'
                    else -> {
                        val t = "pnbrqk".indexOf(ch.lowercaseChar()) + 1
                        b[i++] = if (ch.isUpperCase()) t else -t
                    }
                }
            }
            val side = if (parts.getOrElse(1) { "w" } == "w") Chess.WHITE else Chess.BLACK
            var castle = 0
            parts.getOrElse(2) { "-" }.forEach {
                when (it) {
                    'K' -> castle = castle or 1
                    'Q' -> castle = castle or 2
                    'k' -> castle = castle or 4
                    'q' -> castle = castle or 8
                }
            }
            val epStr = parts.getOrElse(3) { "-" }
            val ep = if (epStr == "-") -1 else Chess.sq(epStr)
            return ChessPosition(b, side, castle, ep, 0)
        }

        fun initial(): IntArray {
            val back = intArrayOf(4, 2, 3, 5, 6, 3, 2, 4)
            return IntArray(64) { i ->
                val r = i / 8
                val c = i % 8
                when (r) {
                    0 -> -back[c]
                    1 -> -1
                    6 -> 1
                    7 -> back[c]
                    else -> 0
                }
            }
        }

        val KNIGHT_D = arrayOf(intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2), intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1))
        val KING_D = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1))
        val DIAG = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        val ORTHO = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
    }
}

// ==================================================================== Yapay zeka

object ChessAi {
    private val VALUE = intArrayOf(0, 100, 320, 330, 500, 900, 0)

    // Beyaz icin tablolar (satir 0 = 8. yatay). Siyah icin dikey aynalanir.
    private val PST_P = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0,
    )
    private val PST_N = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50,
    )
    private val PST_B = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20,
    )
    private val PST_R = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, 10, 10, 10, 10, 5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        0, 0, 0, 5, 5, 0, 0, 0,
    )
    private val PST_Q = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5,
        0, 0, 5, 5, 5, 5, 0, -5,
        -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20,
    )
    private val PST_K = intArrayOf(
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        20, 20, 0, 0, 0, 0, 20, 20,
        20, 30, 10, 0, 0, 10, 30, 20,
    )
    private val PST_K_END = intArrayOf(
        -50, -40, -30, -20, -20, -30, -40, -50,
        -30, -20, -10, 0, 0, -10, -20, -30,
        -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30,
        -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -30, 0, 0, 0, 0, -30, -30,
        -50, -30, -30, -30, -30, -30, -30, -50,
    )

    /** [p.side] bakis acisindan pozisyon degeri. */
    fun evaluate(p: ChessPosition): Int {
        var material = 0
        for (v in p.b) if (abs(v) in 2..5) material += VALUE[abs(v)]
        val endgame = material <= 2600
        var score = 0
        for (i in 0 until 64) {
            val v = p.b[i]
            if (v == 0) continue
            val t = abs(v)
            val idx = if (v > 0) i else (7 - i / 8) * 8 + i % 8
            val pst = when (t) {
                Chess.PAWN -> PST_P[idx]
                Chess.KNIGHT -> PST_N[idx]
                Chess.BISHOP -> PST_B[idx]
                Chess.ROOK -> PST_R[idx]
                Chess.QUEEN -> PST_Q[idx]
                else -> if (endgame) PST_K_END[idx] else PST_K[idx]
            }
            val value = VALUE[t] + pst
            score += if (v > 0) value else -value
        }
        return score * p.side
    }

    const val MATE = 100_000
    private const val INF = 1_000_000
    private var nodes = 0
    private var nodeLimit = Int.MAX_VALUE
    private var aborted = false

    /**
     * En iyi hamle. Derinlik [maxDepth]'e kadar artarak arar; [maxNodes] asilirsa son tamamlanan derinligin
     * sonucunu doner. [noise] > 0 ise hamle puanlarina rastgelelik eklenir (kolay seviye).
     */
    fun bestMove(pos: ChessPosition, maxDepth: Int, random: Random = Random.Default, noise: Int = 0, maxNodes: Int = 400_000): Int? {
        val p = pos.copy()
        val root = p.legalMoves().toMutableList()
        if (root.isEmpty()) return null
        if (root.size == 1) return root.first()
        root.shuffle(random)
        nodes = 0
        nodeLimit = maxNodes
        aborted = false
        var best = root.first()
        for (depth in 1..maxDepth) {
            orderMoves(p, root, best)
            var alpha = -INF
            var depthBest = best
            var depthBestScore = -INF
            for (m in root) {
                val u = p.make(m)
                var score = -search(p, depth - 1, -INF, -alpha, 1)
                p.unmake(m, u)
                if (aborted) break
                if (noise > 0) score += random.nextInt(noise + 1)
                if (score > depthBestScore) {
                    depthBestScore = score
                    depthBest = m
                }
                if (score > alpha) alpha = score
            }
            if (aborted) break
            best = depthBest
            if (depthBestScore >= MATE - 100) break
        }
        return best
    }

    private fun search(p: ChessPosition, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        if (++nodes > nodeLimit) {
            aborted = true
            return 0
        }
        if (p.half >= 100) return 0
        val inCheck = p.inCheck()
        val d = if (inCheck) depth + 1 else depth
        if (d <= 0) return quiesce(p, alphaIn, beta, 0)
        val moves = ArrayList<Int>(48)
        p.pseudoMoves(moves)
        orderMoves(p, moves, 0)
        var alpha = alphaIn
        var legal = 0
        val s = p.side
        for (m in moves) {
            val u = p.make(m)
            if (p.inCheck(s)) {
                p.unmake(m, u)
                continue
            }
            legal++
            val score = -search(p, d - 1, -beta, -alpha, ply + 1)
            p.unmake(m, u)
            if (aborted) return 0
            if (score >= beta) return score
            if (score > alpha) alpha = score
        }
        if (legal == 0) return if (inCheck) -MATE + ply else 0
        return alpha
    }

    private fun quiesce(p: ChessPosition, alphaIn: Int, beta: Int, qply: Int): Int {
        if (++nodes > nodeLimit) {
            aborted = true
            return 0
        }
        val stand = evaluate(p)
        if (stand >= beta) return stand
        var alpha = maxOf(alphaIn, stand)
        if (qply > 6) return alpha
        val moves = ArrayList<Int>(16)
        p.pseudoMoves(moves, capturesOnly = true)
        orderMoves(p, moves, 0)
        val s = p.side
        for (m in moves) {
            val u = p.make(m)
            if (p.inCheck(s)) {
                p.unmake(m, u)
                continue
            }
            val score = -quiesce(p, -beta, -alpha, qply + 1)
            p.unmake(m, u)
            if (aborted) return 0
            if (score >= beta) return score
            if (score > alpha) alpha = score
        }
        return alpha
    }

    /** MVV-LVA: degerli tasi ucuz tasla almayi once dene; [first] en basa. */
    private fun orderMoves(p: ChessPosition, moves: MutableList<Int>, first: Int) {
        moves.sortByDescending { m ->
            if (m == first) return@sortByDescending 1_000_000
            val victim = abs(p.b[Chess.to(m)])
            val attacker = abs(p.b[Chess.from(m)])
            var s = 0
            if (victim != 0) s += 10_000 + VALUE[victim] * 10 - VALUE[attacker]
            if (Chess.flag(m) == Chess.FLAG_EP) s += 10_000
            if (Chess.promo(m) == Chess.QUEEN) s += 9_000
            s
        }
    }
}
