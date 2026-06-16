package com.lizz.myapptemplate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.connectivity.ConnectivityMonitor
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.ThemeMode
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import com.lizz.myapptemplate.ui.OfflineBanner
import com.lizz.myapptemplate.ui.rememberOptionalKoin

@Composable
@Preview
fun App() {
    val themeMode = rememberThemeMode()
    val isOnline = rememberOnlineStatus()

    AppTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                OfflineBanner(visible = !isOnline)
                AppNavHost()
            }
        }
    }
}

@Composable
private fun rememberThemeMode(): ThemeMode {
    val provider = rememberOptionalKoin<ThemeModeProvider>() ?: return ThemeMode.System
    return provider.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System).value
}

@Composable
private fun rememberOnlineStatus(): Boolean {
    val monitor = rememberOptionalKoin<ConnectivityMonitor>() ?: return true
    return monitor.isOnline.collectAsStateWithLifecycle(initialValue = true).value
}
