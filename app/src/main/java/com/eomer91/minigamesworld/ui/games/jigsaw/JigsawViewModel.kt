package com.eomer91.minigamesworld.ui.games.jigsaw

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class JigsawPhase { PICK, PLAY, DONE }

@Serializable
data class JigsawProfile(val version: Int = 1, val best: Map<String, Int> = emptyMap()) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(t: String?): JigsawProfile = t?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: JigsawProfile()
        fun key(pic: Int, level: JigsawLevel) = "$pic-${level.name}"
    }
}

data class JigsawUiState(
    val phase: JigsawPhase = JigsawPhase.PICK,
    val picture: Int = 0,
    val level: JigsawLevel = JigsawLevel.EASY,
    val seconds: Int = 0,
    val placed: Int = 0,
    val total: Int = 0,
    val preview: Boolean = false,
    val profile: JigsawProfile = JigsawProfile(),
    val score: Int = 0,
    val goldEarned: Int = 0,
    val newBest: Boolean = false,
)

@HiltViewModel
class JigsawViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(JigsawUiState())
    val state: StateFlow<JigsawUiState> = _state.asStateFlow()

    var engine: JigsawEngine? = null
        private set

    var frame by mutableLongStateOf(0L)
        private set

    /** Parcalar tepsiye ilk cizimde (ekran boyu bilinince) dagitilir. */
    private var scattered = false
    /** Son bilinen tuval olcusu (ayni ekranda yeni oyun baslayinca kullanilir). */
    private var canvas: FloatArray? = null
    private var dragging: JigsawPiece? = null
    private var timer: Job? = null
    private val goldTracker = GoldTracker()
    private var sessionScore = 0

    init {
        viewModelScope.launch {
            val p = JigsawProfile.fromJson(prefs.savedGame(PROFILE_KEY).first())
            _state.update { it.copy(profile = p) }
        }
    }

    fun selectLevel(l: JigsawLevel) = _state.update { it.copy(level = l) }

    fun start(picture: Int) {
        val l = _state.value.level
        engine = JigsawEngine(l.cols, l.rows)
        scattered = false
        canvas?.let { layout(it[0], it[1], it[2], it[3]) }
        _state.update { it.copy(phase = JigsawPhase.PLAY, picture = picture, seconds = 0, placed = 0, total = l.cols * l.rows, preview = false, newBest = false) }
        timer?.cancel()
        timer = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_state.value.phase == JigsawPhase.PLAY) _state.update { it.copy(seconds = it.seconds + 1) }
            }
        }
        frame++
    }

    /** Ekran olcusu belli olunca: parcalari tahtanin altindaki tepsiye dagit. [aspect] = resim yukseklik/genislik. */
    fun layout(width: Float, height: Float, topPad: Float, aspect: Float) {
        canvas = floatArrayOf(width, height, topPad, aspect)
        val e = engine ?: return
        if (scattered) return
        scattered = true
        val boardH = width * 0.94f * aspect
        val unitH = boardH / e.rows
        e.scatter(e.rows + 0.25f, (height - topPad) / unitH - 0.05f)
        frame++
    }

    fun togglePreview() = _state.update { it.copy(preview = !it.preview) }

    fun backToPick() {
        timer?.cancel()
        engine = null
        _state.update { it.copy(phase = JigsawPhase.PICK) }
    }

    // ------------------------------------------------------------ Surukleme (hucre birimi)

    fun dragStart(x: Float, y: Float) {
        dragging = engine?.pick(x, y)
        if (dragging != null) {
            feedback.vibrate(Haptic.LIGHT)
            frame++
        }
    }

    fun dragBy(dx: Float, dy: Float, minY: Float, maxY: Float) {
        val p = dragging ?: return
        val e = engine ?: return
        p.x = (p.x + dx).coerceIn(-0.4f, e.cols - 0.6f)
        p.y = (p.y + dy).coerceIn(minY, maxY)
        frame++
    }

    fun dragEnd() {
        val p = dragging ?: return
        val e = engine ?: return
        dragging = null
        if (e.drop(p)) {
            feedback.play(Sfx.BLOCK, volume = 0.6f)
            feedback.vibrate(Haptic.LIGHT)
            _state.update { it.copy(placed = e.placedCount) }
            if (e.done) finish()
        }
        frame++
    }

    private fun finish() {
        timer?.cancel()
        val s = _state.value
        val key = JigsawProfile.key(s.picture, s.level)
        val prev = s.profile.best[key]
        val newBest = prev == null || s.seconds < prev
        val profile = if (newBest) s.profile.copy(best = s.profile.best + (key to s.seconds)) else s.profile
        val score = s.level.score + (s.level.score * 2 - s.seconds * 2).coerceAtLeast(0)
        sessionScore += score
        val delta = goldTracker.onScore(sessionScore)
        viewModelScope.launch {
            if (delta > 0) prefs.addGold(delta)
            prefs.submitScore(GAME_ID, score)
            prefs.saveGame(PROFILE_KEY, profile.toJson())
        }
        _state.update { it.copy(phase = JigsawPhase.DONE, profile = profile, score = score, goldEarned = goldTracker.awarded, newBest = newBest) }
        feedback.play(Sfx.COMBO)
        feedback.vibrate(Haptic.HEAVY)
    }

    companion object {
        const val GAME_ID = "jigsaw"
        const val PROFILE_KEY = "jigsaw-profile"
    }
}
