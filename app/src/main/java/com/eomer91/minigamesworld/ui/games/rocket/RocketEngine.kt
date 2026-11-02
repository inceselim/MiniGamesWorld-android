package com.eomer91.minigamesworld.ui.games.rocket

import kotlin.math.abs
import kotlin.random.Random

/** Asteroit kolonu cifti; [gapY] boslugun merkezi, [gapH] yuksekligi. */
data class Gate(
    val id: Long,
    val x: Float,
    val gapY: Float,
    val gapH: Float,
    val passed: Boolean = false,
    val hasStar: Boolean = false,
    val starTaken: Boolean = false,
)

data class RocketGame(
    val y: Float = RocketEngine.HEIGHT / 2f,
    val vy: Float = 0f,
    val gates: List<Gate> = emptyList(),
    /** Bir sonraki kapinin dogacagi mesafe sayaci. */
    val distanceToNext: Float = 2f,
    /** Son boslugun merkezi: art arda bosluklar arasi fark sinirli tutulur. */
    val lastGapY: Float = RocketEngine.HEIGHT / 2f,
    val score: Int = 0,
    val gatesPassed: Int = 0,
    val stars: Int = 0,
    val started: Boolean = false,
    val gameOver: Boolean = false,
    /** Kalkan suresi (saniye); > 0 iken carpismalar sayilmaz. */
    val shield: Float = 0f,
    val time: Float = 0f,
)

/** Bir karedeki olaylar (ses icin). */
data class RocketEvents(val passed: Int = 0, val star: Boolean = false, val crashed: Boolean = false)

/** Roket Zipla kurallari. Dunya [WIDTH] x [HEIGHT] birim. Android'den bagimsiz. */
class RocketEngine(private val random: Random = Random.Default) {
    private var nextId = 1L

    fun newGame() = RocketGame()

    fun speed(g: RocketGame) = (START_SPEED + g.gatesPassed * 0.06f).coerceAtMost(MAX_SPEED)
    fun gapHeight(g: RocketGame) = (START_GAP - g.gatesPassed * 0.03f).coerceAtLeast(MIN_GAP)

    fun flap(g: RocketGame): RocketGame {
        if (g.gameOver) return g
        return g.copy(vy = FLAP_VELOCITY, started = true)
    }

    fun update(g: RocketGame, dt: Float): Pair<RocketGame, RocketEvents> {
        if (g.gameOver) return g to RocketEvents()
        if (!g.started) {
            // Baslamadan once roket hafifce suzulur
            return g.copy(time = g.time + dt, y = HEIGHT / 2f + kotlin.math.sin(g.time * 3f) * 0.25f) to RocketEvents()
        }

        val vy = (g.vy + GRAVITY * dt).coerceAtMost(MAX_FALL)
        var y = g.y + vy * dt
        if (y < ROCKET_RADIUS) y = ROCKET_RADIUS
        val speed = speed(g)
        val dx = speed * dt

        // Kapilar kayar; ekrandan cikanlar silinir
        var gates = g.gates.map { it.copy(x = it.x - dx) }.filter { it.x + GATE_WIDTH > -1f }
        var distance = g.distanceToNext - dx
        var lastGapY = g.lastGapY
        if (distance <= 0f) {
            val gap = gapHeight(g)
            val margin = gap / 2f + 1.2f
            // Bir onceki bosluga gore en fazla MAX_SHIFT kadar yukari/asagi: ulasilamaz bosluk olmasin.
            val lo = maxOf(margin, g.lastGapY - MAX_SHIFT)
            val hi = minOf(GROUND_Y - margin, g.lastGapY + MAX_SHIFT)
            val gapY = lo + random.nextFloat() * (hi - lo)
            lastGapY = gapY
            gates = gates + Gate(nextId++, WIDTH + 0.5f, gapY, gap, hasStar = random.nextFloat() < STAR_CHANCE)
            distance += GATE_SPACING
        }

        // Gecilen kapilar ve yildizlar
        var passed = 0
        var star = false
        gates = gates.map { gate ->
            var gt = gate
            if (!gt.passed && gt.x + GATE_WIDTH < ROCKET_X - ROCKET_RADIUS) {
                gt = gt.copy(passed = true)
                passed++
            }
            if (gt.hasStar && !gt.starTaken && abs(gt.x + GATE_WIDTH / 2f - ROCKET_X) < 0.6f && abs(gt.gapY - y) < 0.7f) {
                gt = gt.copy(starTaken = true)
                star = true
            }
            gt
        }

        val shield = (g.shield - dt).coerceAtLeast(0f)
        val crashed = shield <= 0f && (y + ROCKET_RADIUS >= GROUND_Y || gates.any { hits(it, y) })
        val finalY = if (y + ROCKET_RADIUS >= GROUND_Y) GROUND_Y - ROCKET_RADIUS else y

        val next = g.copy(
            y = finalY,
            vy = if (crashed) 0f else vy,
            gates = gates,
            distanceToNext = distance,
            lastGapY = lastGapY,
            score = g.score + passed * POINTS_PER_GATE + if (star) POINTS_PER_STAR else 0,
            gatesPassed = g.gatesPassed + passed,
            stars = g.stars + if (star) 1 else 0,
            gameOver = crashed,
            shield = shield,
            time = g.time + dt,
        )
        return next to RocketEvents(passed, star, crashed)
    }

