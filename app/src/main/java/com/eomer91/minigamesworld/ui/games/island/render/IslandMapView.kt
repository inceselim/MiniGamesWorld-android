package com.eomer91.minigamesworld.ui.games.island.render

import android.graphics.Bitmap
import android.graphics.Canvas as ACanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Haritanin dunya olcegi: karo 64x32 birim */
const val IS_TW = 64f
const val IS_TH = 32f
const val IS_TOP = 260f

fun isLayout(n: Int) = IsoLayout(n, IS_TW, IS_TH, IS_TOP)

/** Kamera: dunya pikselinde ekran merkezi ve olcek */
@Stable
class IsCamera {
    var cx by mutableFloatStateOf(0f)
    var cy by mutableFloatStateOf(0f)
    var scale by mutableFloatStateOf(1f)
    var viewW by mutableFloatStateOf(0f)
    var viewH by mutableFloatStateOf(0f)
    var ready by mutableStateOf(false)

    fun minScale(L: IsoLayout) = if (viewW <= 0f) 0.2f else min(viewW / L.width, viewH / L.height) * 0.95f
    fun maxScale() = 2.2f

    fun clamp(L: IsoLayout) {
        scale = scale.coerceIn(minScale(L), maxScale())
        cx = cx.coerceIn(0f, L.width)
        cy = cy.coerceIn(0f, L.height)
    }

    /** Karoyu ekranin ortasina getir */
    fun centerOn(L: IsoLayout, tx: Float, ty: Float, s: Float? = null) {
        cx = L.tileX(tx, ty); cy = L.tileY(tx, ty)
        if (s != null) scale = s
        clamp(L)
    }

    fun toWorldX(sx: Float) = cx + (sx - viewW / 2f) / scale
    fun toWorldY(sy: Float) = cy + (sy - viewH / 2f) / scale
}

/** Yerlestirme onizlemesi */
class IsGhost(val type: BType, val x: Int, val y: Int, val ok: Boolean, val color: Int)

/** Secili alan (karo dikdortgeni) */
class IsSelection(val x: Int, val y: Int, val w: Int, val h: Int, val color: Int = 0xFFFFFFFF.toInt())

/** Bitmap onbellegi: pisirilmis zemin ve bina cizimleri */
private class IsRenderCache(val n: Int) {
    val sprites = HashMap<Long, Sprite>()
    val painter = AndroidIsoPainter(true)
    val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    val ghostPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 150 }
    val dst = RectF()

    class Sprite(val bmp: Bitmap, val ox: Float, val oy: Float, val k: Float)

    fun sprite(t: BType, color: Int): Sprite {
        val key = (t.id.toLong() shl 32) or (color.toLong() and 0xFFFFFFFFL)
        sprites[key]?.let { return it }
        val tw = SPRITE_TW; val th = tw / 2f
        val box = IsoArt.spriteBox(t, tw, th)
        val bmp = Bitmap.createBitmap(ceil(box.w).toInt(), ceil(box.h).toInt(), Bitmap.Config.ARGB_8888)
        val c = ACanvas(bmp)
        painter.canvas = c
        IsoArt.building(painter, t, box.ox, box.oy, tw, th, color)
        painter.canvas = null
        // Dunya olcegine donusum: sprite pikseli * k = dunya birimi
        val s = Sprite(bmp, box.ox, box.oy, IS_TW / tw)
        sprites[key] = s
        return s
    }

    fun recycle() {
        for (s in sprites.values) s.bmp.recycle()
        sprites.clear()
    }

    companion object { const val SPRITE_TW = 88f }
}

/** Zemini tek bir bitmap'e pisir (arka planda) */
private fun bakeTerrain(w: IsWorld): Pair<Bitmap, IsoLayout> {
    val tw = if (w.n <= 96) 32f else 24f
    val L = IsoLayout(w.n, tw, tw / 2f, IS_TOP * tw / IS_TW)
    val bmp = Bitmap.createBitmap(ceil(L.width).toInt(), ceil(L.height).toInt(), Bitmap.Config.RGB_565)
    val c = ACanvas(bmp)
    c.drawColor(IsoArt.DEEP)
    val p = AndroidIsoPainter(true)
    p.canvas = c
    IsoScene.drawTerrain(p, w, L)
    p.canvas = null
    return bmp to L
}

/**
 * Ada haritasi. version her degistiginde (gun ilerleyince, insa edilince) yeniden cizilir.
 * onTap: dokunulan karo; pickBuildings acikken bina govdesine dokunmak o binanin karosunu verir.
 */
