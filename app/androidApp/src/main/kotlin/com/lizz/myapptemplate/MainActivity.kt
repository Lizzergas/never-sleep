package com.lizz.myapptemplate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        val startRoute = runBlocking { resolveAppStartRoute() }
        handleDeepLink(intent)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(startRoute = startRoute)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        openAppDeepLink(intent?.dataString)
    }
}
