package com.eomer91.minigamesworld.ui.games.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import com.eomer91.minigamesworld.ui.games.makeup.Customer
import com.eomer91.minigamesworld.ui.games.makeup.Makeup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CookingUiState(
    val step: CakeStep = CakeStep.INGREDIENTS,
    val customer: Customer = Makeup.randomCustomer(),
    val order: CakeOrder = Cake.randomOrder(),
    val work: CakeWork = CakeWork(),
    val baking: Boolean = false,
    val bakeProgress: Float = 0f,
    val topping: Topping = Topping.STRAWBERRY,
    /** Son eklenen malzeme (kaseye ucma animasyonu) */
    val lastAdded: Ingredient? = null,
    val lastMistake: Boolean = false,
    val stars: Int = 0,
    val served: Int = 0,
    val totalStars: Int = 0,
    val goldEarned: Int = 0,
)

@HiltViewModel
class CookingViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(CookingUiState())
    val state: StateFlow<CookingUiState> = _state.asStateFlow()

    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var stirSound = 0f

    init {
        viewModelScope.launch {
            val stars = prefs.savedGame(PROFILE_KEY).first()?.toIntOrNull() ?: 0
            _state.update { it.copy(totalStars = stars) }
        }
    }

    // ------------------------------------------------------------ 1. Malzemeler

    fun add(ing: Ingredient) {
        val s = _state.value
        if (s.step != CakeStep.INGREDIENTS) return
        val work = Cake.add(s.work, ing, s.order)
        val mistake = work.mistakes > s.work.mistakes
        _state.update { it.copy(work = work, lastAdded = if (mistake) null else ing, lastMistake = mistake) }
        if (mistake) {
            feedback.play(Sfx.GAME_OVER, volume = 0.4f)
            feedback.vibrate(Haptic.HEAVY)
        } else {
            feedback.play(Sfx.DROP, volume = 0.6f)
            feedback.vibrate(Haptic.LIGHT)
        }
        if (Cake.ingredientsDone(work)) advance(CakeStep.MIX)
    }

    // ------------------------------------------------------------ 2. Karistir

    fun stir(dAngle: Float) {
        val s = _state.value
        if (s.step != CakeStep.MIX) return
        val work = Cake.stir(s.work, dAngle)
        _state.update { it.copy(work = work) }
        stirSound += kotlin.math.abs(dAngle)
        if (stirSound > 3f) {
            stirSound = 0f
            feedback.vibrate(Haptic.LIGHT)
        }
        if (work.mix >= 1f && s.work.mix < 1f) {
            feedback.play(Sfx.CLEAR, volume = 0.6f)
            advance(CakeStep.BAKE)
        }
    }

    // ------------------------------------------------------------ 3. Pisir

    fun startBake() {
        val s = _state.value
        if (s.step != CakeStep.BAKE || s.baking || s.work.bake != null) return
        _state.update { it.copy(baking = true, bakeProgress = 0f) }
        feedback.play(Sfx.SWIPE, volume = 0.5f)
    }

    fun onFrame(dt: Float) {
        val s = _state.value
        if (!s.baking) return
        val p = s.bakeProgress + dt / Cake.BAKE_SECONDS
        if (p >= 1f) takeOut(1f) else _state.update { it.copy(bakeProgress = p) }
    }

    fun takeOut(at: Float = _state.value.bakeProgress) {
        val s = _state.value
        if (!s.baking) return
        val result = Cake.bakeResult(at)
        _state.update { it.copy(baking = false, bakeProgress = at, work = it.work.copy(bake = result)) }
        feedback.play(if (result == BakeResult.PERFECT) Sfx.COMBO else Sfx.BLOCK)
        feedback.vibrate(Haptic.HEAVY)
        advance(CakeStep.FROST, 1400)
    }

    // ------------------------------------------------------------ 4. Krema

    fun pickFrosting(f: Frosting) {
        if (_state.value.step != CakeStep.FROST) return
        _state.update { it.copy(work = it.work.copy(frosting = f)) }
        feedback.vibrate(Haptic.LIGHT)
    }

    fun spread(cell: Int, dist: Float) {
        val s = _state.value
        if (s.step != CakeStep.FROST) return
        _state.update { it.copy(work = Cake.spread(it.work, cell, dist)) }
    }

    fun frostingDone() {
        if (_state.value.step == CakeStep.FROST) advance(CakeStep.DECORATE, 0)
    }

    // ------------------------------------------------------------ 5. Sus

    fun pickTopping(t: Topping) {
        _state.update { it.copy(topping = t) }
    }

    fun place(x: Float, y: Float) {
        val s = _state.value
        if (s.step != CakeStep.DECORATE) return
        _state.update { it.copy(work = Cake.place(it.work, it.topping, x, y)) }
        feedback.play(Sfx.DROP, volume = 0.5f)
    }

    fun undoTopping() {
        _state.update { it.copy(work = it.work.copy(toppings = it.work.toppings.dropLast(1))) }
    }

    // ------------------------------------------------------------ 6. Servis

    fun serve() {
        val s = _state.value
        if (s.step != CakeStep.DECORATE) return
        val rating = Cake.rate(s.order, s.work)
        val stars = Cake.stars(rating)
        val score = (rating * 1000).toInt()
        sessionScore += score
        val delta = goldTracker.onScore(sessionScore)
        val total = s.totalStars + stars
        viewModelScope.launch {
            if (delta > 0) prefs.addGold(delta)
            prefs.submitScore(GAME_ID, score)
            prefs.saveGame(PROFILE_KEY, total.toString())
        }
        _state.update { it.copy(step = CakeStep.SERVE, stars = stars, served = it.served + 1, totalStars = total, goldEarned = goldTracker.awarded) }
        feedback.play(if (stars >= 2) Sfx.COMBO else Sfx.CLEAR)
        feedback.vibrate(Haptic.HEAVY)
    }

    fun nextCustomer() {
        _state.update {
            CookingUiState(customer = Makeup.randomCustomer(), order = Cake.randomOrder(), served = it.served, totalStars = it.totalStars, goldEarned = it.goldEarned)
        }
    }

    private fun advance(step: CakeStep, delayMs: Long = 600) {
        viewModelScope.launch {
            delay(delayMs)
            _state.update { if (it.step.ordinal < step.ordinal) it.copy(step = step, lastAdded = null) else it }
        }
    }

    companion object {
        const val GAME_ID = "cake-shop"
        const val PROFILE_KEY = "cake-shop-profile"
    }
}
