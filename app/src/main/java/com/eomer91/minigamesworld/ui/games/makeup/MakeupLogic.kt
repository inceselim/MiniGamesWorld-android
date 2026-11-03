package com.eomer91.minigamesworld.ui.games.makeup

import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Makyaj Salonu kurallari (Android'den bagimsiz). Koordinatlar yuz cizimiyle ayni: viewBox 300x360.
 */

enum class MakeupTool { SHADOW, BLUSH, LIPS, LASHES, STICKER, WIPE }

/** Bolge: elips (cx, cy, rx, ry) + hangi arac. */
enum class FaceRegion(val tool: MakeupTool, val cx: Float, val cy: Float, val rx: Float, val ry: Float) {
    SHADOW_L(MakeupTool.SHADOW, 113f, 158f, 30f, 18f),
    SHADOW_R(MakeupTool.SHADOW, 187f, 158f, 30f, 18f),
    BLUSH_L(MakeupTool.BLUSH, 100f, 212f, 30f, 24f),
    BLUSH_R(MakeupTool.BLUSH, 200f, 212f, 30f, 24f),
    LIPS(MakeupTool.LIPS, 150f, 246f, 32f, 16f),
    ;

    fun contains(x: Float, y: Float, pad: Float = 1f): Boolean {
        val dx = (x - cx) / (rx * pad)
        val dy = (y - cy) / (ry * pad)
        return dx * dx + dy * dy <= 1f
    }
}

data class RegionPaint(val color: Long = 0, val coverage: Float = 0f)

data class Sticker(val kind: Int, val x: Float, val y: Float)

data class FaceMakeup(
    val regions: Map<FaceRegion, RegionPaint> = emptyMap(),
    val lashes: Int = 0,
    val stickers: List<Sticker> = emptyList(),
) {
    fun paint(r: FaceRegion) = regions[r] ?: RegionPaint()
}

data class Customer(
    val skin: Long,
    val hair: String,
    val hairColor: Long,
    val eyes: Long,
    val top: Long,
    val lips: Long,
)

/** Musterinin istedigi gorunum; null = o bolgeye makyaj istemiyor. */
data class MakeupRequest(val shadow: Long?, val blush: Long?, val lips: Long?, val lashes: Int)

object MakeupPalettes {
    val shadow = listOf(0xFFC77DFF, 0xFF9D4EDD, 0xFF4CC9F0, 0xFF06D6A0, 0xFFFFD166, 0xFFF4A261, 0xFFB08968, 0xFFFF8FAB)
    val blush = listOf(0xFFFF8FAB, 0xFFFF5C8A, 0xFFF4A261, 0xFFE07A5F, 0xFFC77DFF)
    val lips = listOf(0xFFE5383B, 0xFFD00000, 0xFFFF4D6D, 0xFFFF8FAB, 0xFFB5179E, 0xFF9D0208, 0xFFC08081, 0xFFF4A261)
    val skin = listOf(0xFFFCE3D3, 0xFFF6D2B8, 0xFFE8B48F, 0xFFC68642, 0xFF8D5524, 0xFF5C3A21)
    val hair = listOf(0xFF6B3E26, 0xFF3B2416, 0xFFF2C14E, 0xFF1B1B1B, 0xFFC0392B, 0xFF8E44AD, 0xFFFF7EB6, 0xFFE8E8E8)
    val eyes = listOf(0xFF5B8DD6, 0xFF6B4226, 0xFF2E8B57, 0xFF8A8F98)
    val tops = listOf(0xFFFF8FAB, 0xFF4CC9F0, 0xFF9D4EDD, 0xFF06D6A0, 0xFFFFD166, 0xFFF4A261, 0xFFFFFFFF)
    val naturalLips = listOf(0xFFD98A8A, 0xFFC97B7B, 0xFFB5646E, 0xFFE39A9A)
    val hairStyles = listOf("long", "bob", "bun")
    val stickers = listOf("⭐", "💖", "🌸", "✨", "🦋", "💎")
}

object Makeup {
    /** Parmakla surme: [dist] kat edilen yol (yuz birimi). */
    fun rub(face: FaceMakeup, tool: MakeupTool, color: Long, x: Float, y: Float, dist: Float): FaceMakeup {
        if (tool == MakeupTool.WIPE) {
            val regions = face.regions.mapValues { (r, p) ->
                if (r.contains(x, y, 1.2f)) p.copy(coverage = (p.coverage - dist * 0.02f).coerceAtLeast(0f)) else p
            }
            val nearEyes = FaceRegion.SHADOW_L.contains(x, y, 1.2f) || FaceRegion.SHADOW_R.contains(x, y, 1.2f)
            val stickers = face.stickers.filter { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) > 18f * 18f }
            return face.copy(regions = regions, lashes = if (nearEyes && dist > 6f) 0 else face.lashes, stickers = stickers)
        }
        var changed = false
        val regions = face.regions.toMutableMap()
        for (r in FaceRegion.entries) {
            if (r.tool != tool || !r.contains(x, y, 1.15f)) continue
            val p = face.paint(r)
            val base = if (p.coverage <= 0.01f) color else p.color
            val mixed = lerpColor(base, color, (dist * 0.03f).coerceIn(0f, 1f))
            regions[r] = RegionPaint(mixed, (p.coverage + dist * 0.012f).coerceAtMost(1f))
            changed = true
        }
        return if (changed) face.copy(regions = regions) else face
    }

    fun addSticker(face: FaceMakeup, kind: Int, x: Float, y: Float): FaceMakeup =
        if (face.stickers.size >= 8) face else face.copy(stickers = face.stickers + Sticker(kind, x, y))

    fun lerpColor(a: Long, b: Long, t: Float): Long {
        fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toFloat()
        val r = ch(a, 16) + (ch(b, 16) - ch(a, 16)) * t
        val g = ch(a, 8) + (ch(b, 8) - ch(a, 8)) * t
        val bl = ch(a, 0) + (ch(b, 0) - ch(a, 0)) * t
        return 0xFF000000 or (r.toLong() shl 16) or (g.toLong() shl 8) or bl.toLong()
    }

    fun colorDistance(a: Long, b: Long): Float {
        fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toFloat()
        val dr = ch(a, 16) - ch(b, 16)
        val dg = ch(a, 8) - ch(b, 8)
        val db = ch(a, 0) - ch(b, 0)
        return sqrt(dr * dr + dg * dg + db * db)
    }

    /** 0..1 arasi uygunluk. */
    fun match(face: FaceMakeup, req: MakeupRequest): Float {
        var got = 0f
        var total = 0f
        fun region(r: FaceRegion, want: Long?, weight: Float) {
            total += weight
            val p = face.paint(r)
            got += weight * when {
                want == null -> if (p.coverage < 0.2f) 1f else 0.3f
                p.coverage < 0.35f -> 0f
                else -> {
                    val d = colorDistance(p.color, want)
                    val colorScore = when {
                        d < 45f -> 1f
                        d < 90f -> 0.6f
                        else -> 0.2f
                    }
                    colorScore * (if (p.coverage >= 0.6f) 1f else 0.7f)
                }
            }
        }
        region(FaceRegion.SHADOW_L, req.shadow, 1f)
        region(FaceRegion.SHADOW_R, req.shadow, 1f)
        region(FaceRegion.BLUSH_L, req.blush, 1f)
        region(FaceRegion.BLUSH_R, req.blush, 1f)
        region(FaceRegion.LIPS, req.lips, 2f)
        total += 2f
        if (face.lashes == req.lashes) got += 2f else if (face.lashes != 0 && req.lashes != 0) got += 1f
        return got / total
    }

    fun stars(match: Float): Int = when {
        match >= 0.9f -> 3
        match >= 0.7f -> 2
        match >= 0.45f -> 1
        else -> 0
    }

    fun randomCustomer(random: Random = Random.Default) = Customer(
        skin = MakeupPalettes.skin.random(random),
        hair = MakeupPalettes.hairStyles.random(random),
        hairColor = MakeupPalettes.hair.random(random),
        eyes = MakeupPalettes.eyes.random(random),
        top = MakeupPalettes.tops.random(random),
        lips = MakeupPalettes.naturalLips.random(random),
    )

    fun randomRequest(random: Random = Random.Default) = MakeupRequest(
        shadow = if (random.nextInt(5) == 0) null else MakeupPalettes.shadow.random(random),
        blush = if (random.nextInt(4) == 0) null else MakeupPalettes.blush.random(random),
        lips = MakeupPalettes.lips.random(random),
        lashes = random.nextInt(3),
    )
}
