package com.eomer91.minigamesworld.ui.games.island.render

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.Cat
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Haritanin dunya olcegi: karo 64x32 birim */
const val IS_TW = 64f
const val IS_TH = 32f
const val IS_TOP = 260f

fun isLayout(n: Int) = IsoLayout(n, IS_TW, IS_TH, IS_TOP)

/** Kamera: dunya biriminde ekran merkezi; scale = ekran pikseli / dunya birimi */
@Stable
class IsCamera {
    var cx by mutableFloatStateOf(0f)
    var cy by mutableFloatStateOf(0f)
    var scale by mutableFloatStateOf(1f)
    var viewW by mutableFloatStateOf(0f)
    var viewH by mutableFloatStateOf(0f)
    var ready by mutableStateOf(false)

    fun minScale(L: IsoLayout) = if (viewW <= 0f) 0.2f else min(viewW / L.width, viewH / L.height) * 0.95f
    /** En yakin: karo 192 piksel (en yuksek LOD ile birebir) */
    fun maxScale() = 3f

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
class IsGhost(val type: BType, val x: Int, val y: Int, val ok: Boolean, val color: Int, val tiles: List<Pair<Int, Int>>? = null)

/** Secili alan (karo dikdortgeni) */
class IsSelection(val x: Int, val y: Int, val w: Int, val h: Int, val color: Int = 0xFFFFFFFF.toInt())

/**
 * Ada haritasi. version her degistiginde (gun ilerleyince, insa edilince) yeniden cizilir.
 * onTap: dokunulan karo ve (pickBuildings acikken govdesine dokunulan) bina id'si, yoksa -1.
 * Motor ana is parcaciginda calistigi icin dunya cizim sirasinda degismez.
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
    // Pisirilen parca hazir olunca yeniden ciz
    var tick by remember { mutableIntStateOf(0) }
    val cache = remember(world) { IsRenderCache(world) { tick++ } }
    DisposableEffect(cache) { onDispose { cache.close() } }
    val L = cache.L
    val snap = remember(world, version) { IsoScene.snapshot(world) }
    val snapState = rememberUpdatedState(snap)
    val tapState = rememberUpdatedState(onTap)
    val pickState = rememberUpdatedState(pickBuildings)
    val paints = remember { Paints() }

    Canvas(
        modifier
            .onSizeChanged {
                camera.viewW = it.width.toFloat(); camera.viewH = it.height.toFloat()
                if (!camera.ready) {
                    camera.scale = max(camera.minScale(L), 1f)
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
        if (tick < 0) return@Canvas
        val s = camera.scale
        val tilePx = IS_TW * s
        cache.madeThisFrame = 0
        cache.ensureOverview()
        drawIntoCanvas { cc ->
            val nc = cc.nativeCanvas
            nc.drawColor(IsoArt.DEEP)
            nc.save()
            nc.translate(size.width / 2f, size.height / 2f)
            nc.scale(s, s)
            nc.translate(-camera.cx, -camera.cy)

            // Gorunen dunya dikdortgeni
            val vl = camera.cx - size.width / 2f / s; val vr = camera.cx + size.width / 2f / s
            val vt = camera.cy - size.height / 2f / s; val vb = camera.cy + size.height / 2f / s

            // 1) Genel bakis (her zaman; parcalar gelene kadar bosluk kalmasin)
            cache.overview?.let { bmp ->
                paints.dst.set(0f, 0f, L.width, L.height)
                nc.drawBitmap(bmp, null, paints.dst, paints.terrain)
            }
            // 2) Keskin zemin parcalari (uzaktan bakarken genel bakis yeterli)
            if (tilePx > 18f) {
                val lod = IsRenderCache.lodFor(IsRenderCache.TERRAIN_LODS, tilePx)
                val S = cache.chunkWorld(lod)
                val i0 = max(0, floor(vl / S).toInt()); val i1 = min(floor(L.width / S).toInt(), floor(vr / S).toInt())
                val j0 = max(0, floor(vt / S).toInt()); val j1 = min(floor(L.height / S).toInt(), floor(vb / S).toInt())
                for (j in j0..j1) for (i in i0..i1) {
                    val ch = cache.chunk(lod, i, j) ?: continue
                    paints.dst.set(i * S, j * S, (i + 1) * S, (j + 1) * S)
                    nc.drawBitmap(ch.bmp, null, paints.dst, paints.terrain)
                }
            }

            val p = paints.painter
            p.canvas = nc

            // 3) Arsa sinirlari
            if (s > 0.3f) {
                val c1 = L.toTileF(vl, vt); val c2 = L.toTileF(vr, vt); val c3 = L.toTileF(vl, vb); val c4 = L.toTileF(vr, vb)
                val x0 = min(min(c1.first, c2.first), min(c3.first, c4.first)).toInt() - 1
                val x1 = max(max(c1.first, c2.first), max(c3.first, c4.first)).toInt() + 1
                val y0 = min(min(c1.second, c2.second), min(c3.second, c4.second)).toInt() - 1
                val y1 = max(max(c1.second, c2.second), max(c3.second, c4.second)).toInt() + 1
                IsoScene.drawPlots(p, world, L, x0, y0, x1, y1)
            }

            selection?.let { IsoScene.drawFootprint(p, L, it.x, it.y, it.w, it.h, IsoColor.alpha(it.color, 60), it.color) }

            // 4) Binalar (arkadan one)
            val slod = IsRenderCache.lodFor(IsRenderCache.SPRITE_LODS, tilePx)
            for (b in snap) {
                val bx = L.tileX(b.x.toFloat(), b.y.toFloat()); val by = L.tileY(b.x.toFloat(), b.y.toFloat())
                val left = bx - b.type.h * IS_TW / 2f; val right = bx + (b.type.w + 1) * IS_TW / 2f
                val bottom = by + (b.type.w + b.type.h + 1) * IS_TH / 2f
                val top = by - IsoArt.height(b.type.key) - 60f
                if (right < vl || left > vr || bottom < vt || top > vb) continue
                // Yollar komsularina gore baglandigi icin hep vektorle (ucuz) cizilir
                if (b.building || b.closed || b.type.cat == Cat.ROAD) { IsoScene.drawBuilding(p, L, b); continue }
                val sp = cache.sprite(b.type, b.color, slod)
                if (sp == null) { IsoScene.drawBuilding(p, L, b); continue }
                paints.dst.set(bx - sp.ox * sp.k, by - sp.oy * sp.k, bx + (sp.bmp.width - sp.ox) * sp.k, by + (sp.bmp.height - sp.oy) * sp.k)
                nc.drawBitmap(sp.bmp, null, paints.dst, paints.sprite)
            }
            // Ilk kare kotaya takildiysa bir sonraki karede tamamla
            if (cache.madeThisFrame >= 4) tick++

            // 5) Yerlestirme onizlemesi
            ghost?.let { g ->
                val fill = if (g.ok) 0x5543A047 else 0x66E53935
                val stroke = if (g.ok) 0xFF43A047.toInt() else 0xFFE53935.toInt()
                if (g.tiles != null) {
                    // Yol hatti: dosenecek karolar
                    for ((tx, ty) in g.tiles) IsoScene.drawFootprint(p, L, tx, ty, 1, 1, fill, stroke)
                    IsoScene.drawFootprint(p, L, g.x, g.y, 1, 1, 0x00000000, 0xFFFFFFFF.toInt())
                    return@let
                }
                IsoScene.drawFootprint(p, L, g.x, g.y, g.type.w, g.type.h, fill, stroke)
                val bx = L.tileX(g.x.toFloat(), g.y.toFloat()); val by = L.tileY(g.x.toFloat(), g.y.toFloat())
                nc.saveLayerAlpha(vl, vt, vr, vb, 160)
                IsoArt.building(p, g.type, bx, by, L.tw, L.th, g.color)
                nc.restore()
            }

            p.canvas = null
            nc.restore()
        }
    }
}

private class Paints {
    val dst = RectF()
    /** Zemin: kenar yumusatma kapali (parca sinirlarinda cizgi olmasin), suzme acik */
    val terrain = Paint(Paint.FILTER_BITMAP_FLAG)
    val sprite = Paint(Paint.FILTER_BITMAP_FLAG)
    val painter = AndroidIsoPainter(true)
}
