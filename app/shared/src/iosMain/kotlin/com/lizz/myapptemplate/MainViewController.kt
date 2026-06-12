package com.lizz.myapptemplate

import androidx.compose.ui.window.ComposeUIViewController

// PascalCase on purpose: consumed from Swift as MainViewControllerKt.MainViewController().
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
