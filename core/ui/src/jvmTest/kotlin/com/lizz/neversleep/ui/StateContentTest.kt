package com.lizz.neversleep.ui

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lizz.neversleep.model.AppError
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class StateContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun errorContentShowsMessageAndRetryFires() {
        var retried = false
        rule.setContent {
            ErrorContent(AppError.Network, onRetry = { retried = true })
        }

        rule.onNodeWithText("Can't reach the server. Check your connection.").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun retryingErrorContentKeepsMessageAndShowsProgress() {
        rule.setContent {
            ErrorContent(AppError.Network, onRetry = {}, isRetrying = true)
        }

        rule.onNodeWithText("Can't reach the server. Check your connection.").assertIsDisplayed()
        rule.onNodeWithText("Retrying...").assertIsDisplayed()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
        rule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertIsDisplayed()
    }

    @Test
    fun emptyContentShowsMessage() {
        rule.setContent {
            EmptyContent(message = "No items")
        }

        rule.onNodeWithText("No items").assertIsDisplayed()
    }

    @Test
    fun delayedVisibilityWaitsBeforeShowingContent() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            DelayedVisibility(visible = true) {
                Text("Deferred status")
            }
        }

        rule.onAllNodesWithText("Deferred status").assertCountEquals(0)

        rule.mainClock.advanceTimeBy(UI_DEFERRED_INDICATOR_DELAY_MILLIS.toLong() + UI_STATUS_FADE_IN_MILLIS.toLong())
        rule.onNodeWithText("Deferred status").assertIsDisplayed()
    }

    @Test
    fun uiStateContentRendersSuccessContent() {
        rule.setContent {
            UiStateContent(UiState.Success("payload")) { data ->
                Text("data: $data")
            }
        }

        rule.onNodeWithText("data: payload").assertIsDisplayed()
    }

    @Test
    fun uiStateContentShowsLoadingIndicator() {
        rule.setContent {
            UiStateContent(UiState.Loading) { Text("never") }
        }

        rule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertIsDisplayed()
    }
}
