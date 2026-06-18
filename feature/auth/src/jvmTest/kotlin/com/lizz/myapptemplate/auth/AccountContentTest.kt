package com.lizz.myapptemplate.auth

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.auth.domain.User
import com.lizz.myapptemplate.auth.presentation.AccountContent
import com.lizz.myapptemplate.auth.presentation.AccountUiState
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.ui.UI_DEFERRED_INDICATOR_DELAY_MILLIS
import com.lizz.myapptemplate.ui.UI_STATUS_FADE_IN_MILLIS
import org.junit.Rule
import org.junit.Test

class AccountContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun unknownSessionRendersAccountShellWithoutImmediateLoader() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme {
                AccountContent(
                    state = AccountUiState(session = SessionState.Unknown),
                    onEvent = {},
                )
            }
        }

        rule.onNodeWithText("Sign in to keep your notes available across devices.").assertIsDisplayed()
        rule.onAllNodesWithText("Checking saved session...").assertCountEquals(0)
        rule
            .onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertCountEquals(0)
    }

    @Test
    fun unknownSessionShowsInlineStatusAfterDelay() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme {
                AccountContent(
                    state = AccountUiState(session = SessionState.Unknown),
                    onEvent = {},
                )
            }
        }

        rule.mainClock.advanceTimeBy(
            UI_DEFERRED_INDICATOR_DELAY_MILLIS.toLong() + UI_STATUS_FADE_IN_MILLIS.toLong(),
        )
        rule.onNodeWithText("Checking saved session...").assertIsDisplayed()
    }

    @Test
    fun loggedOutStateRendersAuthForm() {
        rule.setContent {
            AppTheme {
                AccountContent(
                    state = AccountUiState(session = SessionState.LoggedOut),
                    onEvent = {},
                )
            }
        }

        rule.onNodeWithText("Email").assertIsDisplayed()
        rule.onNodeWithText("Password (min 8 chars)").assertIsDisplayed()
        rule.onNodeWithText("Log in").assertIsDisplayed()
        rule.onNodeWithText("Register").assertIsDisplayed()
    }

    @Test
    fun loggedInStateRendersProfile() {
        rule.setContent {
            AppTheme {
                AccountContent(
                    state = AccountUiState(session = SessionState.LoggedIn(User("42", "user@test.dev"))),
                    onEvent = {},
                )
            }
        }

        rule.onNodeWithText("Profile").assertIsDisplayed()
        rule.onNodeWithText("Signed in as user@test.dev").assertIsDisplayed()
        rule.onNodeWithText("Log out").assertIsDisplayed()
    }

    @Test
    fun loggedOutSubmittingKeepsServerErrorVisible() {
        rule.setContent {
            AppTheme {
                AccountContent(
                    state = AccountUiState(
                        session = SessionState.LoggedOut,
                        isSubmitting = true,
                        error = AppError.Network,
                    ),
                    onEvent = {},
                )
            }
        }

        rule.onNodeWithText("Can't reach the server. Check your connection.").assertIsDisplayed()
        rule.onNodeWithText("Log in").assertIsNotEnabled()
        rule.onNodeWithText("Register").assertIsNotEnabled()
    }
}
