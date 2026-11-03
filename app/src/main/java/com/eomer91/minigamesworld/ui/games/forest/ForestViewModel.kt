package com.eomer91.minigamesworld.ui.games.forest

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
import javax.inject.Inject

enum class ForestPhase { LOADING, HUB, PLAYING, PAUSED, LEVEL_UP, RESULT }

data class ForestUiState(
    val phase: ForestPhase = ForestPhase.LOADING,
    val profile: ForestProfile = ForestProfile(),
    val selectedMap: ForestMap = ForestMap.EDGE,
    val choices: List<Upgrade> = emptyList(),
    /** Sonuc ekrani */
    val victory: Boolean = false,
    val canRevive: Boolean = false,
    val runCoins: Int = 0,
    val runKills: Int = 0,
    val runTime: Int = 0,
    val runLevel: Int = 1,
    val score: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
    val bossWarning: Float = 0f,
    /** Bu turda tamamlanan gorev id'leri (kalici olanlar profilde). */
    val runQuestsDone: Set<String> = emptySet(),
    /** Ekranda kisa sure gosterilen "gorev tamamlandi" bildirimi. */
    val questToast: Quest? = null,
    val questToastTime: Float = 0f,
    val evolveToast: Float = 0f,
    val runQuestCoins: Int = 0,
) {
    val quests: List<Quest> get() = ForestQuests.forMap(selectedMap)
    fun questDone(q: Quest) = q.id in profile.questsDone || q.id in runQuestsDone
    val isNewRecord: Boolean get() = phase == ForestPhase.RESULT && bestAtStart >= 0 && score > bestAtStart
}

