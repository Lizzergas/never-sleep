package com.lizz.neversleep.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import co.touchlab.kermit.Logger as KermitLogger

/**
 * The single HttpClient for the whole app (engine chosen per platform unless
 * [engine] is given — tests pass a MockEngine). Owned by Koin
 * ([networkKoinModule]) and closed when Koin stops.
 *
 * With an [authTokenProvider] every request carries the access token and
 * transparently refreshes it on 401 (Ktor's Auth/Bearer plugin).
 */
fun createHttpClient(
    config: NetworkConfig = NetworkConfig(),
    engine: HttpClientEngine? = null,
    authTokenProvider: AuthTokenProvider? = null,
): HttpClient =
    if (engine != null) {
        HttpClient(engine) { applyTemplateDefaults(config, authTokenProvider) }
    } else {
        HttpClient { applyTemplateDefaults(config, authTokenProvider) }
    }

private fun HttpClientConfig<*>.applyTemplateDefaults(
    config: NetworkConfig,
    authTokenProvider: AuthTokenProvider?,
) {
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
    if (authTokenProvider != null) {
        install(Auth) {
            bearer {
                loadTokens { authTokenProvider.loadTokens() }
                refreshTokens { authTokenProvider.refreshTokens(oldTokens?.refreshToken) }
                // Attach the token to every request instead of waiting for a 401 challenge.
                sendWithoutRequest { true }
            }
        }
    }
    defaultRequest {
        config.baseUrl?.let { url.takeFrom(it) }
    }
}
