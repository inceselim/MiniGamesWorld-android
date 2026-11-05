package com.eomer91.minigamesworld.ui.games.mahjong

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class MahjongPhase { HOME, PLAY, WON, NEW_CHAPTER }

/** Kalici ilerleme. Mevcut tahta da burada saklanir (oyuna kaldigi yerden devam). */
@Serializable
data class MahjongProfile(
    val version: Int = 1,
    /** Siradaki oynanacak seviye. */
    val level: Int = 1,
    val totalStars: Int = 0,
    val hints: Int = START_HINTS,
    val shuffles: Int = START_SHUFFLES,
    val highlightFree: Boolean = true,
    val current: MahjongSave? = null,
) {
    companion object {
        const val START_HINTS = 3
        const val START_SHUFFLES = 3
    }
}

data class MahjongUiState(
    val loading: Boolean = true,
    val phase: MahjongPhase = MahjongPhase.HOME,
    val profile: MahjongProfile = MahjongProfile(),
    val seconds: Int = 0,
    val pairsLeft: Int = 0,
    val canUndo: Boolean = false,
    /** Acik eslesme kalmadi: karistirma onerilir. */
    val stuck: Boolean = false,
    // Bitis ekrani
    val stars: Int = 0,
    val goldEarned: Int = 0,
    /** Bilgi baloncugu (ornek: "ipucu kalmadi"). */
    val toast: MahjongToast? = null,
    /** Tahta nesnesi her degistiginde (yeni seviye, karistirma) artar; ekran yeni tahtayi alir. */
    val round: Int = 0,
)

enum class MahjongToast { NO_HINTS, NO_SHUFFLES, AD_NOT_READY, REWARD_HINTS, REWARD_SHUFFLES }

/** Ekranda cizilen gecici efektler (zaman damgasi ms). */
class MahjongFx {
    /** Eslesip kaybolan taslar: id -> baslangic zamani */
    val vanishing = LinkedHashMap<Int, Long>()
    /** Kilitli tasa dokununca titreme */
    var shakeId = -1
    var shakeAt = 0L
    /** Ipucu parlamasi */
    var hintA = -1
    var hintB = -1
    var hintAt = 0L
    /** Karistirma animasyonu */
    var shuffleAt = 0L
    /** Seviye basi giris animasyonu */
    var enterAt = 0L

    fun busy(now: Long): Boolean =
        vanishing.values.any { now - it < VANISH_MS } || now - shakeAt < SHAKE_MS || now - hintAt < HINT_MS ||
            now - shuffleAt < SHUFFLE_MS || now - enterAt < ENTER_MS

    companion object {
        const val VANISH_MS = 420L
        const val SHAKE_MS = 320L
        const val HINT_MS = 2400L
        const val SHUFFLE_MS = 520L
        const val ENTER_MS = 700L
    }
}

