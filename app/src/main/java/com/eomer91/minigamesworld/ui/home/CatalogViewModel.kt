package com.eomer91.minigamesworld.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ana sayfa, arama ve favoriler ekranlarinin ortak verisi. */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
) : ViewModel() {

    val gold: StateFlow<Int> = prefs.gold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPrefsRepository.STARTING_GOLD)

    val favorites: StateFlow<Set<String>> = prefs.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(gameId: String) {
        viewModelScope.launch { prefs.toggleFavorite(gameId) }
    }
}
