package com.eomer91.minigamesworld.ui.games.makeup

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

enum class MakeupPhase { WORKING, RESULT }

data class MakeupUiState(
    val phase: MakeupPhase = MakeupPhase.WORKING,
    val customer: Customer = Makeup.randomCustomer(),
    val request: MakeupRequest = Makeup.randomRequest(),
    val face: FaceMakeup = FaceMakeup(),
    val tool: MakeupTool = MakeupTool.SHADOW,
    val shadowColor: Long = MakeupPalettes.shadow[0],
    val blushColor: Long = MakeupPalettes.blush[0],
    val lipsColor: Long = MakeupPalettes.lips[0],
    val sticker: Int = 0,
    val stars: Int = 0,
    val served: Int = 0,
    val totalStars: Int = 0,
    val goldEarned: Int = 0,
) {
    val currentColor: Long
        get() = when (tool) {
            MakeupTool.SHADOW -> shadowColor
            MakeupTool.BLUSH -> blushColor
            MakeupTool.LIPS -> lipsColor
            else -> 0
        }
}

@HiltViewModel
class MakeupViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(MakeupUiState())
    val state: StateFlow<MakeupUiState> = _state.asStateFlow()

    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var rubSound = 0f

    init {
        viewModelScope.launch {
            val stars = prefs.savedGame(PROFILE_KEY).first()?.toIntOrNull() ?: 0
            _state.update { it.copy(totalStars = stars) }
        }
    }

    fun selectTool(t: MakeupTool) {
        _state.update { it.copy(tool = t) }
        feedback.play(Sfx.SWIPE, volume = 0.3f)
    }

    fun pickColor(c: Long) {
        _state.update {
            when (it.tool) {
                MakeupTool.SHADOW -> it.copy(shadowColor = c)
                MakeupTool.BLUSH -> it.copy(blushColor = c)
                MakeupTool.LIPS -> it.copy(lipsColor = c)
                else -> it
            }
        }
        feedback.vibrate(Haptic.LIGHT)
    }

    fun pickLashes(style: Int) {
        if (_state.value.phase != MakeupPhase.WORKING) return
        _state.update { it.copy(face = it.face.copy(lashes = style)) }
        feedback.play(Sfx.DROP, volume = 0.5f)
    }

    fun pickSticker(i: Int) {
        _state.update { it.copy(sticker = i) }
    }

    /** Yuz koordinatlarinda parmak hareketi. */
    fun rub(x: Float, y: Float, dist: Float) {
        val s = _state.value
        if (s.phase != MakeupPhase.WORKING) return
        if (s.tool == MakeupTool.LASHES || s.tool == MakeupTool.STICKER) return
        val face = Makeup.rub(s.face, s.tool, s.currentColor, x, y, dist)
        if (face != s.face) {
            _state.update { it.copy(face = face) }
            rubSound += dist
            if (rubSound > 40f) {
                rubSound = 0f
                feedback.vibrate(Haptic.LIGHT)
            }
        }
    }

    fun tap(x: Float, y: Float) {
        val s = _state.value
        if (s.phase != MakeupPhase.WORKING) return
        when (s.tool) {
            MakeupTool.STICKER -> {
                _state.update { it.copy(face = Makeup.addSticker(it.face, it.sticker, x, y)) }
                feedback.play(Sfx.DROP, volume = 0.5f)
            }
            MakeupTool.LASHES -> Unit
            else -> rub(x, y, 12f)
        }
    }

    fun finish() {
        val s = _state.value
        if (s.phase != MakeupPhase.WORKING) return
        val match = Makeup.match(s.face, s.request)
        val stars = Makeup.stars(match)
        val score = (match * 1000).toInt() + s.face.stickers.size.coerceAtMost(3) * 20
        sessionScore += score
        val delta = goldTracker.onScore(sessionScore)
        val total = s.totalStars + stars
        viewModelScope.launch {
            if (delta > 0) prefs.addGold(delta)
            prefs.submitScore(GAME_ID, score)
            prefs.saveGame(PROFILE_KEY, total.toString())
        }
        _state.update { it.copy(phase = MakeupPhase.RESULT, stars = stars, served = it.served + 1, totalStars = total, goldEarned = goldTracker.awarded) }
        feedback.play(if (stars >= 2) Sfx.COMBO else Sfx.CLEAR)
        feedback.vibrate(Haptic.HEAVY)
    }

    fun nextCustomer() {
        _state.update {
            it.copy(phase = MakeupPhase.WORKING, customer = Makeup.randomCustomer(), request = Makeup.randomRequest(), face = FaceMakeup(), tool = MakeupTool.SHADOW)
        }
    }

    companion object {
        const val GAME_ID = "makeup"
        const val PROFILE_KEY = "makeup-profile"
    }
}
