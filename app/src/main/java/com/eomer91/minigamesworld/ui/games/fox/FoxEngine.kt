package com.eomer91.minigamesworld.ui.games.fox

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/*
 * Tilki Macerasi platform fizigi. Birim = 1 karo; y asagi dogru artar. (x, y) kutunun sol-ust kosesi.
 * Android'den bagimsiz, unit test ile test edilir.
 */

enum class FoxEnemyType(val w: Float, val h: Float, val speed: Float, val stompable: Boolean, val flying: Boolean) {
    SNAIL(0.9f, 0.75f, 1.1f, true, false),
    BEE(0.85f, 0.7f, 1.6f, true, true),
    HEDGEHOG(0.9f, 0.75f, 1.6f, false, false),
}

class FoxEnemy(val type: FoxEnemyType, var x: Float, var y: Float) {
    var vx = -type.speed
    var vy = 0f
    var alive = true
    var squash = 0f
    val homeX = x
    val homeY = y
    var t = 0f
}

class FoxParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Long, val size: Float)

/** Bolum sonu ozeti. */
data class FoxResult(val stars: Int, val coins: Int, val time: Float, val deaths: Int) {
    val score: Int get() = 1000 + coins * 10 + stars * 300 + max(0, 300 - time.toInt()) * 2
}

