package com.lizz.myapptemplate.notes

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.notes.presentation.NotesContent
import com.lizz.myapptemplate.notes.presentation.NotesUiState
import org.junit.Rule
import org.junit.Test

class NotesContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun errorStateKeepsMessageVisibleWhileRetrying() {
        rule.setContent {
            AppTheme {
                NotesContent(
                    state = NotesUiState(isRefreshing = true, error = AppError.Network),
                    draft = "",
                    onDraftChange = {},
                    onEvent = {},
                )
            }
        }

        rule.onNodeWithText("Can't reach the server. Check your connection.").assertIsDisplayed()
        rule.onNodeWithText("Retrying...").assertIsDisplayed()
        rule.onAllNodesWithText("Refresh").assertCountEquals(0)
        rule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertIsDisplayed()
    }

    @Test
    fun healthyStateShowsBottomRefreshWithoutRetryAction() {
        rule.setContent {
            AppTheme {
                NotesContent(
                    state = NotesUiState(),
                    draft = "",
                    onDraftChange = {},
                    onEvent = {},
                )
            }
        }

        rule.onNodeWithText("Refresh").assertIsDisplayed()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
    }
}
