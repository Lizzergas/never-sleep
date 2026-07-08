package com.lizz.neversleep.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lizz.neversleep.datastore.safeData
import com.lizz.neversleep.designsystem.ThemeMode
import com.lizz.neversleep.designsystem.ThemeModeProvider
import com.lizz.neversleep.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed settings. Also the app's [ThemeModeProvider] — the shell
 * applies whatever theme mode is persisted here.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository,
    ThemeModeProvider {
    override val themeMode: Flow<ThemeMode> = dataStore.safeData().map { preferences ->
        preferences[THEME_MODE_KEY]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.System
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
