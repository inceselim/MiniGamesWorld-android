package com.eomer91.minigamesworld.ui.games.pool

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * 8 top bilardo: fizik + kurallar + yapay zeka (Android'den bagimsiz).
 * Masa: x uzun kenar (0..L), y kisa kenar (0..W). Top 0 beyaz, 1-7 duz, 8 siyah, 9-15 cizgili.
 */

enum class PoolGroup { SOLID, STRIPE }

class Ball(val id: Int, var x: Float, var y: Float) {
    var vx = 0f
    var vy = 0f
    var pocketed = false
    val moving: Boolean get() = !pocketed && (vx != 0f || vy != 0f)

    fun copy() = Ball(id, x, y).also { it.vx = vx; it.vy = vy; it.pocketed = pocketed }
}

fun groupOf(id: Int): PoolGroup? = when (id) {
    in 1..7 -> PoolGroup.SOLID
    in 9..15 -> PoolGroup.STRIPE
    else -> null
}

/** Bir vurusun fizik sonucu. */
class ShotLog {
    var firstContact: Int = -1
    val pocketed = ArrayList<Int>()
    var cushionHits = 0
}

class PoolTable(val balls: List<Ball>) {
    var log = ShotLog()

    fun copy() = PoolTable(balls.map { it.copy() })

    val cue: Ball get() = balls[0]
    val moving: Boolean get() = balls.any { it.moving }

    /** [dt] saniye fizik; kucuk adimlarla. */
    fun step(dt: Float) {
        var t = dt
        while (t > 0f) {
            val h = minOf(SUB_DT, t)
            sub(h)
            t -= h
        }
    }

    private fun sub(h: Float) {
        for (b in balls) {
            if (b.pocketed) continue
            if (b.vx == 0f && b.vy == 0f) continue
            b.x += b.vx * h
            b.y += b.vy * h
            // Surtunme
            val v = hypot(b.vx, b.vy)
            val nv = v - FRICTION * h
            if (nv <= STOP_SPEED) {
                b.vx = 0f
                b.vy = 0f
            } else {
                b.vx *= nv / v
                b.vy *= nv / v
            }
        }
        // Top-top carpismasi
        for (i in balls.indices) {
            val a = balls[i]
            if (a.pocketed) continue
            for (j in i + 1 until balls.size) {
                val b = balls[j]
                if (b.pocketed) continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val d2 = dx * dx + dy * dy
                if (d2 >= (2 * R) * (2 * R) || d2 == 0f) continue
                val d = sqrt(d2)
                val nx = dx / d
                val ny = dy / d
                // Ust uste binmeyi ayir
                val overlap = 2 * R - d
                a.x -= nx * overlap / 2
                a.y -= ny * overlap / 2
                b.x += nx * overlap / 2
                b.y += ny * overlap / 2
                val vr = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny
                if (vr <= 0f) continue
                val jImp = vr * (1 + BALL_E) / 2
                a.vx -= jImp * nx
                a.vy -= jImp * ny
                b.vx += jImp * nx
                b.vy += jImp * ny
                if (log.firstContact < 0) {
                    if (a.id == 0) log.firstContact = b.id else if (b.id == 0) log.firstContact = a.id
                }
            }
        }
        // Cepler ve bantlar
        for (b in balls) {
            if (b.pocketed) continue
            if (inPocket(b.x, b.y)) {
                pocket(b)
                continue
            }
            if (b.x < R && !inCornerMouthY(b.y)) {
                b.x = R; b.vx = abs(b.vx) * CUSHION_E; log.cushionHits++
            }
            if (b.x > L - R && !inCornerMouthY(b.y)) {
                b.x = L - R; b.vx = -abs(b.vx) * CUSHION_E; log.cushionHits++
            }
            if (b.y < R && !inMouthX(b.x)) {
                b.y = R; b.vy = abs(b.vy) * CUSHION_E; log.cushionHits++
            }
            if (b.y > W - R && !inMouthX(b.x)) {
                b.y = W - R; b.vy = -abs(b.vy) * CUSHION_E; log.cushionHits++
            }
            // Masadan tasan top (cep agzinda) cebe duser
            if (b.x < -2 * R || b.x > L + 2 * R || b.y < -2 * R || b.y > W + 2 * R) pocket(b)
        }
    }

    private fun pocket(b: Ball) {
        b.pocketed = true
        b.vx = 0f
        b.vy = 0f
        log.pocketed += b.id
    }

    fun clearPath(x0: Float, y0: Float, x1: Float, y1: Float, ignore: Set<Int>, clearance: Float = 2 * R): Boolean {
        for (b in balls) {
            if (b.pocketed || b.id in ignore) continue
            if (segDist(b.x, b.y, x0, y0, x1, y1) < clearance) return false
        }
        return true
    }

    fun isFree(x: Float, y: Float, except: Int = 0): Boolean =
        x in R..(L - R) && y in R..(W - R) && balls.none { !it.pocketed && it.id != except && hypot(it.x - x, it.y - y) < 2 * R + 0.002f }

    companion object {
        const val L = 2.0f
        const val W = 1.0f
        const val R = 0.03f
        const val CORNER_R = 0.07f
        const val SIDE_R = 0.058f
        const val FRICTION = 0.55f
        const val STOP_SPEED = 0.01f
        const val BALL_E = 0.96f
        const val CUSHION_E = 0.78f
        const val SUB_DT = 1f / 480f
        const val MAX_SPEED = 4.6f

        val POCKETS = listOf(
            0f to 0f, L / 2 to -0.012f, L to 0f,
            0f to W, L / 2 to W + 0.012f, L to W,
        )

        fun inPocket(x: Float, y: Float): Boolean {
            for ((i, p) in POCKETS.withIndex()) {
                val r = if (i == 1 || i == 4) SIDE_R else CORNER_R
                if (hypot(x - p.first, y - p.second) < r) return true
            }
            return false
        }

        private fun inCornerMouthY(y: Float) = y < CORNER_R * 1.05f || y > W - CORNER_R * 1.05f
        private fun inMouthX(x: Float) = x < CORNER_R * 1.05f || x > L - CORNER_R * 1.05f || abs(x - L / 2) < SIDE_R * 0.95f

        fun segDist(px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float): Float {
            val dx = x1 - x0
            val dy = y1 - y0
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0f) 0f else (((px - x0) * dx + (py - y0) * dy) / len2).coerceIn(0f, 1f)
            return hypot(px - (x0 + t * dx), py - (y0 + t * dy))
        }

        /** Ucgen dizilis: 8 ortada, arka koselerde biri duz biri cizgili. */
        fun rack(random: Random = Random.Default): PoolTable {
            val balls = ArrayList<Ball>()
            balls += Ball(0, L * 0.25f, W / 2)
            val order = ArrayList<Int>()
            val others = (1..15).filter { it != 8 && it != 1 && it != 9 && it != 2 }.shuffled(random).toMutableList()
            // Satir satir 15 yuva; 5. yuva (3. satir ortasi) 8
            val slots = 15
            for (k in 0 until slots) {
                order += when (k) {
                    0 -> 1
                    4 -> 8
                    10 -> 9
                    14 -> 2
                    else -> others.removeAt(0)
                }
            }
            val apexX = L * 0.72f
            var k = 0
            val dx = 2 * R * 0.87f + 0.001f
            for (row in 0 until 5) {
                for (i in 0..row) {
                    val x = apexX + row * dx
                    val y = W / 2 + (i - row / 2f) * (2 * R + 0.001f)
                    balls += Ball(order[k++], x, y)
                }
            }
            balls.sortBy { it.id }
            return PoolTable(balls)
        }
    }
}