@HiltViewModel
class MahjongViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(MahjongUiState())
    val state: StateFlow<MahjongUiState> = _state.asStateFlow()

    var game: MahjongGame? = null
        private set

    val fx = MahjongFx()

    /** Her degisiklikte artar; tuval yeniden cizilir. */
    var frame by mutableLongStateOf(0L)
        private set

    private var timer: Job? = null
    private var elapsedMs = 0L
    private var combo = 0
    private var lastMatchAt = 0L
    private val rnd = SeededRandom(System.nanoTime())

    init {
        viewModelScope.launch {
            val p = prefs.savedGame(PROFILE_KEY).first()?.let {
                runCatching { json.decodeFromString(MahjongProfile.serializer(), it) }.getOrNull()
            } ?: MahjongProfile()
            _state.update { it.copy(loading = false, profile = p) }
        }
    }

    // ------------------------------------------------------------------ Akis

    /** Ana ekrandaki "Oyna": kayitli tahta varsa ondan, yoksa siradaki seviyeden. */
    fun play() {
        val p = _state.value.profile
        val saved = p.current?.takeIf { it.level == p.level }?.let { MahjongGame.restore(it) }
        val g = saved ?: MahjongGame.forLevel(p.level)
        elapsedMs = if (saved != null) p.current?.elapsedMs ?: 0L else 0L
        start(g)
    }

    private fun start(g: MahjongGame) {
        game = g
        combo = 0
        fx.vanishing.clear()
        fx.hintA = -1
        fx.enterAt = now()
        _state.update {
            it.copy(
                phase = MahjongPhase.PLAY,
                seconds = (elapsedMs / 1000).toInt(),
                pairsLeft = g.remaining / 2,
                canUndo = g.canUndo,
                stuck = !g.hasMoves(),
                toast = null,
                round = it.round + 1,
            )
        }
        startTimer()
        frame++
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(250)
                val t = System.currentTimeMillis()
                if (_state.value.phase == MahjongPhase.PLAY && paused.not()) {
                    elapsedMs += t - last
                    val s = (elapsedMs / 1000).toInt()
                    if (s != _state.value.seconds) _state.update { it.copy(seconds = s) }
                }
                last = t
            }
        }
    }

    private var paused = false
    fun setPaused(p: Boolean) {
        paused = p
        if (p) persist()
    }

    fun goHome() {
        persist()
        timer?.cancel()
        game = null
        _state.update { it.copy(phase = MahjongPhase.HOME, toast = null) }
    }

    /** Bitis ekranindan sonraki seviyeye (gerekirse once yeni bolum ekrani). */
    fun next() {
        val p = _state.value.profile
        if (_state.value.phase == MahjongPhase.WON && (p.level - 1) % MahjongLevels.CHAPTER_SIZE == 0 && p.level > 1) {
            _state.update { it.copy(phase = MahjongPhase.NEW_CHAPTER) }
            return
        }
        elapsedMs = 0
        start(MahjongGame.forLevel(p.level))
    }

    /** Seviyeyi bastan baslat. */
    fun restart() {
        val g = game ?: return
        elapsedMs = 0
        start(MahjongGame.forLevel(g.info.level))
        persist()
    }

    // ------------------------------------------------------------------ Oyun

    fun tap(id: Int) {
        val g = game ?: return
        if (_state.value.phase != MahjongPhase.PLAY) return
        if (fx.vanishing.containsKey(id)) return
        when (val r = g.tap(id)) {
            MahjongGame.TapResult.Blocked -> {
                fx.shakeId = id; fx.shakeAt = now()
                feedback.vibrate(Haptic.LIGHT)
            }
            MahjongGame.TapResult.Selected, is MahjongGame.TapResult.Mismatch -> feedback.play(Sfx.SWIPE, volume = 0.45f)
            MahjongGame.TapResult.Deselected -> Unit
            is MahjongGame.TapResult.Matched -> onMatched(g, r.a, r.b)
        }
        frame++
    }

    private fun onMatched(g: MahjongGame, a: Int, b: Int) {
        val t = now()
        fx.vanishing[a] = t; fx.vanishing[b] = t
        if (fx.hintA == a || fx.hintA == b || fx.hintB == a || fx.hintB == b) fx.hintA = -1
        combo = if (t - lastMatchAt < 2500) combo + 1 else 1
        lastMatchAt = t
        feedback.play(if (combo >= 3) Sfx.COMBO else Sfx.CLEAR, volume = 0.7f)
        feedback.vibrate(Haptic.LIGHT)
        _state.update { it.copy(pairsLeft = g.remaining / 2, canUndo = g.canUndo) }
        if (g.isCleared) {
            viewModelScope.launch {
                delay(MahjongFx.VANISH_MS + 150)
                win()
            }
        } else {
            _state.update { it.copy(stuck = !g.hasMoves()) }
            if (g.remaining % 8 == 0) persist()
        }
    }

    fun undo() {
        val g = game ?: return
        if (_state.value.phase != MahjongPhase.PLAY) return
        val pair = g.undo() ?: return
        fx.vanishing.remove(pair.first); fx.vanishing.remove(pair.second)
        feedback.play(Sfx.SWIPE, volume = 0.5f)
        _state.update { it.copy(pairsLeft = g.remaining / 2, canUndo = g.canUndo, stuck = !g.hasMoves()) }
        frame++
    }

    fun hint() {
        val g = game ?: return
        if (_state.value.phase != MahjongPhase.PLAY) return
        val p = _state.value.profile
        if (p.hints <= 0) { toast(MahjongToast.NO_HINTS); return }
        val pair = g.useHint() ?: run { _state.update { it.copy(stuck = true) }; return }
        fx.hintA = pair.first; fx.hintB = pair.second; fx.hintAt = now()
        updateProfile { it.copy(hints = it.hints - 1) }
        feedback.play(Sfx.DROP, volume = 0.5f)
        frame++
    }

    fun shuffle() {
        val g = game ?: return
        if (_state.value.phase != MahjongPhase.PLAY) return
        val p = _state.value.profile
        if (p.shuffles <= 0) { toast(MahjongToast.NO_SHUFFLES); return }
        val ng = g.shuffle(rnd)
        if (ng === g) return
        game = ng
        fx.shuffleAt = now(); fx.hintA = -1
        updateProfile { it.copy(shuffles = it.shuffles - 1) }
        feedback.play(Sfx.SWIPE, volume = 0.8f)
        _state.update { it.copy(canUndo = false, stuck = !ng.hasMoves(), round = it.round + 1) }
        persist()
        frame++
    }

    fun rewardHints() { updateProfile { it.copy(hints = it.hints + 3) }; toast(MahjongToast.REWARD_HINTS) }
    fun rewardShuffles() { updateProfile { it.copy(shuffles = it.shuffles + 2) }; toast(MahjongToast.REWARD_SHUFFLES) }
    fun adNotReady() = toast(MahjongToast.AD_NOT_READY)

    fun toggleHighlight() { updateProfile { it.copy(highlightFree = !it.highlightFree) }; frame++ }

    private fun toast(t: MahjongToast) {
        _state.update { it.copy(toast = t) }
        viewModelScope.launch {
            delay(2200)
            _state.update { if (it.toast == t) it.copy(toast = null) else it }
        }
    }

    /** Animasyon karesi: efektler surerken ekran her karede cagirir. */
    fun tick() {
        val t = now()
        fx.vanishing.entries.removeAll { t - it.value > MahjongFx.VANISH_MS + 50 && game?.isRemoved(it.key) == true }
        frame++
    }

    fun animating(): Boolean = fx.busy(now()) || fx.vanishing.isNotEmpty()

    // ------------------------------------------------------------------ Bitis

    private suspend fun win() {
        val g = game ?: return
        timer?.cancel()
        val secs = (elapsedMs / 1000).toInt()
        val par = g.info.tiles * 2.4f + 25
        val clean = g.hintsUsed == 0 && g.shufflesUsed == 0
        val stars = when {
            secs <= par && clean -> 3
            secs <= par * 1.7f -> 2
            else -> 1
        }
        val gold = 4 + stars * 2 + g.info.tiles / 24
        val level = g.info.level
        // Her 3 seviyede bir ipucu, her 5 seviyede bir karistirma hediye
        updateProfile {
            it.copy(
                level = level + 1,
                totalStars = it.totalStars + stars,
                hints = it.hints + if (level % 3 == 0) 1 else 0,
                shuffles = it.shuffles + if (level % 5 == 0) 1 else 0,
                current = null,
            )
        }
        game = null
        _state.update { it.copy(phase = MahjongPhase.WON, stars = stars, goldEarned = gold, stuck = false) }
        feedback.play(Sfx.COMBO)
        prefs.addGold(gold)
        prefs.submitScore(GAME_ID, level)
        save()
    }

    // ------------------------------------------------------------------ Kayit

    private fun updateProfile(f: (MahjongProfile) -> MahjongProfile) {
        _state.update { it.copy(profile = f(it.profile)) }
        save()
    }

    /** Mevcut tahtayi profile yaz. */
    private fun persist() {
        val g = game ?: return
        if (g.isCleared) return
        _state.update { it.copy(profile = it.profile.copy(current = g.snapshot().copy(elapsedMs = elapsedMs))) }
        save()
    }

    private fun save() {
        val text = json.encodeToString(MahjongProfile.serializer(), _state.value.profile)
        viewModelScope.launch { withContext(NonCancellable) { prefs.saveGame(PROFILE_KEY, text) } }
    }

    override fun onCleared() {
        // viewModelScope kapanmis olur: son durumu burada esitlemeli yaz.
        game?.takeIf { !it.isCleared }?.let { g ->
            val p = _state.value.profile.copy(current = g.snapshot().copy(elapsedMs = elapsedMs))
            runBlocking { prefs.saveGame(PROFILE_KEY, json.encodeToString(MahjongProfile.serializer(), p)) }
        }
        super.onCleared()
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        const val GAME_ID = "mahjong"
        /** Oyun kimliginden farkli: ana menude "Devam et" dugmesi cikmasin. */
        private const val PROFILE_KEY = "mahjong-profile"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
