package com.lizz.myapptemplate.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.myapptemplate.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = repository.themeMode
        .map { SettingsUiState(themeMode = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeMode ->
                viewModelScope.launch { repository.setThemeMode(event.mode) }
        }
    }
}
