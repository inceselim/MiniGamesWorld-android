package com.eomer91.minigamesworld.ui.games.kingdom

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.eomer91.minigamesworld.R
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Pixel art cizimleri (tools/pixel klasorundeki betiklerle uretilir). Hepsi en yakin komsu
 * olcekleme ile cizilir ki pikseller bulaniklasmasin.
 */

/** Kale 3 asamali (1-3, 4-7, 8-10); diger binalar 2 asamali (1-4, 5-10). */
@DrawableRes
fun buildingSprite(b: KBuilding, level: Int): Int {
    val high = level >= 5
    return when (b) {
        KBuilding.CASTLE -> when {
            level >= 8 -> R.drawable.ik_castle_3
            level >= 4 -> R.drawable.ik_castle_2
            else -> R.drawable.ik_castle_1
        }
        KBuilding.GOLD_MINE -> if (high) R.drawable.ik_gold_mine_2 else R.drawable.ik_gold_mine_1
        KBuilding.LUMBER_CAMP -> if (high) R.drawable.ik_lumber_2 else R.drawable.ik_lumber_1
        KBuilding.QUARRY -> if (high) R.drawable.ik_quarry_2 else R.drawable.ik_quarry_1
        KBuilding.FARM -> if (high) R.drawable.ik_farm_2 else R.drawable.ik_farm_1
        KBuilding.WAREHOUSE -> if (high) R.drawable.ik_warehouse_2 else R.drawable.ik_warehouse_1
        KBuilding.BARRACKS -> if (high) R.drawable.ik_barracks_2 else R.drawable.ik_barracks_1
        KBuilding.ARCHERY -> if (high) R.drawable.ik_archery_2 else R.drawable.ik_archery_1
        KBuilding.STABLE -> if (high) R.drawable.ik_stable_2 else R.drawable.ik_stable_1
    }
}

@DrawableRes
fun unitSprite(u: UnitType): Int = when (u) {
    UnitType.SWORDSMAN -> R.drawable.ik_unit_sword
    UnitType.SPEARMAN -> R.drawable.ik_unit_spear
    UnitType.ARCHER -> R.drawable.ik_unit_archer
    UnitType.CAVALRY -> R.drawable.ik_unit_cav
}

@DrawableRes
fun heroSprite(h: Hero): Int = when (h) {
    Hero.ARTHUR -> R.drawable.ik_hero_arthur
    Hero.ROBIN -> R.drawable.ik_hero_robin
    Hero.LEONIDAS -> R.drawable.ik_hero_leonidas
}

@DrawableRes
fun resSprite(r: Res): Int = when (r) {
    Res.GOLD -> R.drawable.ik_res_gold
    Res.WOOD -> R.drawable.ik_res_wood
    Res.STONE -> R.drawable.ik_res_stone
    Res.FOOD -> R.drawable.ik_res_food
}

private val GRAYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Keskin pikselli resim. [locked] ise gri ve soluk. */
@Composable
fun Sprite(@DrawableRes id: Int, size: Dp, modifier: Modifier = Modifier, locked: Boolean = false, flipX: Boolean = false) {
    val bmp = ImageBitmap.imageResource(id)
    val painter = remember(bmp) { BitmapPainter(bmp, filterQuality = FilterQuality.None) }
    Image(
        painter = painter,
        contentDescription = null,
        colorFilter = if (locked) GRAYSCALE else null,
        alpha = if (locked) 0.45f else 1f,
        modifier = modifier
            .size(size)
            .then(if (flipX) Modifier.graphicsFlip() else Modifier),
    )
}

private fun Modifier.graphicsFlip(): Modifier = this.graphicsLayer { scaleX = -1f }

/** Bitmap'i sahne pikselinden ekran pikseline en yakin komsu ile ciz. */
private fun DrawScope.blit(img: ImageBitmap, x: Float, y: Float, scale: Float, w: Int = img.width, h: Int = img.height) {
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset((x * scale).roundToInt(), (y * scale).roundToInt()),
        dstSize = IntSize((w * scale).roundToInt(), (h * scale).roundToInt()),
        filterQuality = FilterQuality.None,
    )
}

private const val SCENE_W = 192f
private const val SCENE_H = 84f

/** Sahnedeki bina yerleri: (sol x, taban y) – sahne pikseli. Tabana gore sirali cizilir. */
private val SLOTS: Map<KBuilding, Pair<Int, Int>> = mapOf(
    KBuilding.GOLD_MINE to (6 to 62),
    KBuilding.LUMBER_CAMP to (40 to 61),
    KBuilding.QUARRY to (152 to 61),
    KBuilding.FARM to (118 to 64),
    KBuilding.CASTLE to (80 to 70),
    KBuilding.ARCHERY to (4 to 84),
    KBuilding.WAREHOUSE to (44 to 84),
    KBuilding.BARRACKS to (120 to 84),
    KBuilding.STABLE to (158 to 84),
)

