package com.lizz.myapptemplate.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lizz.myapptemplate.designsystem.ThemeMode
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Typed access to the settings preferences. Also the app's [ThemeModeProvider]
 * — the shell applies whatever theme mode is persisted here.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : ThemeModeProvider {
    override val themeMode: Flow<ThemeMode> =
        dataStore.data.map { preferences ->
            preferences[THEME_MODE_KEY]
                ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.System
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
