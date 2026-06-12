package com.lizz.myapptemplate.settings.presentation

import com.lizz.myapptemplate.designsystem.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
)

sealed interface SettingsEvent {
    data class SetThemeMode(
        val mode: ThemeMode,
    ) : SettingsEvent
}