// ==================================================================== Kurallar

enum class PoolEvent { NONE, CONTINUE, SWITCH, FOUL, GROUPS_SET, WIN, LOSE }

class PoolGame(random: Random = Random.Default) {
    val table: PoolTable = PoolTable.rack(random)
    var turn = 0
        private set
    /** Oyuncu basina grup (null = masa acik) */
    val groups = arrayOfNulls<PoolGroup>(2)
    var ballInHand = false
        private set
    var breakShot = true
        private set
    var winner = -1
        private set
    var lastEvent = PoolEvent.NONE
        private set
    var shooting = false
        private set

    val over: Boolean get() = winner >= 0

    fun remaining(group: PoolGroup?): Int = if (group == null) 7 else table.balls.count { !it.pocketed && groupOf(it.id) == group }

    /** Oyuncunun vurmasi gereken toplar. */
    fun targetsFor(player: Int): Set<Int> {
        val g = groups[player]
        return when {
            g == null -> (1..15).filter { it != 8 }.toSet()
            remaining(g) == 0 -> setOf(8)
            else -> (1..15).filter { groupOf(it) == g }.toSet()
        }.filter { !table.balls[it].pocketed }.toSet()
    }

    fun placeCue(x: Float, y: Float): Boolean {
        if (!ballInHand || shooting) return false
        if (!table.isFree(x, y)) return false
        val c = table.cue
        c.x = x; c.y = y; c.pocketed = false
        return true
    }

