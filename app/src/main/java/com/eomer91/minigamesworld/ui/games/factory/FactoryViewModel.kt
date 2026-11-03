package com.eomer91.minigamesworld.ui.games.factory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
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

enum class FactoryTab { PRODUCTION, MARKET, ORDERS }

data class FactoryUiState(
    val loading: Boolean = true,
    val tab: FactoryTab = FactoryTab.PRODUCTION,
    /** Cevrimdisi kazanc penceresi (0 = yok) */
    val offlineEarned: Double = 0.0,
    val offlineSeconds: Long = 0,
    val goldEarned: Int = 0,
)

@HiltViewModel
class FactoryViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(FactoryUiState())
    val state: StateFlow<FactoryUiState> = _state.asStateFlow()

    var engine = FactoryEngine()
        private set

    /** Her guncellemede artar; ekran bunu okuyup yeniden cizer. */
    var frame by mutableLongStateOf(0L)
        private set

    private val goldTracker = GoldTracker()
    private var startEarned = 0.0
    private var saveTimer = 0.0
    private var lastOrders = 0

    init {
        viewModelScope.launch {
            val save = prefs.savedGame(PROFILE_KEY).first()?.let { runCatching { json.decodeFromString(FactorySave.serializer(), it) }.getOrNull() }
            val e = FactoryEngine(save ?: FactorySave())
            var earned = 0.0
            var seconds = 0L
            if (save != null && save.savedAt > 0) {
                seconds = ((System.currentTimeMillis() - save.savedAt) / 1000).coerceAtLeast(0)
                earned = e.offline(seconds.toDouble())
            }
            engine = e
            startEarned = e.totalEarned - earned
            _state.update { it.copy(loading = false, offlineEarned = earned, offlineSeconds = seconds) }
            award()
            frame++
        }
    }

    fun selectTab(t: FactoryTab) = _state.update { it.copy(tab = t) }

    fun tick(dt: Double) {
        if (_state.value.loading) return
        val e = engine
        e.update(dt)
        if (e.orderDone > lastOrders) lastOrders = e.orderDone
        frame++
        saveTimer += dt
        if (saveTimer > 10) {
            saveTimer = 0.0
            save()
            award()
        }
    }

    fun upgrade(b: Building) {
        if (engine.upgrade(b)) {
            feedback.play(Sfx.CLEAR, volume = 0.6f)
            feedback.vibrate(Haptic.LIGHT)
            frame++
            save()
        } else {
            feedback.vibrate(Haptic.HEAVY)
        }
    }

    fun upgradeWarehouse() {
        if (engine.upgradeWarehouse()) {
            feedback.play(Sfx.CLEAR, volume = 0.6f)
            frame++
            save()
        }
    }

    fun sell(p: Product) {
        if (engine.sell(p) > 0) {
            feedback.play(Sfx.DROP, volume = 0.6f)
            feedback.vibrate(Haptic.LIGHT)
            frame++
            award()
        }
    }

    fun toggleAuto(p: Product) {
        engine.toggleAutoSell(p)
        frame++
        save()
    }

    fun deliver(i: Int) {
        if (engine.deliver(i)) {
            feedback.play(Sfx.COMBO)
            feedback.vibrate(Haptic.HEAVY)
            frame++
            award()
            save()
        }
    }

    /** Cevrimdisi kazanci kapat; [doubled] odullu reklam izlendiyse bir kati daha eklenir. */
    fun closeOffline(doubled: Boolean) {
        val x = _state.value.offlineEarned
        if (doubled && x > 0) engine.bonus(x)
        _state.update { it.copy(offlineEarned = 0.0) }
        frame++
        award()
        save()
    }

    /** Bu oturumda kazanilan paraya gore hub altini (10$ = 1 puan). */
    private fun award() {
        val score = ((engine.totalEarned - startEarned) / 10).toInt().coerceAtLeast(0)
        val delta = goldTracker.onScore(score)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }
        _state.update { it.copy(goldEarned = goldTracker.awarded) }
    }

    fun save() {
        if (_state.value.loading) return
        val text = json.encodeToString(FactorySave.serializer(), engine.save(System.currentTimeMillis()))
        val best = engine.totalEarned.coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
        viewModelScope.launch {
            prefs.saveGame(PROFILE_KEY, text)
            prefs.submitScore(GAME_ID, best)
        }
    }

    override fun onCleared() {
        // viewModelScope iptal edildi; son durumu dogrudan yaz
        if (!_state.value.loading) {
            val text = json.encodeToString(FactorySave.serializer(), engine.save(System.currentTimeMillis()))
            runCatching { kotlinx.coroutines.runBlocking { prefs.saveGame(PROFILE_KEY, text) } }
        }
        super.onCleared()
    }

    companion object {
        const val GAME_ID = "factory"
        const val PROFILE_KEY = "factory-profile"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
