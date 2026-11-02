package com.eomer91.minigamesworld.ui.games.fish

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

enum class FishScreen { LOADING, HUB, FISHING, SHOP, AQUARIUM }

data class FishWorldState(
    val screen: FishScreen = FishScreen.LOADING,
    val profile: FishProfile = FishProfile(),
    val cast: FishingCast? = null,
    /** Bu oturumda (3 atis) kalan atis sayisi. */
    val castsLeft: Int = FishingEngine.CASTS_PER_GAME,
    val sessionCatch: List<FishSpecies> = emptyList(),
    val sessionScore: Int = 0,
    val sessionOver: Boolean = false,
    val extraCastUsed: Boolean = false,
    /** Son atisin sonucu (kisa bilgi icin). */
    val lastCastValue: Int? = null,
    /** Akvaryuma sigmayip inciye cevrilen balik sayisi (oturum). */
    val overflowSold: Int = 0,
    val best: Int = 0,
    val bestAtStart: Int = -1,
    val goldEarned: Int = 0,
) {
    val isNewRecord: Boolean get() = sessionOver && bestAtStart >= 0 && sessionScore > bestAtStart
}

@HiltViewModel
class FishWorldViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val engine = FishingEngine()
    private val goldTracker = GoldTracker()

    private val _state = MutableStateFlow(FishWorldState())
    val state: StateFlow<FishWorldState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = FishProfile.fromJson(prefs.savedGame(PROFILE_KEY).first())
            _state.update { it.copy(screen = FishScreen.HUB, profile = profile) }
        }
        viewModelScope.launch {
            prefs.highScore(GAME_ID).collect { best ->
                _state.update { it.copy(best = best, bestAtStart = if (it.bestAtStart < 0) best else it.bestAtStart) }
            }
        }
    }

    // ---------- Gezinme ----------

    fun openHub() = _state.update { it.copy(screen = FishScreen.HUB, cast = null) }
    fun openShop() = _state.update { it.copy(screen = FishScreen.SHOP) }
    fun openAquarium() = _state.update { it.copy(screen = FishScreen.AQUARIUM) }

    /** Yeni balik tutma oturumu (3 atis). */
    fun startFishing() {
        goldTracker.reset()
        _state.update {
            it.copy(
                screen = FishScreen.FISHING,
                castsLeft = FishingEngine.CASTS_PER_GAME,
                sessionCatch = emptyList(),
                sessionScore = 0,
                sessionOver = false,
                extraCastUsed = false,
                lastCastValue = null,
                overflowSold = 0,
                goldEarned = 0,
                bestAtStart = it.best,
                cast = newCast(it.profile),
            )
        }
    }

    private fun newCast(p: FishProfile) = engine.newCast(
        maxDepth = RodStats.maxDepth(p.rodDepthLevel),
        capacity = RodStats.capacity(p.hookCapacityLevel),
        speedFactor = RodStats.speedFactor(p.reelLevel),
    )

    // ---------- Atis ----------

    /** Parmak ekrana degdi: hazirsa atisi baslat, kancayi o x'e yonlendir. */
    fun onPress(x: Float) {
        val s = _state.value
        var cast = s.cast ?: return
        if (s.sessionOver) return
        if (cast.phase == CastPhase.READY) {
            cast = engine.start(cast)
            feedback.play(Sfx.SWIPE, volume = 0.5f)
        }
        _state.value = s.copy(cast = engine.steer(cast, x))
    }

    /** Parmak surukleniyor: sadece yonlendir (yeni atis baslatmaz). */
    fun onDrag(x: Float) {
        val s = _state.value
        val cast = s.cast ?: return
        if (cast.phase == CastPhase.DESCENDING || cast.phase == CastPhase.ASCENDING) {
            _state.value = s.copy(cast = engine.steer(cast, x))
        }
    }

    fun onFrame(dt: Float) {
        val s = _state.value
        val cast = s.cast ?: return
        if (s.screen != FishScreen.FISHING) return
        val (next, ev) = engine.update(cast, dt.coerceAtMost(0.05f))
        _state.value = s.copy(cast = next)

        ev.caught?.let {
            feedback.play(if (it.value >= 25) Sfx.COMBO else Sfx.BLOCK, volume = 0.7f)
            feedback.vibrate(Haptic.LIGHT)
        }
        if (ev.jellyHit) {
            feedback.play(Sfx.GAME_OVER, volume = 0.4f)
            feedback.vibrate(Haptic.HEAVY)
        }
        if (ev.finished) finishCast(next)
    }

    private fun finishCast(cast: FishingCast) {
        val s = _state.value
        val caught = cast.hooked
        val value = cast.value

        // Akvaryuma ekle; sigmayanlar inciye (degerinin yarisi kadar ekstra) cevrilir
        var profile = s.profile
        val now = System.currentTimeMillis()
        var nextId = profile.nextFishId
        val aquarium = profile.aquarium.toMutableList()
        var overflow = 0
        caught.forEach { sp ->
            if (aquarium.size < profile.aquariumCapacity) aquarium += OwnedFish(nextId++, sp.name, now) else overflow++
        }
        val totals = profile.caughtTotals.toMutableMap()
        caught.forEach { totals[it.name] = (totals[it.name] ?: 0) + 1 }
        profile = profile.copy(
            pearls = profile.pearls + value,
            aquarium = aquarium,
            caughtTotals = totals,
            nextFishId = nextId,
        )

        val castsLeft = s.castsLeft - 1
        val sessionScore = s.sessionScore + value * SCORE_PER_PEARL
        val delta = goldTracker.onScore(sessionScore)
        if (delta > 0) viewModelScope.launch { prefs.addGold(delta) }

        val over = castsLeft <= 0
        _state.value = s.copy(
            profile = profile,
            castsLeft = castsLeft,
            sessionCatch = s.sessionCatch + caught,
            sessionScore = sessionScore,
            sessionOver = over,
            lastCastValue = value,
            overflowSold = s.overflowSold + overflow,
            goldEarned = goldTracker.awarded,
            cast = if (over) cast else newCast(profile),
        )
        saveProfile(profile)
        if (over) {
            if (sessionScore > s.best) viewModelScope.launch { prefs.submitScore(GAME_ID, sessionScore) }
            feedback.play(Sfx.CLEAR, volume = 0.7f)
        }
    }

    /** Odullu reklam sonrasi: oturuma +1 atis (bir kez). */
    fun extraCast() {
        _state.update {
            if (!it.sessionOver || it.extraCastUsed) it
            else it.copy(sessionOver = false, castsLeft = 1, extraCastUsed = true, cast = newCast(it.profile))
        }
    }

    // ---------- Dukkan ----------

    fun buy(upgrade: RodUpgrade) {
        val s = _state.value
        val level = upgrade.level(s.profile)
        if (level >= upgrade.maxLevel) return
        val cost = upgrade.cost(level)
        if (s.profile.pearls < cost) {
            feedback.play(Sfx.SWIPE, volume = 0.4f)
            return
        }
        val profile = upgrade.upgraded(s.profile).copy(pearls = s.profile.pearls - cost)
        _state.value = s.copy(profile = profile)
        saveProfile(profile)
        feedback.play(Sfx.BLOCK)
    }

    private fun saveProfile(p: FishProfile) {
        viewModelScope.launch { prefs.saveGame(PROFILE_KEY, p.toJson()) }
    }

    companion object {
        const val GAME_ID = "fish-world"
        /** Puan = inci x 10 (altin donusumu diger oyunlarla ayni olceklensin). */
        const val SCORE_PER_PEARL = 10
        /** Profil kaydi; katalogdaki oyun id'sinden farkli ki "Kaldigin yerden devam" butonu cikmasin. */
        const val PROFILE_KEY = "fish-world-profile"
    }
}
