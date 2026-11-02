package com.eomer91.minigamesworld.ui.games.fish

import kotlin.math.abs
import kotlin.random.Random

/** Sudaki bir canli: balik ya da ([species] null ise) denizanasi. */
data class SeaCreature(
    val id: Long,
    val species: FishSpecies?,
    val x: Float,
    val depth: Float,
    val dir: Int,
    val caught: Boolean = false,
) {
    val isJelly: Boolean get() = species == null
    val radius: Float get() = species?.size?.times(0.5f) ?: 0.45f
}

enum class CastPhase { READY, DESCENDING, ASCENDING, DONE }

data class FishingCast(
    val phase: CastPhase = CastPhase.READY,
    val hookX: Float = FishingEngine.WIDTH / 2f,
    /** Parmagin hedefledigi x; kanca yumusakca takip eder. */
    val targetX: Float = FishingEngine.WIDTH / 2f,
    val depth: Float = 0f,
    val maxDepth: Float,
    val capacity: Int,
    val speedFactor: Float,
    val creatures: List<SeaCreature> = emptyList(),
    val hooked: List<FishSpecies> = emptyList(),
    /** Denizanasi carpmasindan sonra kisa korunma. */
    val stunned: Float = 0f,
    val time: Float = 0f,
) {
    val value: Int get() = hooked.sumOf { it.value }
}

data class CastEvents(val caught: FishSpecies? = null, val jellyHit: Boolean = false, val lostFish: Boolean = false, val finished: Boolean = false)

/** Balik tutma kurallari. Android'den bagimsiz, unit test ile test edilir. */
class FishingEngine(private val random: Random = Random.Default) {
    private var nextId = 1L

    fun newCast(maxDepth: Float, capacity: Int, speedFactor: Float): FishingCast {
        val creatures = ArrayList<SeaCreature>()
        var d = 3f
        while (d < maxDepth + 8f) {
            repeat(if (random.nextFloat() < 0.5f) 1 else 2) {
                creatures += spawn(d + random.nextFloat() * 2f)
            }
            d += 3.2f
        }
        return FishingCast(maxDepth = maxDepth, capacity = capacity, speedFactor = speedFactor, creatures = creatures)
    }

    private fun spawn(depth: Float): SeaCreature {
        val x = 0.5f + random.nextFloat() * (WIDTH - 1f)
        val dir = if (random.nextBoolean()) 1 else -1
        // Denizanasi orani derinlikle biraz artar
        if (depth > 8f && random.nextFloat() < 0.12f + depth / 900f) return SeaCreature(nextId++, null, x, depth, dir)
        val options = FishSpecies.entries.filter { depth in it.minDepth..it.maxDepth }
        val species = if (options.isEmpty()) FishSpecies.SARDINE else {
            val total = options.sumOf { it.rarity.toDouble() }.toFloat()
            var roll = random.nextFloat() * total
            options.firstOrNull { roll -= it.rarity; roll <= 0f } ?: options.last()
        }
        return SeaCreature(nextId++, species, x, depth, dir)
    }

    fun start(c: FishingCast) = if (c.phase == CastPhase.READY) c.copy(phase = CastPhase.DESCENDING) else c

    fun steer(c: FishingCast, x: Float) = c.copy(targetX = x.coerceIn(0.3f, WIDTH - 0.3f))

    fun update(c: FishingCast, dt: Float): Pair<FishingCast, CastEvents> {
        // Baliklar yatayda yuzer, kenarda doner
        val creatures = c.creatures.map { cr ->
            if (cr.caught) return@map cr
            val speed = (cr.species?.speed ?: 0.6f) * 0.6f
            var x = cr.x + cr.dir * speed * dt
            var dir = cr.dir
            if (x < 0.3f) { x = 0.3f; dir = 1 }
            if (x > WIDTH - 0.3f) { x = WIDTH - 0.3f; dir = -1 }
            cr.copy(x = x, dir = dir)
        }
        var s = c.copy(creatures = creatures, time = c.time + dt, stunned = (c.stunned - dt).coerceAtLeast(0f))
        if (s.phase != CastPhase.DESCENDING && s.phase != CastPhase.ASCENDING) return s to CastEvents()

        // Kanca parmagi takip eder
        val follow = (s.targetX - s.hookX) * (1f - kotlin.math.exp(-STEER_RESPONSE * dt))
        s = s.copy(hookX = s.hookX + follow)

        var events = CastEvents()
        if (s.phase == CastPhase.DESCENDING) {
            val depth = s.depth + DESCENT_SPEED * s.speedFactor * dt
            s = s.copy(depth = depth.coerceAtMost(s.maxDepth))
            val touched = touching(s)
            when {
                touched?.isJelly == true -> {
                    s = s.copy(phase = CastPhase.ASCENDING, stunned = STUN_TIME)
                    events = CastEvents(jellyHit = true)
                }
                touched != null -> {
                    s = catchIt(s, touched).copy(phase = CastPhase.ASCENDING)
                    events = CastEvents(caught = touched.species)
                }
                depth >= s.maxDepth -> s = s.copy(phase = CastPhase.ASCENDING)
            }
        } else {
            val depth = s.depth - ASCENT_SPEED * s.speedFactor * dt
            s = s.copy(depth = depth.coerceAtLeast(0f))
            if (s.stunned <= 0f) {
                val touched = touching(s)
                if (touched?.isJelly == true) {
                    // Denizanasi: son tutulan balik kacar
                    val lost = s.hooked.isNotEmpty()
                    s = s.copy(hooked = s.hooked.dropLast(1), stunned = STUN_TIME)
                    events = CastEvents(jellyHit = true, lostFish = lost)
                } else if (touched != null && s.hooked.size < s.capacity) {
                    s = catchIt(s, touched)
                    events = CastEvents(caught = touched.species)
                }
            }
            if (s.depth <= 0f) {
                s = s.copy(phase = CastPhase.DONE)
                events = events.copy(finished = true)
            }
        }
        return s to events
    }

    private fun touching(c: FishingCast): SeaCreature? = c.creatures.firstOrNull { cr ->
        !cr.caught && abs(cr.depth - c.depth) < cr.radius + HOOK_RADIUS && abs(cr.x - c.hookX) < cr.radius * 1.6f + HOOK_RADIUS
    }

    private fun catchIt(c: FishingCast, cr: SeaCreature) = c.copy(
        hooked = c.hooked + cr.species!!,
        creatures = c.creatures.map { if (it.id == cr.id) it.copy(caught = true) else it },
    )

    companion object {
        const val WIDTH = 10f
        /** Ekranda gorunen derinlik araligi (metre). */
        const val VIEW_HEIGHT = 16f
        const val DESCENT_SPEED = 9f
        const val ASCENT_SPEED = 13f
        const val HOOK_RADIUS = 0.25f
        const val STEER_RESPONSE = 9f
        const val STUN_TIME = 0.8f
        const val CASTS_PER_GAME = 3
    }
}