    /** [angle] radyan (masa koordinati), [power] 0..1 */
    fun shoot(angle: Float, power: Float) {
        if (shooting || over) return
        val c = table.cue
        val v = PoolTable.MAX_SPEED * power.coerceIn(0.03f, 1f)
        c.vx = cos(angle) * v
        c.vy = sin(angle) * v
        table.log = ShotLog()
        targetsBeforeShot = targetsFor(turn)
        shooting = true
        ballInHand = false
    }

    /** Toplar durunca true doner ve kurallar uygulanir. */
    fun update(dt: Float): Boolean {
        if (!shooting) return false
        table.step(dt)
        if (table.moving) return false
        shooting = false
        resolve()
        return true
    }

    /** Vurus bitince kurallari uygula. */
    private fun resolve() {
        val log = table.log
        val me = turn
        val opp = 1 - turn
        val potted = log.pocketed.filter { it != 0 }
        val scratch = 0 in log.pocketed
        val targetsBefore = targetsBeforeShot
        val first = log.firstContact
        val wasBreak = breakShot
        breakShot = false

        var foul = scratch || first < 0
        if (!foul && !wasBreak) {
            foul = first !in targetsBefore
        }

        if (8 in potted) {
            if (wasBreak && !scratch) {
                // Kirarken 8 dustu: geri koy
                respot(8)
            } else {
                val cleared = targetsBefore == setOf(8)
                winner = if (cleared && !foul) me else opp
                lastEvent = if (winner == me) PoolEvent.WIN else PoolEvent.LOSE
                return
            }
        }

        var event = PoolEvent.SWITCH
        if (!foul && !wasBreak && groups[me] == null && potted.any { it != 8 }) {
            val g = groupOf(potted.first { it != 8 })!!
            groups[me] = g
            groups[opp] = if (g == PoolGroup.SOLID) PoolGroup.STRIPE else PoolGroup.SOLID
            event = PoolEvent.GROUPS_SET
        }
        val myGroup = groups[me]
        val scored = potted.any { it != 8 && (myGroup == null || groupOf(it) == myGroup) }
        if (scratch) respotCue()
        if (foul) {
            turn = opp
            ballInHand = true
            lastEvent = PoolEvent.FOUL
        } else if (scored || (wasBreak && potted.isNotEmpty())) {
            lastEvent = if (event == PoolEvent.GROUPS_SET) PoolEvent.GROUPS_SET else PoolEvent.CONTINUE
        } else {
            turn = opp
            lastEvent = PoolEvent.SWITCH
        }
    }

    /** Vurus anindaki hedefler (kurallar bunlara gore). */
    private var targetsBeforeShot: Set<Int> = emptySet()

    private fun respot(id: Int) {
        val b = table.balls[id]
        var x = PoolTable.L * 0.72f
        while (!table.isFree(x, PoolTable.W / 2, except = id) && x < PoolTable.L - PoolTable.R) x += 0.01f
        b.x = x; b.y = PoolTable.W / 2; b.pocketed = false; b.vx = 0f; b.vy = 0f
    }

    private fun respotCue() {
        val c = table.cue
        c.pocketed = false
        c.vx = 0f; c.vy = 0f
        var x = PoolTable.L * 0.25f
        while (!table.isFree(x, PoolTable.W / 2) && x > PoolTable.R) x -= 0.01f
        c.x = x; c.y = PoolTable.W / 2
    }
}

// ==================================================================== Yapay zeka

object PoolAi {
    class Plan(val angle: Float, val power: Float, val score: Float)