private val VILLAGER_COLORS = listOf(Color(0xFF3B5DC9), Color(0xFFB13E53), Color(0xFF38B764), Color(0xFFFFCD75), Color(0xFF6E3CA0))

/**
 * Canli krallik sahnesi: gokyuzu, daglar, sahip olunan binalar (seviyeye gore gorunum),
 * suzulen bulutlar ve yolda yuruyen koyluler. Koylu sayisi kale seviyesiyle artar.
 */
@Composable
fun KingdomScene(levels: Map<KBuilding, Int>, modifier: Modifier = Modifier) {
    val bg = ImageBitmap.imageResource(R.drawable.ik_scene_bg)
    val sprites = KBuilding.entries.associateWith { b ->
        ImageBitmap.imageResource(buildingSprite(b, levels[b] ?: 0))
    }
    val t by rememberInfiniteTransition(label = "scene").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )
    val castle = levels[KBuilding.CASTLE] ?: 1
    Canvas(modifier.fillMaxWidth().aspectRatio(SCENE_W / SCENE_H)) {
        val s = size.width / SCENE_W
        blit(bg, 0f, 0f, s)
        // bulutlar
        for (k in 0 until 3) {
            val cx = ((t * (2.5f + k) + k * 70f) % (SCENE_W + 40f)) - 20f
            val cy = 8f + k * 7f
            cloud(cx, cy, s)
        }
        // binalar (taban y'ye gore)
        val order = SLOTS.entries.sortedBy { it.value.second }
        for ((b, pos) in order) {
            if ((levels[b] ?: 0) <= 0) continue
            val img = sprites[b] ?: continue
            blit(img, pos.first.toFloat(), (pos.second - img.height).toFloat(), s)
        }
        // koyluler: yol boyunca gidip gelir
        val n = (1 + castle).coerceAtMost(8)
        for (k in 0 until n) {
            val speed = 4f + (k % 3) * 1.5f
            val span = 150f
            val phase = (t * speed + k * 37f) % (span * 2)
            val x = 20f + if (phase < span) phase else span * 2 - phase
            val y = 72f + (k % 3) * 4f
            val bob = if (sin(t * 12f + k) > 0) 0f else 1f
            villager(x, y - bob, VILLAGER_COLORS[k % VILLAGER_COLORS.size], s)
        }
    }
}

private fun DrawScope.px(x: Float, y: Float, w: Float, h: Float, c: Color, s: Float) {
    drawRect(c, Offset((x * s).roundToInt().toFloat(), (y * s).roundToInt().toFloat()), Size((w * s).coerceAtLeast(1f), (h * s).coerceAtLeast(1f)))
}

private fun DrawScope.cloud(x: Float, y: Float, s: Float) {
    val c = Color(0xF2F4F4F4)
    val xx = x.roundToInt().toFloat()
    px(xx + 3, y, 10f, 2f, c, s)
    px(xx, y + 2, 18f, 3f, c, s)
    px(xx + 6, y - 2, 6f, 2f, c, s)
}

private fun DrawScope.villager(x: Float, y: Float, body: Color, s: Float) {
    val xx = x.roundToInt().toFloat()
    val yy = y.roundToInt().toFloat()
    px(xx - 1, yy - 1, 5f, 9f, Color(0xFF1A1C2C), s)
    px(xx, yy, 3f, 3f, Color(0xFFFFDCB9), s)
    px(xx, yy + 3, 3f, 3f, body, s)
    px(xx, yy + 6, 1f, 1f, Color(0xFF603828), s)
    px(xx + 2, yy + 6, 1f, 1f, Color(0xFF603828), s)
}

/** Haritadaki bolge dugumleri (ik_world_map.png pikseli, alttan uste). */
val MAP_NODES: List<Pair<Int, Int>> = listOf(
    64 to 340, 96 to 317, 90 to 294, 54 to 271, 29 to 248, 46 to 225, 84 to 202, 99 to 179,
    72 to 156, 36 to 133, 33 to 110, 67 to 87, 97 to 64, 88 to 41, 51 to 18,
)
const val MAP_W = 128f
const val MAP_H = 360f

/** Savas sahnesi icin iki ordunun karsilasma animasyonu zamani (0..1 dongu). */
@Composable
fun rememberClash(): Float {
    val v by rememberInfiniteTransition(label = "clash").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "c",
    )
    return v
}
