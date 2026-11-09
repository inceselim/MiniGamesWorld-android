package com.eomer91.minigamesworld.ui.games.island

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.R
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Sfx
import com.eomer91.minigamesworld.ui.games.island.engine.BuildError
import com.eomer91.minigamesworld.ui.games.island.engine.Cat
import com.eomer91.minigamesworld.ui.games.island.engine.Dirty
import com.eomer91.minigamesworld.ui.games.island.engine.IsRivals
import com.eomer91.minigamesworld.ui.games.island.engine.IsCatalog
import com.eomer91.minigamesworld.ui.games.island.engine.IsGen
import com.eomer91.minigamesworld.ui.games.island.engine.IsSave
import com.eomer91.minigamesworld.ui.games.island.engine.IsSettings
import com.eomer91.minigamesworld.ui.games.island.engine.IsSim
import com.eomer91.minigamesworld.ui.games.island.engine.IsWorld
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Ada kaydi (yedekli, atomik) */
@Singleton
class IslandRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dir = File(context.filesDir, "island").apply { mkdirs() }
    private val file = File(dir, "game.sav")
    private val backup = File(dir, "game.bak")

    fun exists() = file.exists() || backup.exists()

    fun load(): IsWorld? {
        for (f in listOf(file, backup)) {
            if (!f.exists()) continue
            val w = runCatching { IsSave.read(ByteArrayInputStream(f.readBytes())) }.getOrNull()
            if (w != null) return w
        }
        return null
    }

    @Synchronized
    fun write(bytes: ByteArray) {
        val tmp = File(dir, "game.tmp")
        tmp.writeBytes(bytes)
        if (file.exists()) { backup.delete(); file.renameTo(backup) }
        tmp.renameTo(file)
    }

    fun delete() { file.delete(); backup.delete() }
}

enum class IsPhase { LOADING, START, SETUP, GAME }

enum class IsPanel { NONE, BUILD, BUILDING, LAND, FINANCE, MARKETING, RIVALS, NEWS, GUIDE, MENU }

/** Kisa bildirim: kaynak metin (+ arguman) ya da hazir metin */
data class IsToast(@StringRes val res: Int, val arg: String = "", val text: String? = null)

data class IsUiState(
    val phase: IsPhase = IsPhase.LOADING,
    /** Dunya degisti: yeniden ciz */
    val version: Int = 0,
    val hasSave: Boolean = false,
    val generating: Boolean = false,
    /** 0 durdu, 1..3 hiz */
    val speed: Int = 1,
    val panel: IsPanel = IsPanel.NONE,
    val buildCat: Cat = Cat.STAY,
    /** Yerlestirilen bina turu, yoksa -1 */
    val placing: Int = -1,
    val ghostX: Int = -1,
    val ghostY: Int = -1,
    val selBuilding: Int = -1,
    val selX: Int = -1,
    val selY: Int = -1,
    /** Okunan son haber sayisi (rozet icin) */
    val newsSeen: Int = 0,
    val gameOverSeen: Boolean = false,
    /** Kamerayi bir karoya goturme istegi (her istekte artar) */
    val focusSeq: Int = 0,
    val focusX: Float = 0f,
    val focusY: Float = 0f,
    val toast: IsToast? = null,
)

