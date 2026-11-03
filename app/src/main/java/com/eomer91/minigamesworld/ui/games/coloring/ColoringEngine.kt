package com.eomer91.minigamesworld.ui.games.coloring

/*
 * Boyama: kova ile doldurma (Android'den bagimsiz).
 * [lum] cizgi resmin parlakligi (0 siyah .. 255 beyaz). Koyu pikseller duvardir.
 * Boyanan piksel = secilen renk x parlaklik (carpma) -> kenar yumusatmasi korunur.
 */
class ColoringEngine(val width: Int, val height: Int, private val lum: IntArray, initial: IntArray? = null) {

    val pixels: IntArray = initial?.copyOf() ?: IntArray(width * height) { gray(lum[it]) }

    private class Fill(val indices: IntArray, val previous: IntArray)

    private val history = ArrayDeque<Fill>()

    val canUndo: Boolean get() = history.isNotEmpty()

    fun isWall(i: Int) = lum[i] < WALL

    /** (x, y) noktasindaki bolgeyi [color] ile doldur. Degisiklik olduysa true. */
    fun fill(x: Int, y: Int, color: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        var start = y * width + x
        if (isWall(start)) {
            // Cizginin hemen yanina dokunulduysa en yakin bos pikseli bul
            start = nearestOpen(x, y) ?: return false
        }
        val target = shadeOf(color, lum[start])
        if (pixels[start] == target) return false

        val visited = BooleanArray(width * height)
        val out = IntArrayList()
        val stack = IntArrayList()
        stack.add(start)
        while (stack.size > 0) {
            val i = stack.removeLast()
            val yy = i / width
            var lx = i % width
            val row = yy * width
            if (visited[i] || isWall(i)) continue
            // Satirda sola ve saga uzan
            while (lx > 0 && !visited[row + lx - 1] && !isWall(row + lx - 1)) lx--
            var xx = lx
            var upOpen = false
            var downOpen = false
            while (xx < width && !visited[row + xx] && !isWall(row + xx)) {
                val k = row + xx
                visited[k] = true
                out.add(k)
                if (yy > 0) {
                    val u = k - width
                    val open = !visited[u] && !isWall(u)
                    if (open && !upOpen) stack.add(u)
                    upOpen = open
                }
                if (yy < height - 1) {
                    val d = k + width
                    val open = !visited[d] && !isWall(d)
                    if (open && !downOpen) stack.add(d)
                    downOpen = open
                }
                xx++
            }
        }
        val idx = out.toArray()
        val prev = IntArray(idx.size) { pixels[idx[it]] }
        for (k in idx) pixels[k] = shadeOf(color, lum[k])
        history.addLast(Fill(idx, prev))
        while (history.size > MAX_UNDO) history.removeFirst()
        return true
    }

    fun undo(): Boolean {
        val f = history.removeLastOrNull() ?: return false
        for (n in f.indices.indices) pixels[f.indices[n]] = f.previous[n]
        return true
    }

    fun clear() {
        for (i in pixels.indices) pixels[i] = gray(lum[i])
        history.clear()
    }

    /** Boyanmis (beyaz olmayan, duvar olmayan) piksel orani. */
    fun coloredRatio(): Float {
        var open = 0
        var colored = 0
        for (i in pixels.indices) {
            if (isWall(i)) continue
            open++
            if (pixels[i] != gray(lum[i])) colored++
        }
        return if (open == 0) 0f else colored.toFloat() / open
    }

    private fun nearestOpen(x: Int, y: Int): Int? {
        for (r in 1..6) for (dy in -r..r) for (dx in -r..r) {
            val xx = x + dx
            val yy = y + dy
            if (xx !in 0 until width || yy !in 0 until height) continue
            val i = yy * width + xx
            if (!isWall(i)) return i
        }
        return null
    }

    companion object {
        const val WALL = 110
        const val MAX_UNDO = 30

        fun gray(l: Int) = (0xFF shl 24) or (l shl 16) or (l shl 8) or l

        fun shadeOf(color: Int, l: Int): Int {
            val r = ((color shr 16) and 0xFF) * l / 255
            val g = ((color shr 8) and 0xFF) * l / 255
            val b = (color and 0xFF) * l / 255
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}

/** Kutulamasiz buyuyen Int listesi. */
private class IntArrayList {
    private var data = IntArray(1024)
    var size = 0
        private set

    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    fun removeLast(): Int = data[--size]
    fun toArray(): IntArray = data.copyOf(size)
}