class FoxEngine(
    val world: Int,
    val levelIndex: Int,
    private val source: List<String>,
    private val random: Random = Random.Default,
) {
    val height = source.size
    val width = source.first().length

    /** Degisebilir karolar (tugla kirilir, soru blogu kullanilir, sikke toplanir). */
    val tiles: Array<CharArray> = Array(height) { source[it].toCharArray() }

    var px = 0f
    var py = 0f
    var vx = 0f
    var vy = 0f
    var facingRight = true
    var onGround = false
    var hearts = MAX_HEARTS
    var iframe = 0f
    var coins = 0
    var stars = 0
    var time = 0f
    var deaths = 0
    var dead = false
    var won = false
    var deathTimer = 0f
    var winTimer = 0f
    var runAnim = 0f

    /** Ses/titresim icin sayaclar (ViewModel kareler arasi farka bakar). */
    var jumpCount = 0
        private set
    var stompCount = 0
        private set
    var springCount = 0
        private set
    var breakCount = 0
        private set
    var hurtCount = 0
        private set

    private var coyote = 0f
    private var jumpBuffer = 0f
    private var jumpHeld = false
    /** Son guvenli zemin (odullu "burada devam" icin). */
    private var safeX = 0f
    private var safeY = 0f
    private var checkpointX = 0f
    private var checkpointY = 0f
    var checkpointCol = -1
        private set

    val enemies = ArrayList<FoxEnemy>()
    val particles = ArrayList<FoxParticle>()
    /** Kafa vurulan blogun kisa zipla animasyonu: (satir*genislik+sutun) -> kalan sure */
    val bumps = HashMap<Int, Float>()
    /** Bloklardan cikan sikke animasyonlari: x, y, kalan sure */
    val coinPops = ArrayList<FloatArray>()
    var flagCol = width - 2
        private set

    /** Tum yildiz karolarinin baslangic sayisi (genelde 3). */
    val totalStars: Int

    init {
        var stars0 = 0
        for (r in 0 until height) for (c in 0 until width) {
            when (tiles[r][c]) {
                '@' -> {
                    checkpointX = c.toFloat() + (1f - PW) / 2f
                    checkpointY = r + 1f - PH
                    tiles[r][c] = '.'
                }
                'e' -> spawnEnemy(FoxEnemyType.SNAIL, r, c)
                'b' -> spawnEnemy(FoxEnemyType.BEE, r, c)
                'h' -> spawnEnemy(FoxEnemyType.HEDGEHOG, r, c)
                'F' -> flagCol = c
                '*' -> stars0++
            }
        }
        totalStars = stars0
        respawn()
    }

    private fun spawnEnemy(type: FoxEnemyType, r: Int, c: Int) {
        tiles[r][c] = '.'
        enemies += FoxEnemy(type, c + (1f - type.w) / 2f, r + 1f - type.h)
    }

    private fun respawn() {
        px = checkpointX
        py = checkpointY
        safeX = px
        safeY = py
        vx = 0f
        vy = 0f
        hearts = MAX_HEARTS
        iframe = 1f
        dead = false
        deathTimer = 0f
    }

    // ================================================================ Karo sorgulari

    fun tile(c: Int, r: Int): Char = if (r < 0 || c < 0 || c >= width) '#' else if (r >= height) '.' else tiles[r][c]

    private fun isSolid(ch: Char) = ch == '#' || ch == 'B' || ch == '?' || ch == 'H' || ch == 'U' || ch == 'S'

    // ================================================================ Girdi

    fun pressJump() {
        jumpBuffer = JUMP_BUFFER
        jumpHeld = true
    }

    fun releaseJump() {
        jumpHeld = false
        if (vy < 0f) vy *= JUMP_CUT
    }

    // ================================================================ Guncelleme

    /** [dir]: -1 sol, 0, 1 sag */
    fun update(dt: Float, dir: Int) {
        if (won) {
            winTimer += dt
            updateParticles(dt)
            return
        }
        if (dead) {
            deathTimer += dt
            vy = min(vy + GRAVITY * dt, MAX_FALL)
            py += vy * dt
            updateParticles(dt)
            return
        }
        time += dt
        iframe = max(0f, iframe - dt)
        coyote -= dt
        jumpBuffer -= dt

        // Yatay
        val target = dir * RUN_SPEED
        val accel = if (onGround) (if (dir == 0) DECEL else ACCEL) else AIR_ACCEL
        vx = approach(vx, target, accel * dt)
        if (dir != 0) facingRight = dir > 0
        if (onGround && abs(vx) > 0.3f) runAnim += dt * abs(vx) * 1.6f

        // Ziplama (coyote + tampon)
        if (jumpBuffer > 0f && (onGround || coyote > 0f)) {
            vy = -JUMP_SPEED
            onGround = false
            coyote = 0f
            jumpBuffer = 0f
            jumpCount++
            if (!jumpHeld) vy *= JUMP_CUT
        }

        vy = min(vy + GRAVITY * dt, MAX_FALL)
        moveX(vx * dt)
        val wasGround = onGround
        moveY(vy * dt)
        if (wasGround && !onGround && vy >= 0f) coyote = COYOTE
        if (onGround && iframe <= 0f && isSafeSpot()) {
            safeX = px
            safeY = py
        }

        collectAndHazards()
        updateEnemies(dt)
        updateParticles(dt)

        // Cukur
        if (py > height + 1f) die(pit = true)
        // Bayrak
        if (px + PW / 2f >= flagCol) {
            won = true
            vx = 0f
            burst(px + PW / 2, py, 30, 0xFFF7D046, 6f)
        }
    }

    private fun approach(v: Float, t: Float, d: Float) = if (v < t) min(v + d, t) else max(v - d, t)

    private fun moveX(dx: Float) {
        px += dx
        val top = floor(py + 0.01f).toInt()
        val bottom = floor(py + PH - 0.01f).toInt()
        if (dx > 0) {
            val c = floor(px + PW).toInt()
            for (r in top..bottom) if (isSolid(tile(c, r))) {
                px = c - PW
                vx = 0f
                return
            }
        } else if (dx < 0) {
            val c = floor(px).toInt()
            for (r in top..bottom) if (isSolid(tile(c, r))) {
                px = c + 1f
                vx = 0f
                return
            }
        }
    }

    private fun moveY(dy: Float) {
        val prevBottom = py + PH
        py += dy
        val left = floor(px + 0.02f).toInt()
        val right = floor(px + PW - 0.02f).toInt()
        onGround = false
        if (dy > 0) {
            val r = floor(py + PH).toInt()
            for (c in left..right) {
                val ch = tile(c, r)
                val oneWay = ch == '=' && prevBottom <= r + 0.05f
                if (isSolid(ch) || oneWay) {
                    py = r - PH
                    if (ch == 'S') {
                        vy = -SPRING_SPEED
                        springCount++
                        burst(c + 0.5f, r.toFloat(), 6, 0xFFE04848, 3f)
                    } else {
                        vy = 0f
                        onGround = true
                    }
                    return
                }
            }
        } else if (dy < 0) {
            val r = floor(py).toInt()
            // Ortadaki bloga vur (genis kutuda iki blok birden kirilmasin)
            val mid = floor(px + PW / 2f).toInt()
            val order = listOf(mid) + (left..right).filter { it != mid }
            for (c in order) {
                if (isSolid(tile(c, r))) {
                    py = r + 1f
                    vy = 0f
                    bumpBlock(c, r)
                    return
                }
            }
        }
    }

    /** Ayaklarin oldugu satirda ve hemen yaninda diken yoksa guvenli. */
    private fun isSafeSpot(): Boolean {
        val r = floor(py + PH - 0.1f).toInt()
        for (c in floor(px).toInt() - 1..floor(px + PW).toInt() + 1) {
            val ch = tile(c, r)
            if (ch == '^' || ch == 'S') return false
        }
        return true
    }

    private fun bumpBlock(c: Int, r: Int) {
        if (r < 0 || r >= height) return
        when (tiles[r][c]) {
            '?' -> {
                tiles[r][c] = 'U'
                coins++
                coinPops += floatArrayOf(c + 0.5f, r - 0.5f, 0.5f)
                bumps[r * width + c] = 0.15f
            }
            'H' -> {
                tiles[r][c] = 'U'
                hearts = min(MAX_HEARTS, hearts + 1)
                burst(c + 0.5f, r - 0.3f, 10, 0xFFE04848, 3f)
                bumps[r * width + c] = 0.15f
            }
            'B' -> {
                tiles[r][c] = '.'
                breakCount++
                burst(c + 0.5f, r + 0.5f, 10, 0xFFBE5432, 5f)
            }
            else -> bumps[r * width + c] = 0.1f
        }
    }

    private fun collectAndHazards() {
        val c0 = floor(px).toInt()
        val c1 = floor(px + PW).toInt()
        val r0 = floor(py).toInt()
        val r1 = floor(py + PH).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            if (r < 0 || r >= height || c < 0 || c >= width) continue
            // Karo ile kutu ust uste mi (kucuk tolerans)
            val overlap = px < c + 0.85f && px + PW > c + 0.15f && py < r + 0.9f && py + PH > r + 0.15f
            if (!overlap) continue
            when (tiles[r][c]) {
                'c' -> {
                    tiles[r][c] = '.'
                    coins++
                    burst(c + 0.5f, r + 0.5f, 4, 0xFFF7D046, 2f)
                }
                '*' -> {
                    tiles[r][c] = '.'
                    stars++
                    burst(c + 0.5f, r + 0.5f, 18, 0xFFFFE680, 4f)
                }
                'K' -> if (checkpointCol != c) {
                    checkpointCol = c
                    checkpointX = c + (1f - PW) / 2f
                    checkpointY = r + 1f - PH
                    burst(c + 0.5f, r.toFloat(), 12, 0xFF4ADE80, 3f)
                }
                '^' -> if (py + PH > r + 0.5f) hurt()
                'L' -> die(pit = true)
            }
        }
    }

    // ================================================================ Dusmanlar

    private fun updateEnemies(dt: Float) {
        for (e in enemies) {
            if (!e.alive) {
                e.squash -= dt
                continue
            }
            e.t += dt
            // Sadece ekrana yakin dusmanlar hareket eder
            if (abs(e.x - px) > 18f) continue
            if (e.type.flying) {
                e.x += e.vx * dt
                if (e.x < e.homeX - 2.5f) e.vx = abs(e.vx)
                if (e.x > e.homeX + 2.5f) e.vx = -abs(e.vx)
                e.y = e.homeY + sin(e.t * 3f) * 0.8f
            } else {
                e.vy = min(e.vy + GRAVITY * dt, MAX_FALL)
                e.y += e.vy * dt
                val br = floor(e.y + e.type.h).toInt()
                val cl = floor(e.x + 0.05f).toInt()
                val cr = floor(e.x + e.type.w - 0.05f).toInt()
                if (isSolid(tile(cl, br)) || isSolid(tile(cr, br)) || tile(cl, br) == '=' || tile(cr, br) == '=') {
                    e.y = br - e.type.h
                    e.vy = 0f
                }
                // Duvar ya da ucurum kenarinda don
                val nx = e.x + e.vx * dt
                val frontC = if (e.vx > 0) floor(nx + e.type.w).toInt() else floor(nx).toInt()
                val midR = floor(e.y + e.type.h * 0.5f).toInt()
                val footR = floor(e.y + e.type.h + 0.1f).toInt()
                val wall = isSolid(tile(frontC, midR))
                val edge = !isSolid(tile(frontC, footR)) && tile(frontC, footR) != '='
                if (wall || edge) e.vx = -e.vx else e.x = nx
                if (e.y > height + 1) e.alive = false
            }
            // Oyuncu ile temas
            if (px < e.x + e.type.w && px + PW > e.x && py < e.y + e.type.h && py + PH > e.y) {
                val stomp = vy > 0f && (py + PH) - e.y < 0.5f
                if (stomp && e.type.stompable) {
                    e.alive = false
                    e.squash = 0.4f
                    stompCount++
                    vy = -STOMP_BOUNCE * (if (jumpHeld) 1.3f else 1f)
                    burst(e.x + e.type.w / 2, e.y + e.type.h / 2, 8, 0xFFFFFFFF, 3f)
                } else {
                    hurt()
                }
            }
        }
    }

    private fun hurt() {
        if (iframe > 0f || dead || won) return
        hearts--
        hurtCount++
        iframe = HURT_IFRAME
        vy = -8f
        vx = if (facingRight) -5f else 5f
        burst(px + PW / 2, py + PH / 2, 8, 0xFFE04848, 3f)
        if (hearts <= 0) die(pit = false)
    }

    private fun die(pit: Boolean) {
        if (dead) return
        dead = true
        deaths++
        hearts = 0
        vy = if (pit) 0f else -12f
        vx = 0f
    }

    /** Kontrol noktasindan devam: bolum (bloklar, sikkeler, dusmanlar) oldugu gibi kalir. */
    fun respawnAtCheckpoint() = respawn()

    /** Odullu reklam: olunen yere en yakin son guvenli zeminde tam canla devam. */
    fun reviveHere() {
        if (!dead) return
        dead = false
        deathTimer = 0f
        hearts = MAX_HEARTS
        iframe = 2.5f
        px = safeX
        py = safeY
        vx = 0f
        vy = 0f
    }

    // ================================================================ Efekt

    private fun burst(x: Float, y: Float, n: Int, color: Long, speed: Float) {
        repeat(n) {
            if (particles.size > 250) return
            val a = random.nextFloat() * 6.283f
            val s = speed * (0.4f + random.nextFloat() * 0.6f)
            particles += FoxParticle(x, y, kotlin.math.cos(a) * s, kotlin.math.sin(a) * s - 2f, 0.5f + random.nextFloat() * 0.3f, color, 0.1f + random.nextFloat() * 0.08f)
        }
    }

    private fun updateParticles(dt: Float) {
        particles.forEach {
            it.x += it.vx * dt
            it.y += it.vy * dt
            it.vy += 12f * dt
            it.life -= dt
        }
        particles.removeAll { it.life <= 0f }
        bumps.entries.removeAll { (_, t) -> t <= 0f }
        bumps.keys.toList().forEach { k -> bumps[k] = (bumps[k] ?: 0f) - dt }
        coinPops.forEach { it[1] -= dt * 3f; it[2] -= dt }
        coinPops.removeAll { it[2] <= 0f }
    }

    fun result() = FoxResult(stars, coins, time, deaths)

    companion object {
        const val PW = 0.7f
        const val PH = 0.85f
        const val MAX_HEARTS = 3
        const val GRAVITY = 50f
        const val JUMP_SPEED = 20.5f
        const val JUMP_CUT = 0.45f
        const val SPRING_SPEED = 26.5f
        const val STOMP_BOUNCE = 13f
        const val RUN_SPEED = 7f
        const val ACCEL = 45f
        const val DECEL = 55f
        const val AIR_ACCEL = 30f
        const val MAX_FALL = 20f
        const val COYOTE = 0.1f
        const val JUMP_BUFFER = 0.12f
        const val HURT_IFRAME = 1.4f
        const val WORLDS = 3
        const val LEVELS_PER_WORLD = 5
    }
}
