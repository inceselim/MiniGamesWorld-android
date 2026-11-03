package com.eomer91.minigamesworld.ui.games.fox

// Tum bolumlerin bitirilebildigini ve 3 yildizin alinabildigini kontrol eden arama botu (gelistirme araci).
// Calistirma: tools/levels/README.md
import java.util.PriorityQueue

val fields = FoxEngine::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) && !java.lang.reflect.Modifier.isFinal(it.modifiers) || it.name in setOf("tiles","enemies") }.onEach { it.isAccessible = true }
val efields = FoxEnemy::class.java.declaredFields.onEach { it.isAccessible = true }

fun clone(e: FoxEngine, w: Int, l: Int): FoxEngine {
    val n = FoxEngine(w, l, FoxLevels.worlds[w][l])
    for (f in fields) {
        when (f.name) {
            "tiles" -> { val src = e.tiles; for (r in src.indices) src[r].copyInto(n.tiles[r]) }
            "enemies" -> { n.enemies.clear(); e.enemies.forEach { en -> val c = FoxEnemy(en.type, en.x, en.y); for (ef in efields) ef.set(c, ef.get(en)); n.enemies += c } }
            "particles", "coinPops", "bumps", "random", "source" -> {}
            else -> if (f.type.isPrimitive || f.type == String::class.java) f.set(n, f.get(e))
        }
    }
    return n
}

class Node(val e: FoxEngine, val held: Boolean, val steps: Int, val h: Float)

/** goal: hedef kosulu; heur: kalan mesafe. Hasar alirsa budanir. */
fun search(w: Int, l: Int, start: FoxEngine?, goal: (FoxEngine) -> Boolean, heur: (FoxEngine) -> Float, maxNodes: Int = 400000): FoxEngine? {
    val s0 = start ?: FoxEngine(w, l, FoxLevels.worlds[w][l]).also { repeat(30) { _ -> it.update(1/60f, 0) } }
    val pq = PriorityQueue<Node>(compareBy { it.h + it.steps * 0.02f })
    val seen = HashSet<Long>()
    pq += Node(s0, false, 0, heur(s0))
    var count = 0
    while (pq.isNotEmpty() && count < maxNodes) {
        val nd = pq.poll(); count++
        for (dir in -1..1) for (hold in listOf(false, true)) {
            val e = clone(nd.e, w, l)
            var held = nd.held
            val hearts = e.hearts
            if (hold && !held) { e.pressJump(); held = true }
            if (!hold && held) { e.releaseJump(); held = false }
            var ok = true
            repeat(6) { if (ok) { e.update(1/60f, dir); if (e.dead || e.hearts < hearts) ok = false; if (goal(e)) return e } }
            if (!ok) continue
            val key = ((e.px * 3).toInt().toLong() shl 40) or (((e.py * 3).toInt() + 100).toLong() shl 28) or (((e.vx + 10).toInt()).toLong() shl 20) or ((((e.vy + 30) / 3).toInt()).toLong() shl 12) or ((if (held) 1L else 0L) shl 8) or (e.stars.toLong() shl 4) or (if (e.onGround) 1L else 0L)
            if (!seen.add(key)) continue
            pq += Node(e, held, nd.steps + 1, heur(e))
        }
    }
    return null
}

fun main() {
    for (w in 0 until 3) for (l in 0 until 5) {
        val rows = FoxLevels.worlds[w][l]
        val stars = mutableListOf<Pair<Int, Int>>()
        rows.forEachIndexed { r, s -> s.forEachIndexed { c, ch -> if (ch == '*') stars += c to r } }
        val t0 = System.currentTimeMillis()
        val fin = search(w, l, null, { it.won }, { (it.flagCol - it.px) / 7f })
        val starRes = stars.map { (c, r) ->
            val got = search(w, l, null, { e -> e.tiles[r][c] != '*' && e.onGround }, { e -> (kotlin.math.abs(c + 0.5f - e.px) + kotlin.math.abs(r + 0.5f - e.py)) / 7f })
            if (got == null) "FAIL@$c,$r" else if (search(w, l, got, { it.won }, { (it.flagCol - it.px) / 7f }) != null) "ok" else "STUCK@$c,$r"
        }
        println("level ${w+1}-${l+1}: finish=${if (fin != null) "ok %.1fs".format(fin.time) else "FAIL"} stars=$starRes (${System.currentTimeMillis()-t0}ms)")
    }
}