    /** Roket (daire) ile kolonun ust/alt dikdortgenleri cakisiyor mu. */
    fun hits(gate: Gate, y: Float): Boolean {
        val left = gate.x
        val right = gate.x + GATE_WIDTH
        val closestX = ROCKET_X.coerceIn(left, right)
        val dx = ROCKET_X - closestX
        if (abs(dx) > ROCKET_RADIUS) return false
        val gapTop = gate.gapY - gate.gapH / 2f
        val gapBottom = gate.gapY + gate.gapH / 2f
        // Ust kolon: 0..gapTop, alt kolon: gapBottom..GROUND_Y
        fun overlaps(top: Float, bottom: Float): Boolean {
            val closestY = y.coerceIn(top, bottom)
            val dy = y - closestY
            return dx * dx + dy * dy < ROCKET_RADIUS * ROCKET_RADIUS * 0.8f
        }
        return overlaps(-10f, gapTop) || overlaps(gapBottom, GROUND_Y)
    }

    /** Odullu reklamla devam: yakindaki kapilar temizlenir, kalkan verilir. */
    fun revive(g: RocketGame): RocketGame = g.copy(
        gameOver = false,
        vy = FLAP_VELOCITY * 0.6f,
        y = g.y.coerceIn(3f, GROUND_Y - 3f),
        gates = g.gates.filter { it.x > ROCKET_X + 3f },
        shield = REVIVE_SHIELD,
    )

    companion object {
        const val WIDTH = 9f
        const val HEIGHT = 16f
        const val GROUND_Y = 15f
        const val ROCKET_X = 2.6f
        const val ROCKET_RADIUS = 0.38f
        const val GRAVITY = 30f
        const val FLAP_VELOCITY = -9.6f
        const val MAX_FALL = 14f
        const val GATE_WIDTH = 1.5f
        const val GATE_SPACING = 4.8f
        const val START_SPEED = 3.6f
        const val MAX_SPEED = 5.6f
        const val START_GAP = 4.4f
        const val MIN_GAP = 3.5f
        const val STAR_CHANCE = 0.35f
        /** Simulasyonla ayarlandi: 3 birimde basit bir otomatik pilot bile 20+ kapi gecebiliyor. */
        const val MAX_SHIFT = 3f
        const val POINTS_PER_GATE = 10
        const val POINTS_PER_STAR = 5
        const val REVIVE_SHIELD = 2f
    }
}
