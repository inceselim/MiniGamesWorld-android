package com.eomer91.minigamesworld.ui.games.coloring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Boyama sayfalari (tools/vector/coloring.py ile uretildi). */
val COLORING_PAGES = listOf(
    "house" to R.drawable.coloring_house,
    "butterfly" to R.drawable.coloring_butterfly,
    "fish" to R.drawable.coloring_fish,
    "flower" to R.drawable.coloring_flower,
    "cupcake" to R.drawable.coloring_cupcake,
    "car" to R.drawable.coloring_car,
    "cat" to R.drawable.coloring_cat,
    "rocket" to R.drawable.coloring_rocket,
)

val COLORING_PALETTE = listOf(
    0xFFE5383B, 0xFFFF6B6B, 0xFFFF8FAB, 0xFFFFC2D1, 0xFFF77F00, 0xFFFFB347, 0xFFFFD166, 0xFFFFF3B0,
    0xFF80ED99, 0xFF38B000, 0xFF2D6A4F, 0xFFA7F3D0, 0xFF4CC9F0, 0xFF3A86FF, 0xFF1D3557, 0xFFBDE0FE,
    0xFFC77DFF, 0xFF7B2CBF, 0xFFB08968, 0xFF7F5539, 0xFFFFFFFF, 0xFFADB5BD, 0xFF495057, 0xFF000000,
)

data class ColoringUiState(
    /** null = sayfa secimi */
    val page: Int? = null,
    val image: ImageBitmap? = null,
    val color: Long = COLORING_PALETTE[0],
    val canUndo: Boolean = false,
    val version: Int = 0,
    /** Kayitli calismalarin kucuk resimleri (sayfa adi -> dosya var mi) */
    val saved: Set<String> = emptySet(),
    val goldEarned: Int = 0,
)

@HiltViewModel
class ColoringViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(ColoringUiState())
    val state: StateFlow<ColoringUiState> = _state.asStateFlow()

    private var engine: ColoringEngine? = null
    private var bitmap: Bitmap? = null
    private val lock = Mutex()
    private val goldTracker = GoldTracker()
    private var fills = 0
    private val dir: File get() = File(context.filesDir, "coloring").apply { mkdirs() }

    init {
        refreshSaved()
    }

    private fun refreshSaved() {
        viewModelScope.launch(Dispatchers.IO) {
            val names = COLORING_PAGES.map { it.first }.filter { File(dir, "$it.png").exists() }.toSet()
            _state.update { it.copy(saved = names) }
        }
    }

    fun savedFile(name: String): File = File(dir, "$name.png")

    fun open(index: Int) {
        viewModelScope.launch {
            val (name, res) = COLORING_PAGES[index]
            val result = withContext(Dispatchers.Default) {
                val opts = BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val line = BitmapFactory.decodeResource(context.resources, res, opts)
                val w = line.width
                val h = line.height
                val px = IntArray(w * h)
                line.getPixels(px, 0, w, 0, 0, w, h)
                val lum = IntArray(w * h) { px[it] and 0xFF }
                val savedPx = savedFile(name).takeIf { it.exists() }?.let { f ->
                    BitmapFactory.decodeFile(f.absolutePath, opts)?.takeIf { it.width == w && it.height == h }?.let { b ->
                        IntArray(w * h).also { arr -> b.getPixels(arr, 0, w, 0, 0, w, h) }
                    }
                }
                val e = ColoringEngine(w, h, lum, savedPx)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.setPixels(e.pixels, 0, w, 0, 0, w, h)
                e to bmp
            }
            engine = result.first
            bitmap = result.second
            _state.update { it.copy(page = index, image = result.second.asImageBitmap(), canUndo = false, version = it.version + 1) }
        }
    }

    fun close() {
        save()
        engine = null
        _state.update { it.copy(page = null, image = null) }
    }

    fun pickColor(c: Long) {
        _state.update { it.copy(color = c) }
        feedback.vibrate(Haptic.LIGHT)
    }

    /** Resim koordinatinda (0..genislik) dokunma. */
    fun tap(x: Int, y: Int) {
        val e = engine ?: return
        val color = _state.value.color.toInt()
        viewModelScope.launch {
            val changed = lock.withLock {
                withContext(Dispatchers.Default) {
                    if (e.fill(x, y, color)) {
                        bitmap?.setPixels(e.pixels, 0, e.width, 0, 0, e.width, e.height)
                        true
                    } else {
                        false
                    }
                }
            }
            if (changed) {
                fills++
                val delta = goldTracker.onScore(fills * 10)
                if (delta > 0) prefs.addGold(delta)
                _state.update { it.copy(canUndo = e.canUndo, version = it.version + 1, goldEarned = goldTracker.awarded) }
                feedback.play(Sfx.DROP, volume = 0.5f)
                if (fills % 5 == 0) save()
            }
        }
    }

    fun undo() {
        val e = engine ?: return
        viewModelScope.launch {
            lock.withLock {
                withContext(Dispatchers.Default) {
                    if (e.undo()) bitmap?.setPixels(e.pixels, 0, e.width, 0, 0, e.width, e.height)
                }
            }
            _state.update { it.copy(canUndo = e.canUndo, version = it.version + 1) }
            feedback.play(Sfx.SWIPE, volume = 0.4f)
        }
    }

    fun clearPage() {
        val e = engine ?: return
        viewModelScope.launch {
            lock.withLock {
                withContext(Dispatchers.Default) {
                    e.clear()
                    bitmap?.setPixels(e.pixels, 0, e.width, 0, 0, e.width, e.height)
                }
            }
            _state.update { it.copy(canUndo = false, version = it.version + 1) }
            save()
        }
    }

    private fun save() {
        val page = _state.value.page ?: return
        val bmp = bitmap ?: return
        val name = COLORING_PAGES[page].first
        val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { savedFile(name).outputStream().use { copy.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            refreshSaved()
        }
    }

    override fun onCleared() {
        // viewModelScope iptal edildigi icin dogrudan kaydet
        val page = _state.value.page
        val bmp = bitmap
        if (page != null && bmp != null) {
            runCatching { savedFile(COLORING_PAGES[page].first).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        super.onCleared()
    }
}
