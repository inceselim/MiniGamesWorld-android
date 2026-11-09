package com.eomer91.minigamesworld.ui.games.island.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import com.eomer91.minigamesworld.ui.games.island.engine.BType
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import java.util.concurrent.Executors
import kotlin.math.ceil

/**
 * Cizim onbellegi. Kalite icin her sey o anki yakinlastirmaya uygun cozunurlukte uretilir:
 *  - Genel bakis: tum zemin, dusuk cozunurluk (her zaman alttadir, bosluk birakmaz)
 *  - Zemin parcalari: 512x512 piksel, LOD'a gore (karo 32/64/128/192 piksel), arka planda pisirilir
 *  - Bina cizimleri: tur + renk + LOD; ayni karede en fazla birkac yenisi uretilir, kalanlar vektorle cizilir
 * Yalnizca ana is parcacigindan cagrilir (pisirme kendi is parcaciginda). Bitmap'ler recycle edilmez:
 * RenderThread son kareyi cizerken kullaniyor olabilir; referans birakilir, bellegi GC toplar.
 */
class IsRenderCache(private val world: IsWorld, private val onChange: () -> Unit) {

    val L: IsoLayout = isLayout(world.n)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "island-bake").apply { priority = Thread.NORM_PRIORITY - 1 } }
    @Volatile private var closed = false

    // ------------------------------------------------------------------ genel bakis

    var overview: Bitmap? = null; private set
    var overviewL: IsoLayout? = null; private set
    private var overviewVersion = -1
    private var overviewPending = false

    fun ensureOverview() {
        if (closed) return
        val v = world.terrainVersion
        if (overviewPending || (overview != null && overviewVersion == v)) return
        overviewPending = true
        worker.execute {
            val tw = if (world.n <= 64) 24f else 16f
            val ol = IsoLayout(world.n, tw, tw / 2f, IS_TOP * tw / IS_TW)
            val bmp = Bitmap.createBitmap(ceil(ol.width).toInt(), ceil(ol.height).toInt(), Bitmap.Config.RGB_565)
            val c = Canvas(bmp)
            c.drawColor(IsoArt.DEEP)
            val p = AndroidIsoPainter(true); p.canvas = c
            IsoScene.drawTerrain(p, world, ol, 0.8f)
            main.post {
                overviewPending = false
                if (closed) return@post
                overview = bmp; overviewL = ol; overviewVersion = v
                onChange()
            }
        }
    }

    // ------------------------------------------------------------------ zemin parcalari

    class Chunk(val bmp: Bitmap, val version: Int)

    private val chunks = object : LinkedHashMap<Long, Chunk>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Chunk>?): Boolean {
            // recycle() yok: son kare hala bu bitmap'i cizdiriyor olabilir; bellegi GC birakir
            return size > MAX_CHUNKS
        }
    }
    private val pending = HashSet<Long>()

    private fun key(lod: Int, i: Int, j: Int) = (lod.toLong() shl 40) or ((i.toLong() and 0xFFFFF) shl 20) or (j.toLong() and 0xFFFFF)

    /** Parcanin dunya boyutu (birim) */
    fun chunkWorld(lodTw: Int) = CHUNK_PX * IS_TW / lodTw

    /** Hazirsa parca (eski surumu olsa bile) dondurur; yoksa ya da eskiyse pisirmeyi siraya koyar. */
    fun chunk(lodTw: Int, i: Int, j: Int): Chunk? {
        if (closed) return null
        val k = key(lodTw, i, j)
        val c = chunks[k]
        val v = world.terrainVersion
        if ((c == null || c.version != v) && k !in pending) {
            pending.add(k)
            worker.execute { bake(lodTw, i, j, k, v) }
        }
        return c
    }

    private fun bake(lodTw: Int, i: Int, j: Int, k: Long, v: Int) {
        if (closed) return
        val S = chunkWorld(lodTw)
        val scale = lodTw / IS_TW
        val bmp = Bitmap.createBitmap(CHUNK_PX.toInt(), CHUNK_PX.toInt(), Bitmap.Config.RGB_565)
        val c = Canvas(bmp)
        c.drawColor(IsoArt.DEEP)
        c.scale(scale, scale)
        c.translate(-i * S, -j * S)
        val p = AndroidIsoPainter(true); p.canvas = c
        IsoScene.drawTerrainRect(p, world, L, i * S, j * S, (i + 1) * S, (j + 1) * S, 0.9f / scale)
        main.post {
            pending.remove(k)
            if (closed) return@post
            chunks[k] = Chunk(bmp, v)
            onChange()
        }
    }

    // ------------------------------------------------------------------ bina cizimleri

    class Sprite(val bmp: Bitmap, val ox: Float, val oy: Float, val k: Float)

    private var spriteBytes = 0L
    private val sprites = LinkedHashMap<Long, Sprite>(64, 0.75f, true)
    private val painter = AndroidIsoPainter(true)
    /** Bu karede uretilen yeni cizim sayisi */
    var madeThisFrame = 0

    private fun skey(t: BType, color: Int, lod: Int) = (lod.toLong() shl 48) or (t.id.toLong() shl 32) or (color.toLong() and 0xFFFFFFFFL)

    /** Uygun cozunurlukte cizim; uretim kotasi dolduysa baska bir cozunurluk ya da null (vektorle ciz) */
    fun sprite(t: BType, color: Int, lod: Int): Sprite? {
        sprites[skey(t, color, lod)]?.let { return it }
        if (madeThisFrame < 4) {
            madeThisFrame++
            return make(t, color, lod)
        }
        for (l in SPRITE_LODS) sprites[skey(t, color, l)]?.let { return it }
        return null
    }

    private fun make(t: BType, color: Int, lod: Int): Sprite {
        val tw = lod.toFloat(); val th = tw / 2f
        val box = IsoArt.spriteBox(t, tw, th)
        val bmp = Bitmap.createBitmap(ceil(box.w).toInt(), ceil(box.h).toInt(), Bitmap.Config.ARGB_8888)
        painter.canvas = Canvas(bmp)
        IsoArt.building(painter, t, box.ox, box.oy, tw, th, color)
        painter.canvas = null
        val s = Sprite(bmp, box.ox, box.oy, IS_TW / tw)
        sprites[skey(t, color, lod)] = s
        spriteBytes += bmp.byteCount
        // Butce asilirsa en az kullanilanlari birak
        val it = sprites.entries.iterator()
        while (spriteBytes > SPRITE_BUDGET && it.hasNext()) {
            val e = it.next()
            if (e.value === s) continue
            spriteBytes -= e.value.bmp.byteCount; it.remove()
        }
        return s
    }

    fun close() {
        closed = true
        worker.shutdownNow()
        overview = null
        chunks.clear()
        sprites.clear()
    }

    companion object {
        const val CHUNK_PX = 512f
        const val MAX_CHUNKS = 56
        const val SPRITE_BUDGET = 56L * 1024 * 1024
        val TERRAIN_LODS = intArrayOf(32, 64, 128, 192)
        val SPRITE_LODS = intArrayOf(32, 64, 128, 192)

        /** Ekranda karonun piksel genisligine gore LOD (biraz buyugunu sec: kucultmek keskin kalir) */
        fun lodFor(levels: IntArray, tilePx: Float): Int {
            for (l in levels) if (l >= tilePx * 0.92f) return l
            return levels.last()
        }
    }
}
