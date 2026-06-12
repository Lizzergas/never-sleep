package com.lizz.myapptemplate.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lizz.myapptemplate.connectivity.ConnectivityMonitor
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.Item
import com.lizz.myapptemplate.network.safeGet
import com.lizz.myapptemplate.ui.ErrorContent
import com.lizz.myapptemplate.ui.UiState
import com.lizz.myapptemplate.ui.UiStateContent
import com.lizz.myapptemplate.ui.toUiState
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform

/**
 * Calls the template server using shared DTOs from core:model through the
 * typed core:network layer, rendered with the standard core:ui state
 * components. Start the server with: ./gradlew :server:run
 */
@Composable
fun NetworkDemoScreen(onBack: () -> Unit) {
    val httpClient = koinInject<HttpClient>()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UiState<List<Item>>?>(null) }

    fun load() {
        scope.launch {
            state = UiState.Loading
            state = httpClient.safeGet<List<Item>>("/api/items").toUiState { it.isEmpty() }
        }
    }

    // Retry-on-reconnect: when connectivity returns while a network failure is
    // showing, re-issue the call. Optional lookup — without core:connectivity
    // the demo simply never auto-retries.
    val connectivityMonitor =
        remember {
            runCatching { KoinPlatform.getKoin().getOrNull<ConnectivityMonitor>() }.getOrNull()
        }
    if (connectivityMonitor != null) {
        LaunchedEffect(connectivityMonitor) {
            connectivityMonitor.isOnline.collect { online ->
                val current = state
                val failedOnNetwork =
                    current is UiState.Error &&
                        (current.error is AppError.Network || current.error is AppError.Timeout)
                if (online && failedOnNetwork) {
                    load()
                }
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text("Network demo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "GET /api/items from the template server, decoded into shared " +
                "core:model DTOs. Failures map to typed AppError values " +
                "rendered by core:ui state components.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = ::load) {
            Text("Load items")
        }

        when (val s = state) {
            null -> Text("Not requested yet", style = MaterialTheme.typography.bodyMedium)

            else ->
                UiStateContent(
                    state = s,
                    onRetry = ::load,
                    error = { appError ->
                        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                            ErrorContent(appError, onRetry = ::load)
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
                                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
        }

        Button(onClick = onBack) { Text("Back") }
    }
}
