package com.lizz.myapptemplate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError

/** Renders the standard chrome per state; provide only the success content. */
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { LoadingContent() },
    empty: @Composable () -> Unit = { EmptyContent() },
    error: @Composable (AppError) -> Unit = { ErrorContent(it, onRetry) },
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            is UiState.Loading -> loading()
            is UiState.Empty -> empty()
            is UiState.Error -> error(state.error)
            is UiState.Success -> content(state.data)
        }
    }
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(Theme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyContent(
    message: String = "Nothing here yet",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(Theme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ErrorContent(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            error.userMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
