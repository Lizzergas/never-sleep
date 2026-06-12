package com.lizz.myapptemplate

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lizz.myapptemplate.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MyAppTemplate",
        ) {
            App()
        }
    }
}
