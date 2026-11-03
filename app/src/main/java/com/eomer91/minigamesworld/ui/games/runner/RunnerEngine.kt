package com.eomer91.minigamesworld.ui.games.runner

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/*
 * Maden Kosusu: 3 serit, ileri dogru akan engeller. z = oyuncunun onundeki mesafe (birim), serit -1/0/1.
 * Android'den bagimsiz; unit test ile test edilir.
 */

enum class ObstacleType { ROCK, BEAM, BARREL, COIN }

class Obstacle(val type: ObstacleType, val lane: Int, var z: Float) {
    var taken = false
}

class RunnerEngine(private val random: Random = Random.Default) {
    var lane = 0
        private set
    /** Gorsel serit konumu (yumusak gecis) */
    var laneX = 0f
        private set
    var jumpY = 0f
        private set
    private var jumpV = 0f
    var duckTime = 0f
        private set
    val ducking: Boolean get() = duckTime > 0f
    val jumping: Boolean get() = jumpY > 0f || jumpV > 0f

    var speed = START_SPEED
        private set
    var distance = 0f
        private set
    var coins = 0
        private set
    var dead = false
        private set
    var time = 0f
        private set
    /** Raylarin kayma fazi (travers animasyonu) */
    var scroll = 0f
        private set
    var hitFlash = 0f
        private set

    val obstacles = ArrayList<Obstacle>()
    private var nextSpawn = 30f

    // Olaylar
    var coinEvents = 0
        private set
    var jumpEvents = 0
        private set
    var crashEvents = 0
        private set

    val score: Int get() = distance.toInt() + coins * COIN_SCORE

    // ================================================================ Girdi

    fun left() {
        if (!dead && lane > -1) lane--
    }

    fun right() {
        if (!dead && lane < 1) lane++
    }

    fun jump() {
        if (dead || jumpY > 0f) return
        duckTime = 0f
        jumpV = JUMP_V
        jumpEvents++
    }

    fun duck() {
        if (dead) return
        duckTime = DUCK_TIME
        // Havadayken asagi kaydirma hizli inis yapar
        if (jumpY > 0f) jumpV = -JUMP_V
    }

    // ================================================================ Guncelleme

    fun update(dt: Float) {
        if (dead) {
            hitFlash = (hitFlash - dt).coerceAtLeast(0f)
            return
        }
        time += dt
        speed = min(MAX_SPEED, START_SPEED + time * ACCEL)
        val dz = speed * dt
        distance += dz
        scroll = (scroll + dz) % 2f

        // Serit gecisi
        val target = lane.toFloat()
        val step = LANE_SPEED * dt
        laneX = if (abs(target - laneX) <= step) target else laneX + step * (if (target > laneX) 1 else -1)

        // Ziplama
        if (jumpY > 0f || jumpV > 0f) {
            jumpV -= GRAVITY * dt
            jumpY += jumpV * dt
            if (jumpY <= 0f) {
                jumpY = 0f
                jumpV = 0f
            }
        }
        if (duckTime > 0f) duckTime = (duckTime - dt).coerceAtLeast(0f)
        hitFlash = (hitFlash - dt).coerceAtLeast(0f)

        // Engeller yaklasir
        for (o in obstacles) {
            val before = o.z
            o.z -= dz
            if (!o.taken && before > -0.8f && o.z < HIT_DEPTH) checkHit(o)
        }
        obstacles.removeAll { it.z < -3f }

        // Uretim
        nextSpawn -= dz
        if (nextSpawn <= 0f) {
            spawnPattern(FAR)
            nextSpawn = gap()
        }
    }

    private fun checkHit(o: Obstacle) {
        // Serit gecisi sirasinda en yakin serit sayilir
        val myLane = laneX.let { if (it < -0.5f) -1 else if (it > 0.5f) 1 else 0 }
        if (o.lane != myLane) return
        when (o.type) {
            ObstacleType.COIN -> {
                if (jumpY < 1.6f) {
                    o.taken = true
                    coins++
                    coinEvents++
                }
            }
            ObstacleType.ROCK -> crash(o)
            ObstacleType.BEAM -> if (!ducking) crash(o)
            ObstacleType.BARREL -> if (jumpY < BARREL_HEIGHT) crash(o)
        }
    }

    private fun crash(o: Obstacle) {
        o.taken = true
        dead = true
        crashEvents++
        hitFlash = 0.6f
    }

    /** Engeller arasi mesafe hizla birlikte buyur (tepki suresi korunur). */
    private fun gap(): Float = speed * (0.85f + random.nextFloat() * 0.5f)

    /** Her zaman en az bir serit gecilebilir kalir. */
    fun spawnPattern(z: Float) {
        when (random.nextInt(10)) {
            0, 1, 2 -> {
                // Iki seritte kaya, birinde bos (belki sikke)
                val free = random.nextInt(3) - 1
                for (l in -1..1) if (l != free) obstacles += Obstacle(ObstacleType.ROCK, l, z)
                coinLine(free, z - 4f, 4)
            }
            3, 4 -> {
                // Tum seritlerde kiris: egil
                for (l in -1..1) obstacles += Obstacle(ObstacleType.BEAM, l, z)
            }
            5, 6 -> {
                // Fici: zipla ya da seridi degistir
                val l = random.nextInt(3) - 1
                obstacles += Obstacle(ObstacleType.BARREL, l, z)
                coinLine(l, z + 3f, 3)
            }
            7 -> {
                // Fici sirasi (tum seritler): zipla
                for (l in -1..1) obstacles += Obstacle(ObstacleType.BARREL, l, z)
            }
            else -> coinLine(random.nextInt(3) - 1, z, 6)
        }
    }

    private fun coinLine(lane: Int, z: Float, n: Int) {
        for (i in 0 until n) obstacles += Obstacle(ObstacleType.COIN, lane, z + i * 2.2f)
    }

    /** Odullu reklamla devam: yakindaki engeller temizlenir. */
    fun revive() {
        dead = false
        obstacles.removeAll { it.z < 25f }
        jumpY = 0f
        jumpV = 0f
        duckTime = 0f
        nextSpawn = 20f
    }

    companion object {
        const val START_SPEED = 14f
        const val MAX_SPEED = 34f
        const val ACCEL = 0.22f
        const val JUMP_V = 8.5f
        const val GRAVITY = 24f
        const val DUCK_TIME = 0.75f
        const val LANE_SPEED = 9f
        const val BARREL_HEIGHT = 0.7f
        const val HIT_DEPTH = 0.6f
        const val FAR = 70f
        const val COIN_SCORE = 25
    }
}