@HiltViewModel
class IslandViewModel @Inject constructor(
    private val repo: IslandRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    /** Oyun dunyasi. Yalnizca ana is parcaciginda okunur ve degistirilir. */
    var world: IsWorld? = null
        private set

    private val _state = MutableStateFlow(IsUiState())
    val state: StateFlow<IsUiState> = _state.asStateFlow()
    private var loop: Job? = null
    /** Disk yazimi: ViewModel kapanirken de tamamlanmali (viewModelScope iptal olur) */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        viewModelScope.launch {
            val has = withContext(Dispatchers.IO) { repo.exists() }
            _state.update { it.copy(phase = IsPhase.START, hasSave = has) }
        }
    }

    private fun bump() = _state.update { it.copy(version = it.version + 1) }
    fun toast(@StringRes res: Int, arg: String = "") = _state.update { it.copy(toast = IsToast(res, arg)) }
    fun toastText(text: String) = _state.update { it.copy(toast = IsToast(0, text = text)) }
    fun clearToast() = _state.update { it.copy(toast = null) }

    // ================================================================== oyun dongusu

    private fun startLoop() {
        loop?.cancel()
        loop = viewModelScope.launch {
            var sinceSave = 0
            while (isActive) {
                val sp = _state.value.speed
                delay(if (sp == 0) 200L else INTERVAL[sp])
                val w = world ?: continue
                if (_state.value.phase != IsPhase.GAME || _state.value.speed == 0 || w.gameOver) continue
                IsSim.step(w)
                if (w.gameOver) { _state.update { it.copy(speed = 0) }; feedback.play(Sfx.GAME_OVER) }
                if (++sinceSave >= 30) { sinceSave = 0; save() }
                bump()
            }
        }
    }

    fun setSpeed(s: Int) = _state.update { it.copy(speed = s.coerceIn(0, 3)) }

    // ================================================================== kayit / yeni oyun

    fun load() {
        _state.update { it.copy(phase = IsPhase.LOADING) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { repo.load() }
            if (w == null) {
                _state.update { it.copy(phase = IsPhase.START, hasSave = false, toast = IsToast(R.string.is_load_failed)) }
                return@launch
            }
            world = w
            _state.update { IsUiState(phase = IsPhase.GAME, hasSave = true, newsSeen = w.newsCount, gameOverSeen = w.gameOver) }
            startLoop()
        }
    }

    fun openSetup() = _state.update { it.copy(phase = IsPhase.SETUP) }
    fun backToStart() = _state.update { it.copy(phase = IsPhase.START) }

    fun newGame(s: IsSettings) {
        if (_state.value.generating) return
        _state.update { it.copy(generating = true) }
        viewModelScope.launch {
            s.seed = System.nanoTime()
            val w = withContext(Dispatchers.Default) { IsGen.newGame(s) }
            world = w
            // Ilk oyunda rehber acik ve oyun durmus baslar
            _state.update { IsUiState(phase = IsPhase.GAME, hasSave = true, speed = 0, panel = IsPanel.GUIDE) }
            save()
            startLoop()
        }
    }

    /** Dunyayi ana is parcaciginda bayta cevirir, diske arka planda yazar */
    fun save(showToast: Boolean = false) {
        val w = world ?: return
        val bytes = ByteArrayOutputStream().also { IsSave.write(w, it) }.toByteArray()
        ioScope.launch { runCatching { repo.write(bytes) } }
        if (showToast) toast(R.string.is_saved)
    }

    fun exitGame() {
        save()
        loop?.cancel()
        world = null
        _state.update { IsUiState(phase = IsPhase.START, hasSave = true) }
    }

    // ================================================================== paneller ve harita

    fun openPanel(p: IsPanel) = _state.update {
        val np = if (it.panel == p) IsPanel.NONE else p
        it.copy(panel = np, placing = -1, ghostX = -1, ghostY = -1,
            newsSeen = if (np == IsPanel.NEWS) (world?.newsCount ?: 0) else it.newsSeen)
    }

    fun closePanel() = _state.update { it.copy(panel = IsPanel.NONE, selBuilding = -1, selX = -1, selY = -1) }
    fun setBuildCat(c: Cat) = _state.update { it.copy(buildCat = c) }
    fun markNewsSeen() = _state.update { it.copy(newsSeen = world?.newsCount ?: 0) }
    fun dismissGameOver() = _state.update { it.copy(gameOverSeen = true) }

    fun startPlacing(typeId: Int) = _state.update { it.copy(placing = typeId, panel = IsPanel.NONE, ghostX = -1, ghostY = -1, selBuilding = -1) }
    fun cancelPlacing() = _state.update { it.copy(placing = -1, ghostX = -1, ghostY = -1) }

    fun tapMap(x: Int, y: Int, buildingId: Int) {
        val w = world ?: return
        val st = _state.value
        if (st.placing >= 0) {
            val t = IsCatalog[st.placing]
            // Dokunulan karo binanin ortasi olsun
            val gx = (x - (t.w - 1) / 2).coerceIn(0, w.n - t.w)
            val gy = (y - (t.h - 1) / 2).coerceIn(0, w.n - t.h)
            _state.update { it.copy(ghostX = gx, ghostY = gy) }
            return
        }
        val id = if (buildingId >= 0) buildingId else w.bAt[w.idx(x, y)]
        if (id >= 0 && w.buildings.getOrNull(id) != null) {
            _state.update { it.copy(panel = IsPanel.BUILDING, selBuilding = id, selX = -1, selY = -1) }
        } else {
            _state.update { it.copy(panel = IsPanel.LAND, selBuilding = -1, selX = x, selY = y) }
        }
    }

    // ================================================================== eylemler

    fun canPlace(): BuildError {
        val w = world ?: return BuildError.OUTSIDE
        val st = _state.value
        if (st.placing < 0 || st.ghostX < 0) return BuildError.OUTSIDE
        return IsSim.canBuild(w, w.userId, IsCatalog[st.placing], st.ghostX, st.ghostY)
    }

    fun confirmPlace() {
        val w = world ?: return
        val st = _state.value
        if (st.placing < 0 || st.ghostX < 0) return
        val t = IsCatalog[st.placing]
        val b = IsSim.build(w, w.userId, t, st.ghostX, st.ghostY)
        if (b == null) { toast(buildErrorRes(IsSim.canBuild(w, w.userId, t, st.ghostX, st.ghostY))); return }
        feedback.play(Sfx.DROP, volume = 0.7f)
        // Ayni turden bir tane daha koymaya hazir; onizleme kalkar
        _state.update { it.copy(ghostX = -1, ghostY = -1, version = it.version + 1) }
    }

    fun buyLand() {
        val w = world ?: return
        val st = _state.value
        if (st.selX < 0) return
        if (IsSim.buyLand(w, w.userId, st.selX, st.selY)) { toast(R.string.is_land_bought); bump() }
        else toast(R.string.is_err_no_money)
    }

    fun setPrice(id: Int, p: Float) {
        val b = world?.buildings?.getOrNull(id) ?: return
        if (b.owner != 0) return
        IsSim.setPrice(b, p); bump()
    }

    fun upgrade(id: Int) {
        val w = world ?: return
        val b = w.buildings.getOrNull(id) ?: return
        if (b.owner != w.userId) return
        if (IsSim.upgrade(w, b)) { feedback.play(Sfx.DROP, volume = 0.7f); toast(R.string.is_upgraded); bump() } else toast(R.string.is_err_no_money)
    }

    fun demolish(id: Int) {
        val w = world ?: return
        val b = w.buildings.getOrNull(id) ?: return
        if (b.owner != w.userId) return
        IsSim.demolish(w, b)
        feedback.play(Sfx.CLEAR, volume = 0.6f)
        _state.update { it.copy(panel = IsPanel.NONE, selBuilding = -1, version = it.version + 1, toast = IsToast(R.string.is_demolished)) }
    }

    fun takeLoan(amount: Long, years: Int) {
        val w = world ?: return
        if (IsSim.takeLoan(w, w.userId, amount, years)) { toast(R.string.is_loan_taken, IsFmt.money(amount)); bump() }
        else toast(R.string.is_no_credit)
    }

    fun repayLoan() {
        val w = world ?: return
        if (IsSim.repayLoan(w, w.userId)) { toast(R.string.is_loan_repaid); bump() } else toast(R.string.is_err_no_money)
    }

    // ================================================================== rekabet

    fun startAd(cls: Int) {
        val w = world ?: return
        if (IsRivals.startAd(w, w.userId, cls)) { feedback.play(Sfx.COMBO, volume = 0.6f); toast(R.string.is_ad_started); bump() } else toast(R.string.is_err_no_money)
    }

    /** target: SABOTAGE icin bina id'si, digerleri icin sirket id'si */
    fun dirty(d: Dirty, target: Int) {
        val w = world ?: return
        when (IsRivals.doDirty(w, w.userId, d, target)) {
            1 -> { feedback.play(Sfx.COMBO); toast(R.string.is_d_ok) }
            0 -> { feedback.play(Sfx.BLOCK, volume = 0.6f); toast(R.string.is_d_fail) }
            2 -> { feedback.play(Sfx.GAME_OVER, volume = 0.7f); toast(R.string.is_d_caught) }
            else -> toast(R.string.is_err_no_money)
        }
        bump()
    }

    fun bid(auctionId: Int) {
        val w = world ?: return
        val a = w.auctions.firstOrNull { it.id == auctionId } ?: return
        if (IsRivals.bid(w, w.userId, a, IsRivals.nextBid(a))) { feedback.play(Sfx.BLOCK, volume = 0.6f); toast(R.string.is_bid_ok); bump() } else toast(R.string.is_bid_fail)
    }

    fun focus(x: Float, y: Float) = _state.update { it.copy(focusSeq = it.focusSeq + 1, focusX = x, focusY = y) }

    /** Haritada binayi goster ve panelini ac */
    fun showBuilding(id: Int) {
        val b = world?.buildings?.getOrNull(id) ?: return
        _state.update { it.copy(panel = IsPanel.BUILDING, selBuilding = id, selX = -1, selY = -1, focusSeq = it.focusSeq + 1, focusX = b.cx, focusY = b.cy) }
    }

    override fun onCleared() {
        save()
        super.onCleared()
    }

    companion object {
        /** Gun basina sure (ms): 1x, 2x, 3x */
        val INTERVAL = longArrayOf(0, 1100, 450, 140)

        @StringRes
        fun buildErrorRes(e: BuildError): Int = when (e) {
            BuildError.OK -> R.string.is_confirm_build
            BuildError.OUTSIDE -> R.string.is_err_outside
            BuildError.WATER -> R.string.is_err_water
            BuildError.OCCUPIED -> R.string.is_err_occupied
            BuildError.NOT_YOURS -> R.string.is_err_not_yours
            BuildError.NEEDS_BEACH -> R.string.is_err_needs_beach
            BuildError.NO_MONEY -> R.string.is_err_no_money
            BuildError.NOT_BUILDABLE -> R.string.is_err_not_buildable
        }
    }
}
