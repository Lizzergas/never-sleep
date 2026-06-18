package com.lizz.myapptemplate.network

import kotlinx.serialization.json.Json

private const val DEFAULT_REQUEST_TIMEOUT_MS = 30_000L
private const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000L

/**
 * App-wide network configuration. Provide your own instance in the app's
 * Koin module to point at your API.
 *
 * Note for Android emulator: localhost on the device is the emulator itself —
 * use http://10.0.2.2:8080 to reach the host machine.
 */
data class NetworkConfig(
    val baseUrl: String? = null,
    val requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    val connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
)

val DefaultJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
