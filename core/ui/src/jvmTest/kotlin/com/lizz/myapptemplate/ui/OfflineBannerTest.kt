package com.lizz.myapptemplate.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class OfflineBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bannerAppearsAndDisappearsWithVisibility() {
        var visible by mutableStateOf(false)
        rule.setContent {
            OfflineBanner(visible = visible)
        }

        rule.onNodeWithText("You're offline").assertDoesNotExist()

        visible = true
        rule.waitForIdle()
        rule.onNodeWithText("You're offline").assertIsDisplayed()

        visible = false
        rule.waitForIdle()
        rule.onNodeWithText("You're offline").assertDoesNotExist()
    }
}
