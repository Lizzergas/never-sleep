package com.lizz.myapptemplate.designsystem

import kotlinx.coroutines.flow.Flow

/**
 * Implemented by whoever owns the persisted theme preference
 * (feature:settings in the template). The app shell looks this up from DI —
 * if no feature provides it, the theme falls back to [ThemeMode.System],
 * which keeps the settings feature removable.
 */
interface ThemeModeProvider {
    val themeMode: Flow<ThemeMode>
}
