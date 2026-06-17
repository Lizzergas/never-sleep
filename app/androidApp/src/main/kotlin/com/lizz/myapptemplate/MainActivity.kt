package com.lizz.myapptemplate

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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(startRoute = startRoute)
        }
    }
}
