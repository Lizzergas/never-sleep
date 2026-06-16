package com.lizz.myapptemplate.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The single theming entry point for the whole app.
 * Apply at the shell (App()) — screens only consume MaterialTheme/Theme tokens.
 *
 * With [dynamicColor] (default), Android 12+ uses Material You wallpaper
 * colors; every other platform falls back to the brand palette in Color.kt.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDarkTheme =
        when (themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    val brandScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val colorScheme =
        if (dynamicColor) platformColorScheme(useDarkTheme) ?: brandScheme else brandScheme
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
