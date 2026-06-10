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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.Item
import com.lizz.myapptemplate.network.ApiResult
import com.lizz.myapptemplate.network.safeGet
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Calls the template server using shared DTOs from core:model through the
 * typed core:network layer. Start the server with: ./gradlew :server:run
 */
@Composable
fun NetworkDemoScreen(onBack: () -> Unit) {
    val httpClient = koinInject<HttpClient>()
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ApiResult<List<Item>>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text("Network demo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "GET /api/items from the template server, decoded into shared " +
                "core:model DTOs. Failures map to typed AppError values.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = {
            scope.launch { result = httpClient.safeGet("/api/items") }
        }) {
            Text("Load items")
        }

        when (val r = result) {
            null -> Text("Not requested yet", style = MaterialTheme.typography.bodyMedium)

            is ApiResult.Success -> r.data.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Theme.spacing.md)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            is ApiResult.Failure -> {
                Text(
                    "Failed: ${r.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                if (r.error is AppError.Network || r.error is AppError.Timeout) {
                    Text(
                        "Is the server running? Start it with: ./gradlew :server:run",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Button(onClick = onBack) { Text("Back") }
    }
}