@Composable
fun IslandMapView(
    world: IsWorld,
    version: Int,
    camera: IsCamera,
    modifier: Modifier = Modifier,
    selection: IsSelection? = null,
    ghost: IsGhost? = null,
    pickBuildings: Boolean = true,
    onTap: (x: Int, y: Int, buildingId: Int) -> Unit,
) {
    val L = remember(world) { isLayout(world.n) }
    val cache = remember(world) { IsRenderCache(world.n) }
    DisposableEffect(cache) { onDispose { cache.recycle() } }
    val terrain by produceState<Bitmap?>(null, world) {
        val r = withContext(Dispatchers.Default) { bakeTerrain(world).first }
        value = r
    }
    DisposableEffect(terrain) { val t = terrain; onDispose { t?.recycle() } }
    val snap = remember(world, version) {
        // Motor ayni anda listeyi degistirebilir: nadir cakismada bir kez daha dene
        runCatching { IsoScene.snapshot(world) }.getOrElse { runCatching { IsoScene.snapshot(world) }.getOrDefault(emptyList()) }
    }
    val snapState = rememberUpdatedState(snap)
    val tapState = rememberUpdatedState(onTap)
    val pickState = rememberUpdatedState(pickBuildings)

    Canvas(
        modifier
            .onSizeChanged {
                camera.viewW = it.width.toFloat(); camera.viewH = it.height.toFloat()
                if (!camera.ready) {
                    camera.scale = max(camera.minScale(L), 0.6f)
                    camera.cx = L.width / 2f; camera.cy = L.height / 2f
                    camera.ready = true
                }
                camera.clamp(L)
            }
            .pointerInput(world) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val ns = (camera.scale * zoom).coerceIn(camera.minScale(L), camera.maxScale())
                    val wx = camera.toWorldX(centroid.x); val wy = camera.toWorldY(centroid.y)
                    camera.scale = ns
                    camera.cx = wx - (centroid.x - camera.viewW / 2f) / ns - pan.x / ns
                    camera.cy = wy - (centroid.y - camera.viewH / 2f) / ns - pan.y / ns
                    camera.clamp(L)
                }
            }
            .pointerInput(world) {
                detectTapGestures { pt ->
                    val wx = camera.toWorldX(pt.x); val wy = camera.toWorldY(pt.y)
                    if (pickState.value) {
                        val b = IsoScene.pick(snapState.value, L, wx, wy)
                        if (b != null) { tapState.value(b.x, b.y, b.id); return@detectTapGestures }
                    }
                    val (tx, ty) = L.toTile(wx, wy)
                    if (tx in 0 until world.n && ty in 0 until world.n) tapState.value(tx, ty, -1)
                }
            },
    ) {
        val s = camera.scale
        drawIntoCanvas { cc ->
            val nc = cc.nativeCanvas
            nc.drawColor(IsoArt.DEEP)
            nc.save()
            nc.translate(size.width / 2f, size.height / 2f)
            nc.scale(s, s)
            nc.translate(-camera.cx, -camera.cy)

            terrain?.let { bmp ->
                cache.dst.set(0f, 0f, L.width, L.height)
                nc.drawBitmap(bmp, null, cache.dst, cache.bmpPaint)
            }

            // Gorunen dunya dikdortgeni
            val vl = camera.cx - size.width / 2f / s; val vr = camera.cx + size.width / 2f / s
            val vt = camera.cy - size.height / 2f / s; val vb = camera.cy + size.height / 2f / s
            val p = cache.painter
            p.canvas = nc

            // Arsa sinirlari (uzaktan bakarken gerek yok)
            if (s > 0.35f) {
                val c1 = L.toTileF(vl, vt); val c2 = L.toTileF(vr, vt); val c3 = L.toTileF(vl, vb); val c4 = L.toTileF(vr, vb)
                val x0 = min(min(c1.first, c2.first), min(c3.first, c4.first)).toInt() - 1
                val x1 = max(max(c1.first, c2.first), max(c3.first, c4.first)).toInt() + 1
                val y0 = min(min(c1.second, c2.second), min(c3.second, c4.second)).toInt() - 1
                val y1 = max(max(c1.second, c2.second), max(c3.second, c4.second)).toInt() + 1
                IsoScene.drawPlots(p, world, L, x0, y0, x1, y1)
            }

            selection?.let { IsoScene.drawFootprint(p, L, it.x, it.y, it.w, it.h, IsoColor.alpha(it.color, 60), it.color) }

            for (b in snap) {
                val bx = L.tileX(b.x.toFloat(), b.y.toFloat()); val by = L.tileY(b.x.toFloat(), b.y.toFloat())
                val left = bx - b.type.h * IS_TW / 2f; val right = bx + b.type.w * IS_TW / 2f
                val bottom = by + (b.type.w + b.type.h) * IS_TH / 2f
                val top = by - IsoArt.height(b.type.key) - 60f
                if (right < vl || left > vr || bottom < vt || top > vb) continue
                if (b.building || b.closed) {
                    IsoScene.drawBuilding(p, L, b)
                } else {
                    val sp = cache.sprite(b.type, b.color)
                    cache.dst.set(bx - sp.ox * sp.k, by - sp.oy * sp.k, bx + (sp.bmp.width - sp.ox) * sp.k, by + (sp.bmp.height - sp.oy) * sp.k)
                    nc.drawBitmap(sp.bmp, null, cache.dst, cache.bmpPaint)
                }
            }

            ghost?.let { g ->
                val fill = if (g.ok) 0x5543A047 else 0x66E53935
                val stroke = if (g.ok) 0xFF43A047.toInt() else 0xFFE53935.toInt()
                IsoScene.drawFootprint(p, L, g.x, g.y, g.type.w, g.type.h, fill, stroke)
                val bx = L.tileX(g.x.toFloat(), g.y.toFloat()); val by = L.tileY(g.x.toFloat(), g.y.toFloat())
                val sp = cache.sprite(g.type, g.color)
                cache.dst.set(bx - sp.ox * sp.k, by - sp.oy * sp.k, bx + (sp.bmp.width - sp.ox) * sp.k, by + (sp.bmp.height - sp.oy) * sp.k)
                nc.drawBitmap(sp.bmp, null, cache.dst, cache.ghostPaint)
            }

            p.canvas = null
            nc.restore()
        }
    }
}
