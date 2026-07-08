package com.lizz.neversleep

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lizz.neversleep.di.initKoin
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    initKoin()
    val deepLinkBridge = DesktopDeepLinkBridge()
    deepLinkBridge.installUriHandler()
    deepLinkBridge.openStartupLinks(args)
    val startRoute = runBlocking { resolveAppStartRoute() }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "NeverSleep",
        ) {
            App(startRoute = startRoute)
        }
    }
}
