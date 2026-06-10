package com.lizz.myapptemplate

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.ThemeMode
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import org.koin.mp.KoinPlatform

@Composable
@Preview
fun App() {
    // Theme follows whichever feature provides a ThemeModeProvider
    // (feature:settings). Without one, falls back to following the system.
    val themeModeProvider = remember {
        runCatching { KoinPlatform.getKoin().getOrNull<ThemeModeProvider>() }.getOrNull()
    }
    val themeMode = themeModeProvider
        ?.themeMode
        ?.collectAsState(initial = ThemeMode.System)
        ?.value
        ?: ThemeMode.System

    AppTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost()
        }
    }
}
