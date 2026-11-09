package com.eomer91.minigamesworld.ui.games.island.render

import com.eomer91.minigamesworld.ui.games.island.engine.Difficulty
import com.eomer91.minigamesworld.ui.games.island.engine.Goal
import com.eomer91.minigamesworld.ui.games.island.engine.IsCatalog
import com.eomer91.minigamesworld.ui.games.island.engine.IsGen
import com.eomer91.minigamesworld.ui.games.island.engine.IsSettings
import com.eomer91.minigamesworld.ui.games.island.engine.IsSim
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import com.eomer91.minigamesworld.ui.games.island.engine.MapSize
import com.eomer91.minigamesworld.ui.games.island.engine.MapType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Masaustu cizici: Android Canvas olmadan sanati gorebilmek icin */
class AwtPainter(val g: Graphics2D) : IsoPainter {
    private fun col(c: Int) = Color(c, true)
    override fun poly(xs: FloatArray, ys: FloatArray, color: Int) {
        val p = Path2D.Float(); p.moveTo(xs[0], ys[0]); for (i in 1 until xs.size) p.lineTo(xs[i], ys[i]); p.closePath()
        g.color = col(color); g.fill(p)
    }
    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        g.color = col(color); g.stroke = BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); g.draw(Line2D.Float(x1, y1, x2, y2))
    }
    override fun oval(l: Float, t: Float, r: Float, b: Float, color: Int) { g.color = col(color); g.fill(Ellipse2D.Float(l, t, r - l, b - t)) }
    override fun ring(cx: Float, cy: Float, rx: Float, ry: Float, width: Float, color: Int) {
        g.color = col(color); g.stroke = BasicStroke(width); g.draw(Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2))
    }
}

class IsoPreviewTest {

    private fun out(name: String) = File(System.getProperty("isPreviewDir") ?: "build/is-preview").also { it.mkdirs() }.resolve(name)

    private fun img(wpx: Int, hpx: Int, bg: Int = 0xFF1A5B96.toInt()): Pair<BufferedImage, Graphics2D> {
        val im = BufferedImage(wpx, hpx, BufferedImage.TYPE_INT_ARGB)
        val g = im.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(bg, true); g.fillRect(0, 0, wpx, hpx)
        return im to g
    }

    /** Tum bina turleri yan yana (sanat kontrolu) */
    @Test
    fun catalogSheet() {
        val tw = 64f; val th = 32f
        val cols = 6; val cell = 300
        val rows = (IsCatalog.types.size + cols - 1) / cols
        val (im, g) = img(cols * cell, rows * cell, 0xFF6BAF46.toInt())
        val p = AwtPainter(g)
        for ((i, t) in IsCatalog.types.withIndex()) {
            val cx = (i % cols) * cell + cell / 2f; val cy = (i / cols) * cell + cell - 20f - (t.w + t.h) * th / 2f
            val oy = cy
            Iso(p, cx, oy, tw, th).flat(0f, 0f, t.w.toFloat(), t.h.toFloat(), 0f, 0xFFF2DBA2.toInt())
            IsoArt.building(p, t, cx, oy, tw, th, 0xFFE53935.toInt())
            g.color = Color.BLACK; g.drawString(t.key, (i % cols) * cell + 8, (i / cols) * cell + 16)
        }
        // Insaat alani
        IsoArt.construction(p, 3, 3, cols * cell - cell / 2f, rows * cell - 160f, tw, th, 0.5f, 0xFF1E88E5.toInt())
        g.dispose()
        ImageIO.write(im, "png", out("catalog.png"))
    }

    private fun simWorld(size: MapSize, type: MapType, years: Int, seed: Long): IsWorld {
        val w = IsGen.newGame(IsSettings(companyName = "Player", bots = 4, mapType = type, mapSize = size, difficulty = Difficulty.NORMAL, seed = seed))
        w.settings.goal = Goal.ENDLESS
        repeat(360 * years) { IsSim.step(w) }
        return w
    }

    @Test
    fun cityPreview() {
        for ((name, type) in listOf("island" to MapType.ISLAND, "arch" to MapType.ARCHIPELAGO)) {
            val w = simWorld(MapSize.SMALL, type, 4, 5)
            val L = IsoLayout(w.n, 32f, 16f, 220f)
            val (im, g) = img(L.width.toInt(), L.height.toInt() + 20)
            IsoScene.drawAll(AwtPainter(g), w, L)
            g.dispose()
            ImageIO.write(im, "png", out("city_$name.png"))
            // Yakin plan: en kalabalik bolge, 2x
            val bs = w.buildings.filterNotNull()
            assertTrue(bs.isNotEmpty())
            val mx = bs.map { it.cx }.average().toFloat(); val my = bs.map { it.cy }.average().toFloat()
            val Z = IsoLayout(w.n, 64f, 32f, 300f)
            val vw = 1400; val vh = 900
            val (im2, g2) = img(vw, vh)
            g2.translate(-(Z.tileX(mx, my) - vw / 2.0), -(Z.tileY(mx, my) - vh / 2.0))
            IsoScene.drawAll(AwtPainter(g2), w, Z)
            g2.dispose()
            ImageIO.write(im2, "png", out("zoom_$name.png"))
        }
    }

    /** Uzun binanin govdesine dokunmak arkadaki karoyu degil binayi secmeli */
    @Test
    fun pickTallBuilding() {
        val w = simWorld(MapSize.SMALL, MapType.ISLAND, 3, 5)
        val list = IsoScene.snapshot(w)
        val L = IsoLayout(w.n, 64f, 32f, 260f)
        var ok = 0; var tried = 0
        for (b in list) {
            val fx = b.x + b.type.w / 2f; val fy = b.y + b.type.h / 2f
            // Gorunur on yuzde, zeminin biraz ustu
            val px = L.tileX(fx, fy); val py = L.tileY(fx, fy) + (b.type.w + b.type.h) * 4f - 6f
            val hit = IsoScene.pick(list, L, px, py) ?: continue
            tried++
            if (hit.id == b.id) ok++
        }
        println("pick $ok/$tried")
        // Iskaliklar: ondeki yuksek komsu binanin govdesi noktayi ortuyor (dogru davranis)
        assertTrue(tried > 10 && ok >= tried * 0.65)
    }

    @Test
    fun tapMappingRoundTrip() {
        val L = IsoLayout(96, 64f, 32f, 200f)
        for (x in listOf(0, 5, 40, 95)) for (y in listOf(0, 7, 60, 95)) {
            val cx = L.tileX(x + 0.5f, y + 0.5f); val cy = L.tileY(x + 0.5f, y + 0.5f)
            val (tx, ty) = L.toTile(cx, cy)
            assertTrue("($x,$y) -> ($tx,$ty)", tx == x && ty == y)
        }
    }
}