@HiltViewModel
class ForestViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(ForestUiState())
    val state: StateFlow<ForestUiState> = _state.asStateFlow()

    /** Calisan oyun; ekran bunu dogrudan cizer. */
    var engine: ForestEngine? = null
        private set

    /** Her karede artar; Canvas yeniden cizilsin diye okunur. */
    var frame by mutableLongStateOf(0L)
        private set

    private var inputX = 0f
    private var inputY = 0f
    private val goldTracker = GoldTracker()
    private var gemSfxCooldown = 0f

    init {
        viewModelScope.launch {
            val p = ForestProfile.fromJson(prefs.savedGame(PROFILE_KEY).first())
            _state.update { it.copy(phase = ForestPhase.HUB, profile = p, selectedMap = ForestMap.entries[p.unlocked.coerceIn(0, ForestMap.entries.lastIndex)]) }
        }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    // ------------------------------------------------------------ Hub

    fun selectMap(m: ForestMap) {
        if (m.ordinal <= _state.value.profile.unlocked) _state.update { it.copy(selectedMap = m) }
    }

    fun startRun() {
        val s = _state.value
        val e = ForestEngine(s.selectedMap, HeroStats())
        engine = e
        inputX = 0f
        inputY = 0f
        goldTracker.reset()
        finalized = false
        _state.update {
            it.copy(
                phase = ForestPhase.PLAYING, choices = emptyList(), goldEarned = 0, bestAtStart = it.best, bossWarning = 0f,
                runQuestsDone = emptySet(), questToast = null, questToastTime = 0f, evolveToast = 0f, runQuestCoins = 0,
            )
        }
        feedback.play(Sfx.CLEAR, volume = 0.5f)
    }

    fun toHub() {
        // Ikinci sans bekleniyorsa sonucu kesinlestir (sikkeler kaybolmasin)
        engine?.let { if (!finalized && it.time > 0f) { it.reviveUsed = true; finish(it) } }
        engine = null
        _state.update { it.copy(phase = ForestPhase.HUB) }
    }

    fun nextMap() {
        val s = _state.value
        val next = ForestMap.entries.getOrNull(s.selectedMap.ordinal + 1)
        if (next != null && next.ordinal <= s.profile.unlocked) _state.update { it.copy(selectedMap = next) }
        startRun()
    }

    // ------------------------------------------------------------ Oyun

    fun setViewSize(w: Float, h: Float) {
        engine?.let {
            it.viewW = w
            it.viewH = h
        }
    }

    fun setInput(x: Float, y: Float) {
        inputX = x
        inputY = y
    }

    fun dash() {
        if (_state.value.phase != ForestPhase.PLAYING) return
        if (engine?.dash() == true) feedback.play(Sfx.SWIPE, volume = 0.5f)
    }

    fun skill() {
        if (_state.value.phase != ForestPhase.PLAYING) return
        if (engine?.useSkill() == true) {
            feedback.play(Sfx.COMBO)
            feedback.vibrate(Haptic.HEAVY)
        }
    }

    fun pause() {
        if (_state.value.phase == ForestPhase.PLAYING) _state.update { it.copy(phase = ForestPhase.PAUSED) }
    }

    fun resume() {
        if (_state.value.phase == ForestPhase.PAUSED) _state.update { it.copy(phase = ForestPhase.PLAYING) }
    }

    /** Duraklatma menusunden haritadan cik: o ana kadar toplanan sikkeler kaydedilir. */
    fun quitRun() {
        val e = engine ?: return toHub()
        e.dead = true
        finish(e)
    }

    fun onFrame(dt: Float) {
        val e = engine ?: return
        val s = _state.value
        if (s.phase != ForestPhase.PLAYING) return
        e.update(dt.coerceAtMost(0.05f), inputX, inputY)
        frame++
        gemSfxCooldown -= dt
        val ev = e.events
        if (ev.kills > 0) feedback.play(Sfx.BLOCK, volume = 0.25f)
        if (ev.pickedGems > 0 && gemSfxCooldown <= 0f) {
            gemSfxCooldown = 0.08f
            feedback.play(Sfx.DROP, volume = 0.2f)
        }
        if (ev.pickedCoins > 0) feedback.play(Sfx.COMBO, volume = 0.35f)
        if (ev.heroHit) feedback.vibrate(Haptic.LIGHT)
        if (ev.bossSpawned) {
            _state.update { it.copy(bossWarning = 3f) }
            feedback.play(Sfx.GAME_OVER, volume = 0.6f)
            feedback.vibrate(Haptic.HEAVY)
        }
        if (s.bossWarning > 0f) _state.update { it.copy(bossWarning = (it.bossWarning - dt).coerceAtLeast(0f)) }
        if (s.questToastTime > 0f) _state.update { it.copy(questToastTime = (it.questToastTime - dt).coerceAtLeast(0f)) }
        if (s.evolveToast > 0f) _state.update { it.copy(evolveToast = (it.evolveToast - dt).coerceAtLeast(0f)) }
        if (ev.evolved) {
            _state.update { it.copy(evolveToast = 2.5f) }
            feedback.play(Sfx.CLEAR)
            feedback.vibrate(Haptic.HEAVY)
        }
        checkQuests(e)

        when {
            e.dead -> {
                feedback.play(Sfx.GAME_OVER)
                feedback.vibrate(Haptic.HEAVY)
                showResult(e, victory = false)
            }
            e.victory -> {
                feedback.play(Sfx.CLEAR)
                finish(e)
            }
            e.pendingLevelUps > 0 -> {
                _state.update { it.copy(phase = ForestPhase.LEVEL_UP, choices = e.rollChoices()) }
                feedback.play(Sfx.COMBO, volume = 0.7f)
            }
        }
    }

    /** Yeni tamamlanan gorevleri isaretle; odul tur sonunda verilir. */
    private fun checkQuests(e: ForestEngine) {
        val s = _state.value
        s.quests.forEach { q ->
            if (!s.questDone(q) && ForestQuests.isDone(q, e)) {
                _state.update { it.copy(runQuestsDone = it.runQuestsDone + q.id, questToast = q, questToastTime = 3f, runQuestCoins = it.runQuestCoins + q.reward) }
                feedback.play(Sfx.COMBO)
                feedback.vibrate(Haptic.LIGHT)
            }
        }
    }

    fun choose(u: Upgrade) {
        val e = engine ?: return
        e.apply(u)
        feedback.play(Sfx.CLEAR, volume = 0.6f)
        _state.update {
            if (e.pendingLevelUps > 0) it.copy(choices = e.rollChoices())
            else it.copy(phase = ForestPhase.PLAYING, choices = emptyList())
        }
    }

    // ------------------------------------------------------------ Bitis

    /** Olum: once ikinci sans secenegi gosterilir; sikkeler henuz kaydedilmez. */
    private fun showResult(e: ForestEngine, victory: Boolean) {
        _state.update {
            it.copy(
                phase = ForestPhase.RESULT,
                victory = victory,
                canRevive = !victory && !e.reviveUsed,
                runCoins = e.coins,
                runKills = e.kills,
                runTime = e.time.toInt(),
                runLevel = e.level,
                score = e.score,
            )
        }
        if (victory || e.reviveUsed) finish(e)
    }

    fun revive() {
        val e = engine ?: return
        e.revive()
        _state.update { it.copy(phase = ForestPhase.PLAYING, canRevive = false) }
    }

    /** Odullu reklam izlenmezse: sonucu kesinlestir. */
    fun declineRevive() {
        val e = engine ?: return
        e.reviveUsed = true
        finish(e)
    }

    private var finalized = false

    private fun finish(e: ForestEngine) {
        if (_state.value.phase != ForestPhase.RESULT) {
            _state.update {
                it.copy(
                    phase = ForestPhase.RESULT,
                    victory = e.victory,
                    canRevive = false,
                    runCoins = e.coins,
                    runKills = e.kills,
                    runTime = e.time.toInt(),
                    runLevel = e.level,
                    score = e.score,
                )
            }
        } else {
            _state.update { it.copy(canRevive = false) }
        }
        saveRunResults(e)
    }

    /** Sikke, harita ilerlemesi, skor ve hub altini (tur basina bir kez). */
    private fun saveRunResults(e: ForestEngine) {
        if (finalized) return
        finalized = true
        val s = _state.value
        val mapIdx = e.map.ordinal
        val p = s.profile
        val profile = p.copy(
            coins = p.coins + e.coins + s.runQuestCoins,
            questsDone = (p.questsDone + s.runQuestsDone).distinct(),
            unlocked = if (e.victory) maxOf(p.unlocked, (mapIdx + 1).coerceAtMost(ForestMap.entries.lastIndex)) else p.unlocked,
            cleared = if (e.victory && mapIdx !in p.cleared) p.cleared + mapIdx else p.cleared,
            bestKills = p.bestKills + (mapIdx to maxOf(p.bestKills[mapIdx] ?: 0, e.kills)),
        )
        val delta = goldTracker.onScore(e.score)
        _state.update { it.copy(profile = profile, goldEarned = goldTracker.awarded) }
        viewModelScope.launch {
            prefs.saveGame(PROFILE_KEY, profile.toJson())
            if (delta > 0) prefs.addGold(delta)
            prefs.submitScore(GAME_ID, e.score)
        }
    }

    companion object {
        const val GAME_ID = "forest-guardian"
        /** Katalog id'sinden farkli: detay ekraninda "devam" butonu cikmasin. */
        const val PROFILE_KEY = "forest-guardian-profile"
    }
}
