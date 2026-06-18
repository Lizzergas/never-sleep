package com.lizz.myapptemplate.showcase.presentation.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.ui.ErrorContent
import com.lizz.myapptemplate.ui.UiStateContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Calls the template server using shared DTOs from core:model through the
 * typed core:network layer, rendered with the standard core:ui state
 * components. Start the server with: ./gradlew :server:run
 */
@Composable
fun NetworkDemoScreen() {
    val viewModel = koinViewModel<NetworkDemoViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    NetworkDemoContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun NetworkDemoContent(
    state: NetworkDemoUiState,
    onEvent: (NetworkDemoEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            "GET /api/items from the template server, decoded into shared " +
                "core:model DTOs. Failures map to typed AppError values " +
                "rendered by core:ui state components.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = { onEvent(NetworkDemoEvent.Load) }) {
            Text("Load items")
        }

        when (val s = state.items) {
            null -> Text("Not requested yet", style = MaterialTheme.typography.bodyMedium)

            else ->
                UiStateContent(
                    state = s,
                    onRetry = { onEvent(NetworkDemoEvent.Load) },
                    error = { appError ->
                        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                            ErrorContent(appError, onRetry = { onEvent(NetworkDemoEvent.Load) })
                            if (appError is AppError.Network || appError is AppError.Timeout) {
                                Text(
                                    "Is the server running? Start it with: ./gradlew :server:run",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                ) { items ->
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                        items.forEach { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(Theme.spacing.md)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}
