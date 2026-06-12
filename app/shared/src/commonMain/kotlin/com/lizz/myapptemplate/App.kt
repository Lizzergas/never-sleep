package com.lizz.myapptemplate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.connectivity.ConnectivityMonitor
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.ThemeMode
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import com.lizz.myapptemplate.ui.OfflineBanner
import org.koin.mp.KoinPlatform

@Composable
@Preview
fun App() {
    // Optional contracts looked up with a fallback so the providing modules
    // stay removable: theme follows feature:settings' ThemeModeProvider
    // (else System), the offline banner follows core:connectivity (else
    // never shown).
    val themeModeProvider =
        remember {
            runCatching { KoinPlatform.getKoin().getOrNull<ThemeModeProvider>() }.getOrNull()
        }
    val themeMode =
        themeModeProvider
            ?.themeMode
            ?.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            ?.value
            ?: ThemeMode.System

    val connectivityMonitor =
        remember {
            runCatching { KoinPlatform.getKoin().getOrNull<ConnectivityMonitor>() }.getOrNull()
        }
    val isOnline =
        connectivityMonitor
            ?.isOnline
            ?.collectAsStateWithLifecycle(initialValue = true)
            ?.value
            ?: true

    AppTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                OfflineBanner(visible = !isOnline)
                AppNavHost()
            }
        }
    }
}
