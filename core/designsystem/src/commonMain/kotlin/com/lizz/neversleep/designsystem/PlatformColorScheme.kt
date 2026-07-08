package com.lizz.neversleep.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Platform-preferred color scheme, or null to use the brand palette.
 * Android 12+ returns Material You dynamic colors; everywhere else null.
 */
@Composable
expect fun platformColorScheme(useDarkTheme: Boolean): ColorScheme?