    /** [noiseDeg] nisan hatasi (derece). */
    fun choose(game: PoolGame, player: Int, noiseDeg: Float, random: Random = Random.Default): Plan {
        val t = game.table
        if (game.ballInHand) placeBest(game, player, random)
        val cue = t.cue
        val targets = game.targetsFor(player)
        var best: Plan? = null
        for (id in targets) {
            val b = t.balls[id]
            for ((pi, p) in PoolTable.POCKETS.withIndex()) {
                val px = p.first + (if (p.first == 0f) 0.02f else if (p.first == PoolTable.L) -0.02f else 0f)
                val py = p.second + (if (pi < 3) 0.02f else -0.02f)
                val dx = px - b.x
                val dy = py - b.y
                val dist = hypot(dx, dy)
                if (dist < 0.001f) continue
                val ux = dx / dist
                val uy = dy / dist
                // Hayalet top
                val gx = b.x - ux * 2 * PoolTable.R
                val gy = b.y - uy * 2 * PoolTable.R
                val ax = gx - cue.x
                val ay = gy - cue.y
                val aim = hypot(ax, ay)
                if (aim < 0.001f) continue
                val cut = (ax * ux + ay * uy) / aim // 1 = duz
                if (cut < 0.3f) continue
                if (!t.clearPath(cue.x, cue.y, gx, gy, setOf(0, id))) continue
                if (!t.clearPath(b.x, b.y, px, py, setOf(0, id))) continue
                val angle = atan2(ay, ax)
                val power = (0.25f + (aim + dist / cut) * 0.28f).coerceIn(0.2f, 0.95f)
                // Simulasyonla dogrula
                val sim = simulate(game, angle, power)
                var score = cut * 2f - dist * 0.5f - aim * 0.3f
                if (id in sim.pocketed) score += 5f
                if (0 in sim.pocketed) score -= 8f
                if (8 in sim.pocketed && id != 8) score -= 20f
                if (best == null || score > best.score) best = Plan(angle, power, score)
            }
        }
        val plan = best ?: safety(game, player, random)
        val noise = Math.toRadians((random.nextFloat() * 2 - 1) * noiseDeg.toDouble()).toFloat()
        return Plan(plan.angle + noise, plan.power, plan.score)
    }

    /** Uygun vurus yoksa: hedef toplardan birine dogrudan, orta gucte. */
    private fun safety(game: PoolGame, player: Int, random: Random): Plan {
        val t = game.table
        val cue = t.cue
        val target = game.targetsFor(player).map { t.balls[it] }
            .sortedBy { hypot(it.x - cue.x, it.y - cue.y) }
            .firstOrNull { t.clearPath(cue.x, cue.y, it.x, it.y, setOf(0, it.id), PoolTable.R * 1.9f) }
            ?: game.targetsFor(player).map { t.balls[it] }.firstOrNull()
        return if (target != null) Plan(atan2(target.y - cue.y, target.x - cue.x), 0.45f, -1f)
        else Plan(random.nextFloat() * 6.28f, 0.5f, -2f)
    }

    private fun simulate(game: PoolGame, angle: Float, power: Float): ShotLog {
        val t = game.table.copy()
        val c = t.cue
        val v = PoolTable.MAX_SPEED * power
        c.vx = cos(angle) * v
        c.vy = sin(angle) * v
        t.log = ShotLog()
        var time = 0f
        while (t.moving && time < 12f) {
            t.step(1f / 60f)
            time += 1f / 60f
        }
        return t.log
    }

    /** Elde top: birkac aday konumdan en iyi atis acisi verene koy. */
    private fun placeBest(game: PoolGame, player: Int, random: Random) {
        val t = game.table
        var bestPos: Pair<Float, Float>? = null
        var bestScore = -1e9f
        repeat(10) {
            val x = PoolTable.R * 2 + random.nextFloat() * (PoolTable.L - PoolTable.R * 4)
            val y = PoolTable.R * 2 + random.nextFloat() * (PoolTable.W - PoolTable.R * 4)
            if (!t.isFree(x, y)) return@repeat
            // En yakin hedefe duz bakis
            val s = game.targetsFor(player).maxOfOrNull { id ->
                val b = t.balls[id]
                if (t.clearPath(x, y, b.x, b.y, setOf(0, id))) 1f / (0.2f + hypot(b.x - x, b.y - y)) else 0f
            } ?: 0f
            if (s > bestScore) {
                bestScore = s
                bestPos = x to y
            }
        }
        bestPos?.let { game.placeCue(it.first, it.second) }
    }
}
