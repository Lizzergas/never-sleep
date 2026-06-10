package com.lizz.myapptemplate

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        // TEMPORARY: dependency-verification screen.
        // Delete DemoScreen.kt and put real app content here.
        DemoScreen()
    }
}
