package com.lizz.neversleep

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark, shader-friendly theme for the Never Sleep screen. */
@Composable
internal fun NeverSleepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C4DFF),
            onPrimary = Color.White,
            background = Color(0xFF0D0D1A),
            surface = Color(0xFF1A1A2E),
        ),
        content = content,
    )
}
