package com.eomer91.minigamesworld.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eomer91.minigamesworld.data.Game
import com.eomer91.minigamesworld.data.GameCatalog
import com.eomer91.minigamesworld.data.UserPrefsRepository
import com.eomer91.minigamesworld.ui.navigation.GameDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameDetailUiState(
    val game: Game?,
    val gold: Int = UserPrefsRepository.STARTING_GOLD,
    val isFavorite: Boolean = false,
    val bestScore: Int = 0,
)

sealed interface GameDetailEvent {
    /** [showAd]: oyun gercekten oynanabiliyorsa once gecis reklami gosterilir. */
    data class StartGame(val gameId: String, val showAd: Boolean) : GameDetailEvent
    data object NotEnoughGold : GameDetailEvent
}

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prefs: UserPrefsRepository,
) : ViewModel() {

    private val gameId = savedStateHandle.toRoute<GameDetailRoute>().gameId
    private val game = GameCatalog.find(gameId)

    val uiState: StateFlow<GameDetailUiState> = combine(
        prefs.gold,
        prefs.favorites,
        prefs.highScore(gameId),
    ) { gold, favorites, best ->
        GameDetailUiState(game, gold, gameId in favorites, best)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameDetailUiState(game))

    private val _events = Channel<GameDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun toggleFavorite() {
        viewModelScope.launch { prefs.toggleFavorite(gameId) }
    }

    fun play() {
        val game = game ?: return
        viewModelScope.launch {
            // Henuz tasinmamis oyunlar icin altin harcanmaz, "Cok yakinda" acilir.
            val canStart = !game.isPlayable || prefs.spendGold(game.entryCost)
            _events.send(if (canStart) GameDetailEvent.StartGame(game.id, showAd = game.isPlayable) else GameDetailEvent.NotEnoughGold)
        }
    }
}
