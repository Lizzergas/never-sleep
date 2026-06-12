package com.lizz.myapptemplate.settings.domain

import com.lizz.myapptemplate.designsystem.ThemeMode
import kotlinx.coroutines.flow.Flow

/** What the settings feature needs from persistence — implemented in data/. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
