package com.lizz.myapptemplate.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.auth.domain.User
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.ui.ErrorContent
import com.lizz.myapptemplate.ui.LoadingContent
import org.koin.compose.viewmodel.koinViewModel

/** Stateful wrapper: owns the ViewModel. All rendering is in [AccountContent]. */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<SessionViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    AccountContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

/**
 * Conditional navigation on session state: login/register when logged out,
 * profile when logged in — the pattern to copy for protected areas.
 */
@Composable
fun AccountContent(
    state: AccountUiState,
    onEvent: (AccountEvent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        when (val session = state.session) {
            SessionState.Unknown -> LoadingContent()
            SessionState.LoggedOut -> AuthForm(state, onEvent)
            is SessionState.LoggedIn -> Profile(session.user, onLogout = { onEvent(AccountEvent.Logout) })
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun AuthForm(
    state: AccountUiState,
    onEvent: (AccountEvent) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Text("Account", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Register or log in against the template server (JWT + refresh rotation).",
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        isError = state.validation.emailError != null,
        supportingText = state.validation.emailError?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min 8 chars)") },
        isError = state.validation.passwordError != null,
        supportingText = state.validation.passwordError?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )

    state.error?.let { ErrorContent(it) }

    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        Button(
            onClick = { onEvent(AccountEvent.Login(email.trim(), password)) },
            enabled = !state.inFlight,
        ) { Text("Log in") }
        OutlinedButton(
            onClick = { onEvent(AccountEvent.Register(email.trim(), password)) },
            enabled = !state.inFlight,
        ) { Text("Register") }
    }
}

@Composable
private fun Profile(
    user: User,
    onLogout: () -> Unit,
) {
    Text("Profile", style = MaterialTheme.typography.headlineMedium)
    Text("Signed in as ${user.email}", style = MaterialTheme.typography.bodyLarge)
    Text("User id: ${user.id}", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = onLogout) { Text("Log out") }
}

@Preview
@Composable
private fun AccountLoggedOutPreview() {
    AppTheme {
        AccountContent(
            state = AccountUiState(session = SessionState.LoggedOut),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AccountLoggedInPreview() {
    AppTheme {
        AccountContent(
            state = AccountUiState(session = SessionState.LoggedIn(User("42", "preview@lizz.dev"))),
            onEvent = {},
            onBack = {},
        )
    }
}
