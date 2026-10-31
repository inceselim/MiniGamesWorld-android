package com.eomer91.minigamesworld.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eomer91.minigamesworld.data.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
) : ViewModel() {
    val soundEnabled: StateFlow<Boolean> = prefs.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val vibrationEnabled: StateFlow<Boolean> = prefs.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { prefs.setSoundEnabled(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { prefs.setVibrationEnabled(enabled) }
    }
}
