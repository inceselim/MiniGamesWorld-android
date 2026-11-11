package com.eomer91.minigamesworld.ui.games.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.GoldTracker
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.feedback.GameFeedback
import com.eomer91.minigamesworld.feedback.Haptic
import com.eomer91.minigamesworld.feedback.Sfx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AnimalsMode { MENU, EXPLORE, QUIZ, QUIZ_DONE }

data class AnimalsUiState(
    val mode: AnimalsMode = AnimalsMode.MENU,
    /** Kesfet: buyutulmus hayvan */
    val shown: Animal? = null,
    /** Her dokunusta artar (zipla animasyonu ve yeniden seslendirme icin) */
    val tapCount: Int = 0,
    val questions: List<AnimalQuestion> = emptyList(),
    val index: Int = 0,
    val correct: Int = 0,
    /** Bu soruda yanlis secilenler */
    val wrong: Set<Animal> = emptySet(),
    val solved: Boolean = false,
    val stars: Int = 0,
    val goldEarned: Int = 0,
) {
    val question: AnimalQuestion? get() = questions.getOrNull(index)
}

@HiltViewModel
class AnimalsViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(AnimalsUiState())
    val state: StateFlow<AnimalsUiState> = _state.asStateFlow()

    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var nextJob: Job? = null

    fun explore() = _state.update { it.copy(mode = AnimalsMode.EXPLORE, shown = null) }

    fun menu() {
        nextJob?.cancel()
        _state.update { it.copy(mode = AnimalsMode.MENU, shown = null) }
    }

    fun show(a: Animal?) {
        _state.update { it.copy(shown = a, tapCount = it.tapCount + 1) }
        if (a != null) feedback.play(Sfx.DROP, volume = 0.5f)
    }

    fun startQuiz() {
        nextJob?.cancel()
        _state.update { it.copy(mode = AnimalsMode.QUIZ, questions = AnimalQuiz.make(), index = 0, correct = 0, wrong = emptySet(), solved = false) }
    }

    fun answer(a: Animal) {
        val s = _state.value
        val q = s.question ?: return
        if (s.solved || a in s.wrong) return
        if (a == q.answer) {
            val firstTry = s.wrong.isEmpty()
            _state.update { it.copy(solved = true, correct = it.correct + if (firstTry) 1 else 0, tapCount = it.tapCount + 1) }
            feedback.play(Sfx.COMBO)
            feedback.vibrate(Haptic.LIGHT)
            nextJob = viewModelScope.launch {
                delay(1600)
                next()
            }
        } else {
            _state.update { it.copy(wrong = it.wrong + a) }
            feedback.play(Sfx.BLOCK, volume = 0.6f)
            feedback.vibrate(Haptic.HEAVY)
        }
    }

    private fun next() {
        val s = _state.value
        if (s.index + 1 < s.questions.size) {
            _state.update { it.copy(index = it.index + 1, wrong = emptySet(), solved = false) }
        } else {
            val stars = AnimalQuiz.stars(s.correct)
            val score = s.correct * 60
            sessionScore += score
            val delta = goldTracker.onScore(sessionScore)
            viewModelScope.launch {
                prefs.submitScore(GAME_ID, score)
            }
            _state.update { it.copy(mode = AnimalsMode.QUIZ_DONE, stars = stars, goldEarned = goldTracker.awarded) }
            feedback.play(if (stars >= 2) Sfx.CLEAR else Sfx.DROP)
        }
    }

    companion object {
        const val GAME_ID = "animals"
    }
}
