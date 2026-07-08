package com.lizz.neversleep.settings.presentation

import com.lizz.neversleep.designsystem.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
)

sealed interface SettingsEvent {
    data class SetThemeMode(
        val mode: ThemeMode,
    ) : SettingsEvent
}
