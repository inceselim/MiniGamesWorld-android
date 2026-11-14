package com.eomer91.minigamesworld.ui.games.kingdom

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class KingdomTab { KINGDOM, ARMY, MAP, RESEARCH, QUESTS }

data class KingdomUiState(
    val loading: Boolean = true,
    val tab: KingdomTab = KingdomTab.KINGDOM,
    /** Cevrimdisi kazanc penceresi (bos = gosterme) */
    val offline: Map<Res, Double> = emptyMap(),
    val offlineSeconds: Long = 0,
    val report: BattleReport? = null,
    /** Kisa bilgi mesaji: gorev odulu vb. */
    val toast: Map<Res, Int>? = null,
)

@HiltViewModel
class KingdomViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(KingdomUiState())
    val state: StateFlow<KingdomUiState> = _state.asStateFlow()

    var engine = KingdomEngine()
        private set

    /** Her degisiklikte artar; ekran bunu okuyup yeniden cizer. */
    var frame by mutableLongStateOf(0L)
        private set

    private var saveTimer = 0.0

    /** Son tick'in duvar saati; uygulama arka plandayken gecen sure icin. */
    private var lastTickAt = 0L

    init {
        viewModelScope.launch {
            val save = prefs.savedGame(PROFILE_KEY).first()
                ?.let { runCatching { json.decodeFromString(KingdomSave.serializer(), it) }.getOrNull() }
            val e = KingdomEngine(save ?: KingdomSave())
            var gained: Map<Res, Double> = emptyMap()
            var seconds = 0L
            if (save != null && save.savedAt > 0) {
                seconds = ((System.currentTimeMillis() - save.savedAt) / 1000).coerceAtLeast(0)
                gained = e.offline(seconds)
            }
            engine = e
            _state.update { it.copy(loading = false, offline = gained, offlineSeconds = seconds) }
            frame++
            // Cevrimdisi kazanc hemen kaydedilsin ki uygulama kapanirsa iki kez verilmesin.
            save()
        }
    }

    /**
     * Ekran geri geldiginde cagrilir: ViewModel yasarken uygulama arka planda kaldiysa
     * gecen sureyi cevrimdisi uretim olarak ekler (ayni sinirlarla).
     */
    fun onResume() {
        if (_state.value.loading || lastTickAt == 0L) return
        val now = System.currentTimeMillis()
        val gap = (now - lastTickAt) / 1000
        lastTickAt = now
        if (gap < 60) return
        val gained = engine.offline(gap)
        if (gained.isNotEmpty() && _state.value.offline.isEmpty()) {
            _state.update { it.copy(offline = gained, offlineSeconds = gap) }
        }
        frame++
        save()
    }

    fun selectTab(t: KingdomTab) = _state.update { it.copy(tab = t) }

    fun tick(dt: Double) {
        if (_state.value.loading) return
        lastTickAt = System.currentTimeMillis()
        engine.produce(dt)
        frame++
        saveTimer += dt
        if (saveTimer > 10) {
            saveTimer = 0.0
            save()
        }
    }

    private fun ok(sfx: Sfx = Sfx.CLEAR) {
        feedback.play(sfx, volume = 0.6f)
        feedback.vibrate(Haptic.LIGHT)
        frame++
        save()
    }

    private fun fail() = feedback.vibrate(Haptic.HEAVY)

    fun upgrade(b: KBuilding) = if (engine.upgrade(b)) ok() else fail()

    fun train(u: UnitType, count: Int) = if (engine.train(u, count) > 0) ok(Sfx.DROP) else fail()

    fun selectHero(h: Hero) {
        engine.selectHero(h)
        frame++
        save()
    }

    fun upgradeHero(h: Hero) = if (engine.upgradeHero(h)) ok() else fail()

    fun research(t: Tech) = if (engine.research(t)) ok() else fail()

    fun attack() {
        val r = engine.attack() ?: return fail()
        feedback.play(if (r.won) Sfx.COMBO else Sfx.GAME_OVER)
        feedback.vibrate(Haptic.HEAVY)
        _state.update { it.copy(report = r) }
        frame++
        save()
    }

    fun closeReport() = _state.update { it.copy(report = null) }

    fun claimQuest() {
        val reward = engine.claimQuest() ?: return fail()
        _state.update { it.copy(toast = reward) }
        ok(Sfx.COMBO)
    }

    fun clearToast() = _state.update { it.copy(toast = null) }

    /** Cevrimdisi pencereyi kapat; [doubled] odullu reklam izlendiyse kazanc bir kez daha eklenir. */
    fun closeOffline(doubled: Boolean) {
        val gained = _state.value.offline
        if (doubled && gained.isNotEmpty()) engine.bonus(gained)
        _state.update { it.copy(offline = emptyMap()) }
        frame++
        save()
    }

    fun save() {
        if (_state.value.loading) return
        val text = json.encodeToString(KingdomSave.serializer(), engine.save(System.currentTimeMillis()))
        val score = engine.score()
        viewModelScope.launch {
            prefs.saveGame(PROFILE_KEY, text)
            prefs.submitScore(GAME_ID, score)
        }
    }

    override fun onCleared() {
        // viewModelScope iptal edildi; son durumu dogrudan yaz
        if (!_state.value.loading) {
            val text = json.encodeToString(KingdomSave.serializer(), engine.save(System.currentTimeMillis()))
            runCatching { kotlinx.coroutines.runBlocking { prefs.saveGame(PROFILE_KEY, text) } }
        }
        super.onCleared()
    }

    companion object {
        const val GAME_ID = "idle-kingdom"
        const val PROFILE_KEY = "idle-kingdom-profile"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
