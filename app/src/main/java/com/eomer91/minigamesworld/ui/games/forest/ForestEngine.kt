package com.eomer91.minigamesworld.ui.games.forest

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Orman Muhafizi simulasyonu. Yuzlerce dusman oldugu icin (60 fps'te kopyalama pahali) siniflar
 * degistirilebilir (mutable); ViewModel her karede update() cagirir, ekran dogrudan bu nesneleri cizer.
 * Android'den bagimsizdir, unit test ile test edilir. Koordinatlar "birim" (1 birim = 1 piksel-art karo).
 */

class Enemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var hp: Float,
    val maxHp: Float,
    val damage: Float,
) {
    var alive = true
    var flash = 0f
    var slow = 0f
    /** Mutlak Sifir: tamamen donmus (hareket etmez). */
    var frozen = 0f
    var kx = 0f
    var ky = 0f
    var bladeCd = 0f
    var facingLeft = false
    // Boss
    var attackTimer = 3f
    var chargeTimer = 6f
    var charging = 0f
}

class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val radius: Float,
    var pierce: Int,
    var life: Float,
    val kind: ProjectileKind,
) {
    val hit = HashSet<Int>(4)
}

enum class ProjectileKind { BOLT, LANCE, ARROW, SEED }

class Pickup(var x: Float, var y: Float, val kind: PickupKind, val value: Int) {
    var attracted = false
    var alive = true
}

enum class PickupKind { GEM, BIG_GEM, MEAT, CHEST }

/** Gorsel parcacik (olum, vurus kivilcimi). */
class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val maxLife: Float, val color: Long, val size: Float)

class FloatText(val x: Float, var y: Float, val value: Int, val crit: Boolean, var life: Float = 0.7f)
class Strike(val x: Float, val y: Float, var life: Float = 0.25f)
class Nova(val x: Float, val y: Float, val maxRadius: Float, var life: Float = 0.45f) {
    val hit = HashSet<Int>()
}

/** Seviye atlamada sunulan secenek. */
sealed interface Upgrade {
    data class Weapon(val type: WeaponType, val newLevel: Int) : Upgrade
    data class Passive(val type: PassiveType, val newLevel: Int) : Upgrade
    data object Heal : Upgrade
    /** Silah evrimi: silah 5. seviye + eslesen pasif. */
    data class Evolve(val type: WeaponType) : Upgrade
}

/** Bir karedeki olaylar (ses ve ekran icin). */
class FrameEvents {
    var kills = 0
    var heroHit = false
    var pickedGems = 0
    var bossSpawned = false
    var skillUsed = false
    var evolved = false
    fun clear() {
        kills = 0; heroHit = false; pickedGems = 0; bossSpawned = false; skillUsed = false; evolved = false
    }
}

