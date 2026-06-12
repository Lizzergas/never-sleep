package com.lizz.myapptemplate.network

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
import co.touchlab.kermit.Logger as KermitLogger

/**
 * The single HttpClient for the whole app (engine chosen per platform unless
 * [engine] is given — tests pass a MockEngine). Owned by Koin
 * ([networkKoinModule]) and closed when Koin stops.
 */
fun createHttpClient(
    config: NetworkConfig = NetworkConfig(),
    engine: HttpClientEngine? = null,
): HttpClient =
    if (engine != null) {
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
        logger =
            object : Logger {
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
