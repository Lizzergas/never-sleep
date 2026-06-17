package com.lizz.myapptemplate

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lizz.myapptemplate.di.initKoin
import kotlinx.coroutines.runBlocking

fun main() {
    initKoin()
    val startRoute = runBlocking { resolveAppStartRoute() }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MyAppTemplate",
        ) {
            App(startRoute = startRoute)
        }
    }
}