class ForestEngine(
    val map: ForestMap,
    val stats: HeroStats,
    private val random: Random = Random.Default,
) {
    // ---------------- Kahraman
    var heroX = 0f
    var heroY = 0f
    var hp = stats.maxHp
    var mp = 0f
    var maxHp = stats.maxHp
    var facingX = 1f
    var facingY = 0f
    var moving = false
    var iframe = 0f
    var dashTime = 0f
    var dashCd = 0f
    var level = 1
    var xp = 0f
    var pendingLevelUps = 0
    var kills = 0
    var time = 0f
    var dead = false
    var victory = false
    var reviveUsed = false
    var bossSpawned = false
    var boss: Enemy? = null

    val weapons = LinkedHashMap<WeaponType, Int>().apply { put(WeaponType.BOLT, 1) }
    val passives = LinkedHashMap<PassiveType, Int>()
    val evolved = HashSet<WeaponType>()
    var bossSpawnTime = -1f
    var bossKillTime = -1f
    private val cooldowns = HashMap<WeaponType, Float>()
    var bladeAngle = 0f
        private set
    private var thornTimer = 0f

    // ---------------- Dunya
    val enemies = ArrayList<Enemy>()
    val projectiles = ArrayList<Projectile>()
    val enemyShots = ArrayList<Projectile>()
    val pickups = ArrayList<Pickup>()
    val texts = ArrayList<FloatText>()
    val strikes = ArrayList<Strike>()
    val novas = ArrayList<Nova>()
    val particles = ArrayList<Particle>()
    var skillFlash = 0f
    /** Kahraman vurulunca kisa ekran sarsintisi. */
    var shake = 0f
    private var nextId = 1
    private var spawnAcc = 0f
    private var swarmTimer = SWARM_EVERY

    /** Gorunen alan (birim); ekran boyutuna gore ViewModel ayarlar. */
    var viewW = 22f
    var viewH = 11f

    val events = FrameEvents()

    // ---------------- Turetilen degerler
    private fun pLevel(p: PassiveType) = passives[p] ?: 0
    val moveSpeed: Float get() = stats.moveSpeed * (1f + 0.08f * pLevel(PassiveType.SWIFT))
    val pickupRadius: Float get() = 1.3f * (1f + 0.4f * pLevel(PassiveType.MAGNET))
    private val damageMult: Float get() = 1f + 0.1f * pLevel(PassiveType.MIGHT) + stats.attack / 100f
    private val cooldownMult: Float get() = (1f - 0.08f * pLevel(PassiveType.HASTE)) / (1f + stats.attackSpeed / 100f)
    private val regen: Float get() = 0.4f * pLevel(PassiveType.VITALITY)
    val xpNeeded: Float get() = 5f + 6f * (level - 1) + (level - 1) * (level - 1).toFloat()
    val maxMp: Float get() = stats.maxMp
    val canDash: Boolean get() = dashCd <= 0f
    val canSkill: Boolean get() = mp >= SKILL_COST

    // ---------------- Silah degerleri (seviye 1..5)
    fun boltCount(l: Int) = 1 + (if (l >= 3) 1 else 0) + (if (l >= 5) 1 else 0)
    fun bladeCount(l: Int) = l + 1
    val bladeRadius = 1.7f
    fun thornRadius(l: Int) = 1.4f + 0.25f * l
    fun frostRadius(l: Int) = 3f + 0.5f * l

    // =================================================================== Guncelleme

    /** [inputX], [inputY]: joystick (-1..1). */
    fun update(dt: Float, inputX: Float, inputY: Float) {
        events.clear()
        if (dead || victory || pendingLevelUps > 0) return
        time += dt
        moveHero(dt, inputX, inputY)
        spawn(dt)
        updateWeapons(dt)
        updateProjectiles(dt)
        updateEnemies(dt)
        updateEnemyShots(dt)
        updatePickups(dt)
        updateEffects(dt)
        hp = min(maxHp, hp + regen * dt)
        mp = min(maxMp, mp + MP_REGEN * dt)
        if (hp <= 0f) {
            hp = 0f
            dead = true
        }
    }

    private fun moveHero(dt: Float, ix: Float, iy: Float) {
        val len = hypot(ix, iy)
        moving = len > 0.15f
        if (moving) {
            facingX = ix / len
            facingY = iy / len
        }
        iframe = max(0f, iframe - dt)
        dashCd = max(0f, dashCd - dt)
        var speed = moveSpeed * min(1f, len)
        var dx = ix
        var dy = iy
        if (dashTime > 0f) {
            dashTime -= dt
            speed = moveSpeed * DASH_SPEED_MULT
            dx = facingX
            dy = facingY
        }
        val l = hypot(dx, dy)
        if (l > 0.01f) {
            heroX += dx / l * speed * dt
            heroY += dy / l * speed * dt
        }
    }

    fun dash(): Boolean {
        if (!canDash || dead) return false
        dashTime = DASH_TIME
        dashCd = DASH_COOLDOWN
        iframe = max(iframe, DASH_TIME + 0.15f)
        return true
    }

    /** Ozel yetenek (MP): ekrandaki tum dusmanlara buyuk hasar ve geri itme. */
    fun useSkill(): Boolean {
        if (!canSkill || dead) return false
        mp -= SKILL_COST
        skillFlash = 0.5f
        events.skillUsed = true
        val dmg = SKILL_DAMAGE * damageMult
        enemies.forEach { e ->
            if (abs(e.x - heroX) < viewW / 2f + 1 && abs(e.y - heroY) < viewH / 2f + 1) {
                hitEnemy(e, if (e.type.isBoss) dmg * 0.5f else dmg, knock = 1.5f)
            }
        }
        enemyShots.clear()
        return true
    }

    // ---------------- Dogma

    private fun spawn(dt: Float) {
        // Boss: 4:30'da
        if (!bossSpawned && time >= BOSS_TIME) {
            bossSpawned = true
            events.bossSpawned = true
            boss = spawnEnemy(EnemyType.TREANT)
            bossSpawnTime = time
        }
        val minutes = time / 60f
        val rate = (0.8f + minutes * 1.1f) * map.spawnMult * (if (bossSpawned) 0.5f else 1f)
        spawnAcc += rate * dt
        while (spawnAcc >= 1f) {
            spawnAcc -= 1f
            if (enemies.size < MAX_ENEMIES) spawnEnemy(pickType())
        }
        swarmTimer -= dt
        if (swarmTimer <= 0f) {
            swarmTimer = SWARM_EVERY
            // Yarasa halkasi
            val n = 14 + (time / 60f).toInt() * 3
            val r = hypot(viewW, viewH) / 2f + 1f
            repeat(n) { i ->
                if (enemies.size < MAX_ENEMIES + 30) {
                    val a = i * 2f * PI.toFloat() / n
                    spawnEnemy(EnemyType.BAT, heroX + cos(a) * r, heroY + sin(a) * r)
                }
            }
        }
    }

    private fun pickType(): EnemyType {
        // Zamana gore acilan dusmanlar
        val w = map.weights
        val allowed = listOf(
            EnemyType.GOBLIN to w[0],
            EnemyType.BAT to w[1],
            EnemyType.WOLF to if (time > 50f) w[2] else 0f,
            EnemyType.ORC to if (time > 120f) w[3] else 0f,
        )
        val total = allowed.sumOf { it.second.toDouble() }.toFloat()
        var roll = random.nextFloat() * total
        for ((t, wt) in allowed) {
            roll -= wt
            if (roll <= 0f) return t
        }
        return EnemyType.GOBLIN
    }

    fun spawnEnemy(type: EnemyType, x: Float? = null, y: Float? = null): Enemy {
        val px: Float
        val py: Float
        if (x != null && y != null) {
            px = x; py = y
        } else {
            // Gorunen alanin hemen disinda rastgele bir nokta
            val a = random.nextFloat() * 2f * PI.toFloat()
            val r = hypot(viewW, viewH) / 2f + 1f + random.nextFloat()
            px = heroX + cos(a) * r
            py = heroY + sin(a) * r
        }
        val timeMult = 1f + time / 300f
        val hp = type.hp * map.hpMult * (if (type.isBoss) 1f else timeMult)
        // Hasar da zamanla artar (5. dakikada +%50)
        val dmg = type.damage * map.damageMult * (1f + time / 600f)
        val e = Enemy(nextId++, type, px, py, hp, hp, dmg)
        enemies += e
        return e
    }

    // ---------------- Silahlar

    private fun cooldown(w: WeaponType, base: Float, dt: Float): Boolean {
        val c = (cooldowns[w] ?: 0f) - dt
        return if (c <= 0f) {
            cooldowns[w] = base * cooldownMult
            true
        } else {
            cooldowns[w] = c
            false
        }
    }

    private fun updateWeapons(dt: Float) {
        bladeAngle += dt * 3.2f
        weapons.forEach { (w, l) ->
            val evo = w in evolved
            when (w) {
                WeaponType.BOLT -> if (cooldown(w, if (evo) 0.45f else 0.95f - 0.08f * l, dt)) fireBolts(l, evo)
                WeaponType.BLADES -> updateBlades(l, dt, evo)
                WeaponType.LIGHTNING -> if (cooldown(w, if (evo) 1.0f else 2.2f - 0.2f * l, dt)) lightning(l, evo)
                WeaponType.THORNS -> updateThorns(l, dt, evo)
                WeaponType.ARROWS -> if (cooldown(w, if (evo) 0.9f else 1.5f - 0.12f * l, dt)) fireArrows(l, evo)
                WeaponType.FROST -> if (cooldown(w, if (evo) 3.0f else 3.8f - 0.25f * l, dt)) novas += Nova(heroX, heroY, frostRadius(l) + if (evo) 2.5f else 0f)
            }
        }
    }

    private fun nearestEnemies(n: Int, range: Float): List<Enemy> =
        enemies.asSequence()
            .filter { it.alive && dist2(it.x, it.y) < range * range }
            .sortedBy { dist2(it.x, it.y) }
            .take(n)
            .toList()

    private fun dist2(x: Float, y: Float): Float {
        val dx = x - heroX
        val dy = y - heroY
        return dx * dx + dy * dy
    }

    private fun fireBolts(l: Int, evo: Boolean = false) {
        val targets = nearestEnemies(if (evo) 5 else boltCount(l), 10f)
        targets.forEach { t ->
            val a = atan2(t.y - heroY, t.x - heroX)
            projectiles += if (evo) {
                // Ruhani Mizrak: buyuk, her seyi delip gecer
                Projectile(heroX, heroY, cos(a) * 12f, sin(a) * 12f, (14f + 5f * (l - 1)) * 1.8f, 0.4f, 99, 1.2f, ProjectileKind.LANCE)
            } else {
                Projectile(heroX, heroY, cos(a) * 9f, sin(a) * 9f, 14f + 5f * (l - 1), 0.25f, if (l >= 4) 2 else 1, 1.4f, ProjectileKind.BOLT)
            }
        }
    }

    private fun fireArrows(l: Int, evo: Boolean = false) {
        if (evo) {
            // Ruzgar Oklari: her yone 12 ok
            val n = 12
            repeat(n) { i ->
                val a = i * 2f * PI.toFloat() / n + time
                projectiles += Projectile(heroX, heroY, cos(a) * 12f, sin(a) * 12f, (7f + 3f * l) * 1.4f, 0.22f, 4, 0.9f, ProjectileKind.ARROW)
            }
            return
        }
        val n = 2 + l
        val base = atan2(facingY, facingX)
        val spread = 0.9f
        repeat(n) { i ->
            val a = base - spread / 2f + spread * i / (n - 1)
            projectiles += Projectile(heroX, heroY, cos(a) * 11f, sin(a) * 11f, 7f + 3f * l, 0.2f, 2, 0.9f, ProjectileKind.ARROW)
        }
    }

    private fun lightning(l: Int, evo: Boolean = false) {
        val candidates = enemies.filter { it.alive && dist2(it.x, it.y) < 81f }
        val area = if (evo) 1.5f else 0.9f
        candidates.shuffled(random).take(if (evo) 8 else l).forEach { t ->
            strikes += Strike(t.x, t.y)
            val dmg = (22f + 9f * l) * (if (evo) 1.5f else 1f)
            enemies.forEach { e ->
                if (e.alive && hypot(e.x - t.x, e.y - t.y) < area) hitEnemy(e, dmg)
            }
        }
    }

    /** Donen bicak konumlari (cizim icin de kullanilir). */
    fun bladePositions(): List<Pair<Float, Float>> {
        val l = weapons[WeaponType.BLADES] ?: return emptyList()
        val evo = WeaponType.BLADES in evolved
        val n = if (evo) 8 else bladeCount(l)
        // Kasirga: yaricap nefes alir gibi buyuyup kuculur
        val r = if (evo) bladeRadius + 0.9f + sin(time * 2.2f) * 0.8f else bladeRadius
        val angle = if (evo) bladeAngle * 1.5f else bladeAngle
        return List(n) { i ->
            val a = angle + i * 2f * PI.toFloat() / n
            (heroX + cos(a) * r) to (heroY + sin(a) * r)
        }
    }

    private fun updateBlades(l: Int, dt: Float, evo: Boolean = false) {
        val blades = bladePositions()
        enemies.forEach { e ->
            if (!e.alive) return@forEach
            e.bladeCd = max(0f, e.bladeCd - dt)
            if (e.bladeCd > 0f) return@forEach
            if (blades.any { (bx, by) -> hypot(e.x - bx, e.y - by) < e.type.radius + 0.35f }) {
                e.bladeCd = if (evo) 0.35f else 0.5f
                hitEnemy(e, (8f + 3f * l) * (if (evo) 1.6f else 1f), knock = 0.4f)
            }
        }
    }

    private fun updateThorns(l: Int, dt: Float, evo: Boolean = false) {
        thornTimer -= dt
        if (thornTimer > 0f) return
        thornTimer = 0.5f
        val r = thornRadius(l) + if (evo) 1f else 0f
        var healed = 0
        enemies.forEach { e ->
            if (e.alive && dist2(e.x, e.y) < (r + e.type.radius) * (r + e.type.radius)) {
                hitEnemy(e, (4f + 2f * l) * (if (evo) 1.5f else 1f), showText = false)
                // Yasam Sarmasigi: her vurusta biraz can
                if (evo && healed < 4) {
                    healed++
                    hp = min(maxHp, hp + 0.6f)
                }
            }
        }
    }

    // ---------------- Mermiler

    private fun updateProjectiles(dt: Float) {
        projectiles.forEach { p ->
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            for (e in enemies) {
                if (!e.alive || e.id in p.hit) continue
                if (hypot(e.x - p.x, e.y - p.y) < e.type.radius + p.radius) {
                    p.hit += e.id
                    hitEnemy(e, p.damage, knock = 0.25f)
                    p.pierce--
                    if (p.pierce <= 0) {
                        p.life = 0f
                        break
                    }
                }
            }
        }
        projectiles.removeAll { it.life <= 0f }
    }

    private fun updateEnemyShots(dt: Float) {
        enemyShots.forEach { s ->
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.life -= dt
            if (hypot(s.x - heroX, s.y - heroY) < s.radius + HERO_RADIUS) {
                s.life = 0f
                damageHero(s.damage)
            }
        }
        enemyShots.removeAll { it.life <= 0f }
    }

    /** Hasar uygula (kritik dahil). */
    fun hitEnemy(e: Enemy, base: Float, knock: Float = 0f, showText: Boolean = true) {
        if (!e.alive) return
        val crit = random.nextFloat() * 100f < stats.critChance
        val dmg = base * damageMult * (if (crit) stats.critDamage / 100f else 1f)
        e.hp -= dmg
        e.flash = 0.12f
        if (knock > 0f && !e.type.isBoss) {
            val a = atan2(e.y - heroY, e.x - heroX)
            e.kx += cos(a) * knock * 6f
            e.ky += sin(a) * knock * 6f
        }
        if (showText && texts.size < MAX_TEXTS) texts += FloatText(e.x, e.y - e.type.drawSize * 0.5f, dmg.toInt(), crit)
        if (particles.size < MAX_PARTICLES) burst(e.x, e.y - e.type.drawSize * 0.3f, if (crit) 4 else 2, 0xFFFFF3B0, 3f, 0.25f, 0.08f)
        if (e.hp <= 0f) kill(e)
    }

    /** Parcacik patlamasi. */
    fun burst(x: Float, y: Float, n: Int, color: Long, speed: Float, life: Float, size: Float) {
        repeat(n) {
            if (particles.size >= MAX_PARTICLES) return
            val a = random.nextFloat() * 2f * PI.toFloat()
            val sp = speed * (0.4f + random.nextFloat() * 0.6f)
            val l = life * (0.6f + random.nextFloat() * 0.4f)
            particles += Particle(x, y, cos(a) * sp, sin(a) * sp, l, l, color, size * (0.7f + random.nextFloat() * 0.6f))
        }
    }

    private fun kill(e: Enemy) {
        e.alive = false
        val color = when (e.type) {
            EnemyType.GOBLIN -> 0xFF5FAE6E
            EnemyType.BAT -> 0xFFA66EE0
            EnemyType.WOLF -> 0xFFA3A9B3
            EnemyType.ORC -> 0xFF4E8270
            EnemyType.TREANT -> 0xFF8FE36B
        }
        burst(e.x, e.y - e.type.drawSize * 0.3f, if (e.type.isBoss) 40 else 7, color, if (e.type.isBoss) 6f else 3.5f, 0.5f, 0.14f)
        burst(e.x, e.y - e.type.drawSize * 0.3f, 3, 0xFFFFFFFF, 2f, 0.35f, 0.1f)
        kills++
        events.kills++
        mp = min(maxMp, mp + MP_PER_KILL)
        // Dusurulenler
        if (pickups.size < MAX_PICKUPS) {
            pickups += Pickup(e.x, e.y, if (e.type.xp >= 5) PickupKind.BIG_GEM else PickupKind.GEM, e.type.xp)
        } else {
            // Cok kristal varsa dogrudan ver (performans)
            addXp(e.type.xp.toFloat())
        }
        when {
            e.type.isBoss -> {
                pickups += Pickup(e.x, e.y, PickupKind.CHEST, 0)
                boss = null
                bossKillTime = time
            }
            random.nextFloat() < MEAT_CHANCE -> pickups += Pickup(e.x, e.y + 0.2f, PickupKind.MEAT, 30)
        }
    }

    private fun addXp(v: Float) {
        xp += v * xpMult
        while (xp >= xpNeeded) {
            xp -= xpNeeded
            level++
            pendingLevelUps++
        }
    }

    private val xpMult: Float get() = 1f + 0.1f * pLevel(PassiveType.WISDOM)

    // ---------------- Dusmanlar

    private fun updateEnemies(dt: Float) {
        for (e in enemies) {
            if (!e.alive) continue
            e.flash = max(0f, e.flash - dt)
            e.slow = max(0f, e.slow - dt)
            e.frozen = max(0f, e.frozen - dt)
            var speed = e.type.speed * (if (e.slow > 0f) 0.5f else 1f) * (if (e.frozen > 0f) 0f else 1f)
            if (e.type.isBoss) speed = updateBoss(e, dt, speed)
            val dx = heroX - e.x
            val dy = heroY - e.y
            val d = hypot(dx, dy)
            if (d > 0.01f) {
                e.x += dx / d * speed * dt
                e.y += dy / d * speed * dt
                e.facingLeft = dx < 0f
            }
            // Geri itme sonumlenir
            e.x += e.kx * dt
            e.y += e.ky * dt
            e.kx *= 0.85f
            e.ky *= 0.85f
            // Kahramana temas
            if (d < e.type.radius + HERO_RADIUS) damageHero(e.damage)
            // Cok uzaklastiysa karsi tarafa tasi (oyuncunun etrafinda kalsin)
            val far = hypot(viewW, viewH)
            if (!e.type.isBoss && d > far) {
                e.x = heroX - dx * 0.9f
                e.y = heroY - dy * 0.9f
            }
        }
        separate()
        enemies.removeAll { !it.alive }
    }

    private fun updateBoss(e: Enemy, dt: Float, speed: Float): Float {
        e.attackTimer -= dt
        e.chargeTimer -= dt
        if (e.attackTimer <= 0f) {
            // Tohum halkasi
            e.attackTimer = 3.5f
            val n = 14
            repeat(n) { i ->
                val a = i * 2f * PI.toFloat() / n + time
                enemyShots += Projectile(e.x, e.y, cos(a) * 4f, sin(a) * 4f, e.damage * 0.6f, 0.25f, 1, 4f, ProjectileKind.SEED)
            }
        }
        if (e.chargeTimer <= 0f) {
            e.chargeTimer = 7f
            e.charging = 1f
        }
        if (e.charging > 0f) {
            e.charging -= dt
            return speed * 3.2f
        }
        return speed
    }

    /** Dusmanlarin ust uste binmesini engelle (basit itme). */
    private fun separate() {
        val n = enemies.size
        for (i in 0 until n) {
            val a = enemies[i]
            if (!a.alive) continue
            for (j in i + 1 until n) {
                val b = enemies[j]
                if (!b.alive) continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val minD = (a.type.radius + b.type.radius) * 0.9f
                val d2 = dx * dx + dy * dy
                if (d2 < minD * minD && d2 > 1e-6f) {
                    val d = sqrt(d2)
                    val push = (minD - d) * 0.5f
                    val ux = dx / d
                    val uy = dy / d
                    val wa = if (a.type.isBoss) 0f else 1f
                    val wb = if (b.type.isBoss) 0f else 1f
                    a.x -= ux * push * wa
                    a.y -= uy * push * wa
                    b.x += ux * push * wb
                    b.y += uy * push * wb
                }
            }
        }
    }

    private fun damageHero(raw: Float) {
        if (iframe > 0f || dead) return
        val dmg = raw * 100f / (100f + stats.defence)
        hp -= dmg
        iframe = HIT_IFRAME
        shake = 0.2f
        events.heroHit = true
        burst(heroX, heroY - 0.4f, 6, 0xFFE04848, 3f, 0.35f, 0.1f)
    }

    // ---------------- Toplanabilirler

    private fun updatePickups(dt: Float) {
        val r = pickupRadius
        pickups.forEach { p ->
            val dx = heroX - p.x
            val dy = heroY - p.y
            val d = hypot(dx, dy)
            if (d < r || p.attracted) {
                p.attracted = true
                val sp = 9f
                if (d > 0.01f) {
                    p.x += dx / d * min(d, sp * dt)
                    p.y += dy / d * min(d, sp * dt)
                }
            }
            if (d < 0.45f) collect(p)
        }
        pickups.removeAll { !it.alive }
    }

    private fun collect(p: Pickup) {
        p.alive = false
        when (p.kind) {
            PickupKind.GEM, PickupKind.BIG_GEM -> {
                addXp(p.value.toFloat())
                events.pickedGems++
            }
            PickupKind.MEAT -> hp = min(maxHp, hp + p.value)
            PickupKind.CHEST -> victory = true
        }
    }

    /** Ekrandaki tum kristalleri cek (seviye atlama secimi sonrasi kucuk odul degil; miknatis). */
    fun attractAll() = pickups.forEach { if (it.kind == PickupKind.GEM || it.kind == PickupKind.BIG_GEM) it.attracted = true }

    // ---------------- Efektler

    private fun updateEffects(dt: Float) {
        shake = max(0f, shake - dt)
        particles.forEach { p ->
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vx *= 0.9f
            p.vy *= 0.9f
            p.life -= dt
        }
        particles.removeAll { it.life <= 0f }
        texts.forEach { it.life -= dt; it.y -= dt * 1.2f }
        texts.removeAll { it.life <= 0f }
        strikes.forEach { it.life -= dt }
        strikes.removeAll { it.life <= 0f }
        skillFlash = max(0f, skillFlash - dt)
        novas.forEach { n ->
            n.life -= dt
            val r = n.maxRadius * (1f - n.life / 0.45f)
            val l = weapons[WeaponType.FROST] ?: 1
            enemies.forEach { e ->
                if (e.alive && e.id !in n.hit && hypot(e.x - n.x, e.y - n.y) < r + e.type.radius) {
                    n.hit += e.id
                    e.slow = 2f
                    val evo = WeaponType.FROST in evolved
                    if (evo && !e.type.isBoss) e.frozen = 1.5f
                    hitEnemy(e, (12f + 5f * l) * (if (evo) 1.5f else 1f))
                }
            }
        }
        novas.removeAll { it.life <= 0f }
    }

    // =================================================================== Seviye atlama

    /** Evrim kosulu saglanan silahlar. */
    fun evolvable(): List<WeaponType> = weapons.filter { (w, l) ->
        l >= MAX_LEVEL && w !in evolved && (passives[evolutionPassive(w)] ?: 0) > 0
    }.keys.toList()

    fun rollChoices(): List<Upgrade> {
        // Evrim hazirsa her zaman ilk secenek olarak sunulur
        val evo = evolvable().firstOrNull()
        if (evo != null) {
            val rest = rollBasic().filter { it !is Upgrade.Heal }.take(2)
            return listOf(Upgrade.Evolve(evo)) + rest
        }
        return rollBasic()
    }

    private fun rollBasic(): List<Upgrade> {
        val options = ArrayList<Upgrade>()
        weapons.forEach { (w, l) -> if (l < MAX_LEVEL) options += Upgrade.Weapon(w, l + 1) }
        if (weapons.size < MAX_WEAPONS) WeaponType.entries.filter { it !in weapons }.forEach { options += Upgrade.Weapon(it, 1) }
        passives.forEach { (p, l) -> if (l < MAX_LEVEL) options += Upgrade.Passive(p, l + 1) }
        if (passives.size < MAX_PASSIVES) PassiveType.entries.filter { it !in passives }.forEach { options += Upgrade.Passive(it, 1) }
        if (options.isEmpty()) return listOf(Upgrade.Heal)
        return options.shuffled(random).take(3)
    }

    fun apply(u: Upgrade) {
        when (u) {
            is Upgrade.Weapon -> weapons[u.type] = u.newLevel
            is Upgrade.Passive -> {
                passives[u.type] = u.newLevel
                if (u.type == PassiveType.VITALITY) {
                    maxHp += 20f
                    hp += 20f
                }
            }
            Upgrade.Heal -> hp = maxHp
            is Upgrade.Evolve -> {
                evolved += u.type
                events.evolved = true
                burst(heroX, heroY - 0.5f, 30, 0xFFF7D046, 5f, 0.6f, 0.12f)
            }
        }
        pendingLevelUps = max(0, pendingLevelUps - 1)
    }

    /** Odullu reklam: tam canla devam, yakindaki dusmanlar geri itilir. */
    fun revive() {
        if (!dead || reviveUsed) return
        reviveUsed = true
        dead = false
        hp = maxHp
        iframe = 2.5f
        enemies.forEach { e ->
            if (!e.type.isBoss && dist2(e.x, e.y) < 16f) e.alive = false
        }
        enemies.removeAll { !it.alive }
        enemyShots.clear()
    }

    /** Skor: oldurme + sure + harita bonusu. */
    val score: Int
        get() = kills * 5 + time.toInt() * 2 + (if (victory) 1500 * (map.ordinal + 1) else 0)

    private fun abs(v: Float) = kotlin.math.abs(v)

    companion object {
        /** Her silahin evrimi icin gereken pasif. */
        fun evolutionPassive(w: WeaponType): PassiveType = when (w) {
            WeaponType.BOLT -> PassiveType.MIGHT
            WeaponType.BLADES -> PassiveType.HASTE
            WeaponType.LIGHTNING -> PassiveType.WISDOM
            WeaponType.THORNS -> PassiveType.VITALITY
            WeaponType.ARROWS -> PassiveType.SWIFT
            WeaponType.FROST -> PassiveType.MAGNET
        }

        const val HERO_RADIUS = 0.35f
        const val MAP_SECONDS = 300f
        const val BOSS_TIME = 270f
        const val MAX_ENEMIES = 160
        const val MAX_PICKUPS = 350
        const val MAX_TEXTS = 60
        const val MAX_PARTICLES = 400
        const val SWARM_EVERY = 60f
        const val MAX_LEVEL = 5
        const val MAX_WEAPONS = 6
        const val MAX_PASSIVES = 6
        const val HIT_IFRAME = 0.35f
        const val DASH_TIME = 0.22f
        const val DASH_SPEED_MULT = 3.6f
        const val DASH_COOLDOWN = 2.5f
        const val SKILL_COST = 100f
        const val SKILL_DAMAGE = 80f
        const val MP_REGEN = 1.5f
        const val MP_PER_KILL = 0.6f
        const val MEAT_CHANCE = 0.012f
    }
}
