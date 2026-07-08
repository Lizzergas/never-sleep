package com.lizz.neversleep.settings.domain

import com.lizz.neversleep.designsystem.ThemeMode
import kotlinx.coroutines.flow.Flow

/** What the settings feature needs from persistence — implemented in data/. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
