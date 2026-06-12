package com.lizz.myapptemplate.auth

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.UserDto
import com.lizz.myapptemplate.ui.ErrorContent
import com.lizz.myapptemplate.ui.LoadingContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Conditional navigation on session state: login/register when logged out,
 * profile when logged in — the pattern to copy for protected areas.
 */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<SessionViewModel>()
    val session by viewModel.sessionState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        when (val current = session) {
            SessionState.Unknown -> LoadingContent()
            SessionState.LoggedOut -> AuthForm(viewModel)
            is SessionState.LoggedIn -> Profile(current.user, onLogout = viewModel::logout)
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun AuthForm(viewModel: SessionViewModel) {
    val inFlight by viewModel.inFlight.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
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
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min 8 chars)") },
        modifier = Modifier.fillMaxWidth(),
    )

    lastError?.let { ErrorContent(it) }

    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        Button(
            onClick = { viewModel.login(email.trim(), password) },
            enabled = !inFlight,
        ) { Text("Log in") }
        OutlinedButton(
            onClick = { viewModel.register(email.trim(), password) },
            enabled = !inFlight,
        ) { Text("Register") }
    }
}

@Composable
private fun Profile(
    user: UserDto,
    onLogout: () -> Unit,
) {
    Text("Profile", style = MaterialTheme.typography.headlineMedium)
    Text("Signed in as ${user.email}", style = MaterialTheme.typography.bodyLarge)
    Text("User id: ${user.id}", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = onLogout) { Text("Log out") }
}
