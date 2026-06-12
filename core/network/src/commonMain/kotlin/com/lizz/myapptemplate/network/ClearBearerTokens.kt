package com.lizz.myapptemplate.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

/**
 * Drops the Auth plugin's cached bearer tokens so the next request re-runs
 * [AuthTokenProvider.loadTokens]. MUST be called whenever the session changes
 * (login, logout, user switch) — Ktor caches loadTokens() after first use and
 * would otherwise keep sending the previous session's tokens.
 */
fun HttpClient.clearBearerTokens() {
    authProviders.filterIsInstance<BearerAuthProvider>().forEach { it.clearToken() }
}
