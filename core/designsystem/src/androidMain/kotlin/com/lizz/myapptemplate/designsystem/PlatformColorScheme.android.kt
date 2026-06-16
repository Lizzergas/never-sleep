package com.lizz.myapptemplate.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformColorScheme(useDarkTheme: Boolean): ColorScheme? {
    val context = LocalContext.current
    return if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
