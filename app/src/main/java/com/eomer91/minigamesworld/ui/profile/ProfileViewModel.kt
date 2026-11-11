package com.eomer91.minigamesworld.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    prefs: UserPrefsRepository,
) : ViewModel() {
    val highScores: StateFlow<Map<String, Int>> = prefs.highScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
