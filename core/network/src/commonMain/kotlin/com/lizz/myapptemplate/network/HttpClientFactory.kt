package com.lizz.myapptemplate.network

import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * App-wide network configuration. Provide your own instance in the app's
 * Koin module to point at your API.
 *
 * Note for Android emulator: localhost on the device is the emulator itself —
 * use http://10.0.2.2:8080 to reach the host machine.
 */
data class NetworkConfig(
    val baseUrl: String? = null,
    val requestTimeoutMs: Long = 30_000,
    val connectTimeoutMs: Long = 10_000,
    /** Called per request; return a bearer token to add an Authorization header. */
    val authToken: () -> String? = { null },
)

val DefaultJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * The single HttpClient for the whole app (engine chosen per platform unless
 * [engine] is given — tests pass a MockEngine). Owned by Koin
 * ([networkKoinModule]) and closed when Koin stops.
 */
fun createHttpClient(
    config: NetworkConfig = NetworkConfig(),
    engine: HttpClientEngine? = null,
): HttpClient = if (engine != null) {
    HttpClient(engine) { applyTemplateDefaults(config) }
} else {
    HttpClient { applyTemplateDefaults(config) }
}

private fun HttpClientConfig<*>.applyTemplateDefaults(config: NetworkConfig) {
    install(ContentNegotiation) {
        json(DefaultJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMs
        connectTimeoutMillis = config.connectTimeoutMs
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                KermitLogger.d(tag = "HttpClient") { message }
            }
        }
        level = LogLevel.INFO
    }
    defaultRequest {
        config.baseUrl?.let { url.takeFrom(it) }
        config.authToken()?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
