package com.eomer91.minigamesworld.ui.games.tower

import androidx.annotation.StringRes
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.ui.games.bubble.Vec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Kulenin savas ozellikleri. Seviye = kalici (altin) + oyun ici (gumus). */
enum class TowerStat(@StringRes val labelRes: Int) {
    DAMAGE(R.string.td_damage),
    ATTACK_SPEED(R.string.td_attack_speed),
    RANGE(R.string.td_range),
    HEALTH(R.string.td_health),
    REGEN(R.string.td_regen),
}

/** Sadece atolyede (altinla) alinan ekonomi yukseltmeleri. */
enum class EconomyPerk(@StringRes val labelRes: Int) {
    SILVER_BONUS(R.string.td_silver_bonus),
    START_SILVER(R.string.td_start_silver),
}

/**
 * Denge degerleri bir Python simulasyonuyla ayarlandi: yukseltmesiz kule ~5. dalgaya,
 * tum ozellikler 10. seviyede ~18. dalgaya ulasir.
 */
object TowerBalance {
    const val MAX_PERMANENT_LEVEL = 20

    fun damage(level: Int) = 6f * (1f + 0.22f * level)
    fun attacksPerSecond(level: Int) = (1.4f * (1f + 0.10f * level)).coerceAtMost(6f)
    fun range(level: Int) = (3.6f + 0.22f * level).coerceAtMost(6.4f)
    fun maxHealth(level: Int) = 50f * (1f + 0.18f * level)
    fun regenPerSecond(level: Int) = 0.4f + 0.35f * level
    fun silverMultiplier(level: Int) = 1f + 0.10f * level
    fun startSilver(level: Int) = 20 * level

    /** Oyun ici (gumus) yukseltme fiyati. */
    fun runCost(runLevel: Int) = (10 * 1.32.pow(runLevel)).roundToInt()

    /** Kalici (altin) yukseltme fiyati. */
    fun permanentCost(level: Int) = (15 * 1.45.pow(level)).roundToInt()
}

enum class EnemyType(val hpFactor: Float, val speed: Float, val damage: Float, val radius: Float, val silver: Int, val points: Int) {
    NORMAL(1f, 0.9f, 5f, 0.30f, 1, 10),
    FAST(0.6f, 1.6f, 3f, 0.25f, 1, 15),
    TANK(3f, 0.55f, 12f, 0.42f, 3, 25),
    BOSS(25f, 0.45f, 40f, 0.70f, 25, 300),
}

data class Enemy(val id: Long, val type: EnemyType, val pos: Vec, val hp: Float, val maxHp: Float)

data class Projectile(val id: Long, val pos: Vec, val targetId: Long, val damage: Float)

/** Kisa omurlu gorsel efekt (olum patlamasi). */
data class Burst(val pos: Vec, val radius: Float, val age: Float = 0f)

data class TowerRun(
    val wave: Int = 1,
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val bursts: List<Burst> = emptyList(),
    /** Bu dalgada henuz cikmamis dusmanlar. */
    val toSpawn: List<EnemyType> = emptyList(),
    val spawnTimer: Float = 0f,
    /** Dalgalar arasi bekleme (saniye). */
    val breakTimer: Float = 0f,
    val cooldown: Float = 0f,
    val health: Float,
    val silver: Int,
    val runLevels: Map<TowerStat, Int> = emptyMap(),
    val score: Int = 0,
    val kills: Int = 0,
    val gameOver: Boolean = false,
    val time: Float = 0f,
)

/** Bir karede olanlar (ses/titresim icin). */
data class FrameEvents(val shots: Int = 0, val kills: Int = 0, val bossKilled: Boolean = false, val hits: Int = 0, val waveCleared: Boolean = false)

/**
 * Tek kule savunmasi kurallari. Saha [FIELD] x [FIELD] birim, kule ortada.
 * Android'den bagimsiz; unit test ile test edilir.
 */
