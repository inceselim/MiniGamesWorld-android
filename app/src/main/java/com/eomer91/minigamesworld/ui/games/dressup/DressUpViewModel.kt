package com.eomer91.minigamesworld.ui.games.dressup

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
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DressPhase { LOADING, DRESSING, JUDGING, RESULT }

data class DressUiState(
    val phase: DressPhase = DressPhase.LOADING,
    val profile: DressProfile = DressProfile(),
    val tab: DressCategory = DressCategory.HAIR,
    val theme: DressTheme = DressTheme.BEACH,
    /** Gosterilen juri puanlari (animasyonla birer birer acilir) */
    val judges: List<Int> = emptyList(),
    val revealed: Int = 0,
    val stars: Int = 0,
    val goldEarned: Int = 0,
) {
    val outfit: Outfit get() = profile.outfit
}

@HiltViewModel
class DressUpViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val _state = MutableStateFlow(DressUiState())
    val state: StateFlow<DressUiState> = _state.asStateFlow()

    private val goldTracker = GoldTracker()
    private var sessionScore = 0
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val p = DressProfile.fromJson(prefs.savedGame(PROFILE_KEY).first())
            _state.update { it.copy(phase = DressPhase.DRESSING, profile = p, theme = DressTheme.entries.random()) }
        }
    }

    // ------------------------------------------------------------ Secimler

    fun selectTab(t: DressCategory) {
        _state.update { it.copy(tab = t) }
        feedback.play(Sfx.SWIPE, volume = 0.3f)
    }

    private fun edit(f: (Outfit) -> Outfit) {
        if (_state.value.phase != DressPhase.DRESSING) return
        _state.update { it.copy(profile = it.profile.copy(outfit = f(it.outfit))) }
        feedback.vibrate(Haptic.LIGHT)
        feedback.play(Sfx.DROP, volume = 0.3f)
        scheduleSave()
    }

    fun pickHair(style: String) = edit { it.copy(hair = style) }

    fun pickItem(cat: DressCategory, id: String) = edit { o ->
        when (cat) {
            DressCategory.TOP -> o.copy(top = id, dress = null, bottom = o.bottom ?: "jeans")
            DressCategory.BOTTOM -> o.copy(bottom = id, dress = null, top = o.top ?: "tee")
            DressCategory.DRESS -> o.copy(dress = id, top = null, bottom = null)
            DressCategory.SHOES -> o.copy(shoes = id)
            DressCategory.HEAD -> o.copy(head = id)
            DressCategory.EXTRA -> o.copy(extra = id)
            else -> o
        }
    }

    fun pickBackground(i: Int) = edit { it.copy(background = i) }

    /** Secili sekmedeki secili parcanin rengi. */
    fun pickColor(color: Long) {
        val s = _state.value
        val o = s.outfit
        when (s.tab) {
            DressCategory.HAIR -> edit { it.copy(hairColor = color) }
            else -> {
                val id = selectedId(o, s.tab) ?: return
                val key = "${catKey(s.tab)}:$id"
                edit { it.copy(colors = it.colors + (key to color)) }
            }
        }
    }

    fun pickSkin(c: Long) = edit { it.copy(skin = c) }
    fun pickEyes(c: Long) = edit { it.copy(eyes = c) }
    fun pickLips(c: Long) = edit { it.copy(lips = c) }

    // ------------------------------------------------------------ Jüri

    fun showOff() {
        val s = _state.value
        if (s.phase != DressPhase.DRESSING) return
        val o = s.outfit
        val worn = wornItems(o).map { (cat, item) -> item.tags to o.colorOf(catKey(cat), item) }
        val raw = DressJudge.rawScore(s.theme, worn, DressBackground.entries[o.background.coerceIn(0, DressBackground.entries.lastIndex)])
        val judges = DressJudge.judges(raw)
        val stars = DressJudge.stars(judges)
        _state.update { it.copy(phase = DressPhase.JUDGING, judges = judges, revealed = 0, stars = stars) }
        viewModelScope.launch {
            for (k in 1..3) {
                delay(700)
                _state.update { it.copy(revealed = k) }
                feedback.play(Sfx.BLOCK, volume = 0.6f)
            }
            delay(500)
            val score = (judges.average() * 100).toInt()
            sessionScore += score
            val delta = goldTracker.onScore(sessionScore)
            if (delta > 0) prefs.addGold(delta)
            prefs.submitScore(GAME_ID, score)
            _state.update {
                it.copy(
                    phase = DressPhase.RESULT,
                    goldEarned = goldTracker.awarded,
                    profile = it.profile.copy(totalStars = it.profile.totalStars + stars, shows = it.profile.shows + 1),
                )
            }
            feedback.play(if (stars >= 2) Sfx.COMBO else Sfx.CLEAR)
            feedback.vibrate(Haptic.HEAVY)
            save()
        }
    }

    fun nextTheme() {
        val current = _state.value.theme
        _state.update { it.copy(phase = DressPhase.DRESSING, theme = DressTheme.entries.filter { t -> t != current }.random(), judges = emptyList(), revealed = 0) }
    }

    fun changeTheme() {
        if (_state.value.phase == DressPhase.DRESSING) nextTheme()
    }

    // ------------------------------------------------------------ Kayit

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            save()
        }
    }

    private suspend fun save() {
        prefs.saveGame(PROFILE_KEY, _state.value.profile.toJson())
    }

    companion object {
        const val GAME_ID = "dress-up"
        const val PROFILE_KEY = "dress-up-profile"

        fun itemsOf(cat: DressCategory): List<DressItem> = when (cat) {
            DressCategory.TOP -> DressUpArt.top
            DressCategory.BOTTOM -> DressUpArt.bottom
            DressCategory.DRESS -> DressUpArt.dress
            DressCategory.SHOES -> DressUpArt.shoes
            DressCategory.HEAD -> DressUpArt.head
            DressCategory.EXTRA -> DressUpArt.extra
            else -> emptyList()
        }

        fun catKey(cat: DressCategory) = cat.name.lowercase()

        fun selectedId(o: Outfit, cat: DressCategory): String? = when (cat) {
            DressCategory.TOP -> o.top
            DressCategory.BOTTOM -> o.bottom
            DressCategory.DRESS -> o.dress
            DressCategory.SHOES -> o.shoes
            DressCategory.HEAD -> o.head
            DressCategory.EXTRA -> o.extra
            else -> null
        }

        fun item(cat: DressCategory, id: String?): DressItem? = id?.let { i -> itemsOf(cat).firstOrNull { it.id == i } }

        /** Giyilen (gorunen) parcalar, cizim sirasina gore degil. */
        fun wornItems(o: Outfit): List<Pair<DressCategory, DressItem>> = buildList {
            if (o.dress != null) item(DressCategory.DRESS, o.dress)?.let { add(DressCategory.DRESS to it) }
            else {
                item(DressCategory.TOP, o.top)?.let { add(DressCategory.TOP to it) }
                item(DressCategory.BOTTOM, o.bottom)?.let { add(DressCategory.BOTTOM to it) }
            }
            item(DressCategory.SHOES, o.shoes)?.let { add(DressCategory.SHOES to it) }
            if (o.head != "none") item(DressCategory.HEAD, o.head)?.let { add(DressCategory.HEAD to it) }
            if (o.extra != "none") item(DressCategory.EXTRA, o.extra)?.let { add(DressCategory.EXTRA to it) }
        }
    }
}
