package com.lizz.myapptemplate

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.runBlocking

// PascalCase on purpose: consumed from Swift as MainViewControllerKt.MainViewController().
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController() =
    runBlocking {
        val startRoute = resolveAppStartRoute()
        ComposeUIViewController {
            App(startRoute = startRoute)
        }
    }