class TowerEngine(
    private val permanent: Map<TowerStat, Int>,
    private val perks: Map<EconomyPerk, Int>,
    private val random: Random = Random.Default,
) {
    private var nextId = 1L
    val center = Vec(FIELD / 2f, FIELD / 2f)

    fun level(run: TowerRun, stat: TowerStat) = (permanent[stat] ?: 0) + (run.runLevels[stat] ?: 0)
    fun damage(run: TowerRun) = TowerBalance.damage(level(run, TowerStat.DAMAGE))
    fun attacksPerSecond(run: TowerRun) = TowerBalance.attacksPerSecond(level(run, TowerStat.ATTACK_SPEED))
    fun range(run: TowerRun) = TowerBalance.range(level(run, TowerStat.RANGE))
    fun maxHealth(run: TowerRun) = TowerBalance.maxHealth(level(run, TowerStat.HEALTH))
    fun regen(run: TowerRun) = TowerBalance.regenPerSecond(level(run, TowerStat.REGEN))
    private val silverMultiplier get() = TowerBalance.silverMultiplier(perks[EconomyPerk.SILVER_BONUS] ?: 0)

    fun newRun(): TowerRun {
        val base = TowerRun(health = 0f, silver = TowerBalance.startSilver(perks[EconomyPerk.START_SILVER] ?: 0))
        return base.copy(health = maxHealth(base), toSpawn = waveComposition(1), breakTimer = 1.5f)
    }

    fun upgradeCost(run: TowerRun, stat: TowerStat) = TowerBalance.runCost(run.runLevels[stat] ?: 0)

    /** Gumusle oyun ici yukseltme. Yetersizse ayni run doner. */
    fun buyRunUpgrade(run: TowerRun, stat: TowerStat): TowerRun {
        val cost = upgradeCost(run, stat)
        if (run.gameOver || run.silver < cost) return run
        val oldMax = maxHealth(run)
        val upgraded = run.copy(silver = run.silver - cost, runLevels = run.runLevels + (stat to (run.runLevels[stat] ?: 0) + 1))
        // Can yukseltmesi: artan kadar can da eklenir.
        return if (stat == TowerStat.HEALTH) upgraded.copy(health = upgraded.health + (maxHealth(upgraded) - oldMax)) else upgraded
    }

    /** Odullu reklamla devam: yari can, sahadaki dusmanlar temizlenir. */
    fun revive(run: TowerRun): TowerRun =
        run.copy(gameOver = false, health = maxHealth(run) * 0.5f, enemies = emptyList(), projectiles = emptyList())

    fun waveComposition(wave: Int): List<EnemyType> {
        val list = ArrayList<EnemyType>()
        val count = 6 + wave * 2
        repeat(count) {
            val roll = random.nextFloat()
            list += when {
                wave >= 5 && roll < 0.15f + wave * 0.005f -> EnemyType.TANK
                wave >= 3 && roll < 0.40f -> EnemyType.FAST
                else -> EnemyType.NORMAL
            }
        }
        if (wave % 10 == 0) list += EnemyType.BOSS
        return list.shuffled(random)
    }

    fun enemyHp(type: EnemyType, wave: Int) = 6f * 1.13f.pow(wave - 1) * type.hpFactor

    fun update(run: TowerRun, dt: Float): Pair<TowerRun, FrameEvents> {
        val bursts = run.bursts.map { it.copy(age = it.age + dt) }.filter { it.age < BURST_LIFETIME }
        if (run.gameOver) return run.copy(bursts = bursts) to FrameEvents()

        var s = run.copy(bursts = bursts, time = run.time + dt)
        var shots = 0
        var kills = 0
        var hits = 0
        var bossKilled = false
        var waveCleared = false

        // 1) Dalga arasi / dogma
        if (s.breakTimer > 0f) {
            s = s.copy(breakTimer = (s.breakTimer - dt).coerceAtLeast(0f))
        } else if (s.toSpawn.isNotEmpty()) {
            var timer = s.spawnTimer - dt
            var toSpawn = s.toSpawn
            val spawned = ArrayList<Enemy>()
            while (timer <= 0f && toSpawn.isNotEmpty()) {
                val type = toSpawn.first()
                toSpawn = toSpawn.drop(1)
                val a = random.nextFloat() * 2f * PI.toFloat()
                val pos = center + Vec(cos(a), sin(a)) * SPAWN_RADIUS
                val hp = enemyHp(type, s.wave)
                spawned += Enemy(nextId++, type, pos, hp, hp)
                timer += spawnInterval(s.wave)
            }
            s = s.copy(enemies = s.enemies + spawned, toSpawn = toSpawn, spawnTimer = timer)
        }

        // 2) Dusmanlar kuleye yurur; degenler hasar verip yok olur
        var health = s.health
        val walking = ArrayList<Enemy>()
        for (e in s.enemies) {
            val toTower = center - e.pos
            val dist = e.pos.dist(center)
            if (dist <= TOWER_RADIUS + e.type.radius) {
                health -= e.type.damage
                hits++
                continue
            }
            val step = (e.type.speed * dt).coerceAtMost(dist)
            walking += e.copy(pos = e.pos + toTower * (step / dist))
        }

        // 3) Kule ates eder (menzildeki en yakin hedefe)
        var cooldown = s.cooldown - dt
        val projectiles = s.projectiles.toMutableList()
        val range = range(s)
        if (cooldown <= 0f) {
            val target = walking.filter { it.pos.dist(center) <= range + it.type.radius }.minByOrNull { it.pos.dist(center) }
            if (target != null) {
                projectiles += Projectile(nextId++, center, target.id, damage(s))
                shots++
                cooldown = 1f / attacksPerSecond(s)
            } else {
                cooldown = 0f
            }
        }

        // 4) Mermiler hedefe gider
        val enemies = walking.associateBy { it.id }.toMutableMap()
        val flying = ArrayList<Projectile>()
        val newBursts = ArrayList<Burst>()
        var silver = s.silver
        var score = s.score
        for (p in projectiles) {
            val target = enemies[p.targetId] ?: continue
            val d = p.pos.dist(target.pos)
            val step = PROJECTILE_SPEED * dt
            if (d <= step + target.type.radius * 0.5f) {
                val hp = target.hp - p.damage
                if (hp <= 0f) {
                    enemies.remove(target.id)
                    kills++
                    if (target.type == EnemyType.BOSS) bossKilled = true
                    silver += (target.type.silver * (1 + s.wave / 8) * silverMultiplier).roundToInt().coerceAtLeast(1)
                    score += target.type.points
                    newBursts += Burst(target.pos, target.type.radius)
                } else {
                    enemies[target.id] = target.copy(hp = hp)
                }
            } else {
                flying += p.copy(pos = p.pos + (target.pos - p.pos) * (step / d))
            }
        }

        // 5) Can yenileme
        health = (health + regen(s) * dt).coerceAtMost(maxHealth(s))
        var next = s.copy(
            enemies = enemies.values.toList(),
            projectiles = flying,
            bursts = s.bursts + newBursts,
            cooldown = cooldown,
            health = health,
            silver = silver,
            score = score,
            kills = s.kills + kills,
        )

        // 6) Dalga bitti mi?
        if (next.toSpawn.isEmpty() && next.enemies.isEmpty() && next.breakTimer <= 0f) {
            waveCleared = true
            val wave = next.wave + 1
            next = next.copy(
                wave = wave,
                toSpawn = waveComposition(wave),
                spawnTimer = 0f,
                breakTimer = WAVE_BREAK,
                silver = next.silver + ((5 + next.wave) * silverMultiplier).roundToInt(),
                score = next.score + 50 * next.wave,
            )
        }

        if (next.health <= 0f) next = next.copy(health = 0f, gameOver = true)
        return next to FrameEvents(shots, kills, bossKilled, hits, waveCleared)
    }

    private fun spawnInterval(wave: Int) = (0.9f - wave * 0.02f).coerceAtLeast(0.3f)

    companion object {
        const val FIELD = 14f
        const val TOWER_RADIUS = 0.6f
        const val SPAWN_RADIUS = 7.6f
        const val PROJECTILE_SPEED = 14f
        const val WAVE_BREAK = 3f
        const val BURST_LIFETIME = 0.4f
    }
}
