package com.lizz.myapptemplate.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.auth.presentation.AccountContent
import com.lizz.myapptemplate.auth.presentation.AccountUiState
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.model.AppError
import org.junit.Rule
import org.junit.Test

class AccountContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun loggedOutSubmittingKeepsServerErrorVisible() {
        rule.setContent {
            AppTheme {
                AccountContent(
                    state =
                        AccountUiState(
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
