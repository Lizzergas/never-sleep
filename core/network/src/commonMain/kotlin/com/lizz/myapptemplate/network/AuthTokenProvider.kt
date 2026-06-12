package com.lizz.myapptemplate.network

import io.ktor.client.plugins.auth.providers.BearerTokens

/**
 * Supplies and refreshes bearer tokens for the app HttpClient. Bind an
 * implementation in DI (feature:auth does) and every API call automatically
 * carries the access token and transparently refreshes on 401. Without a
 * binding the client simply makes unauthenticated calls.
 */
interface AuthTokenProvider {
    /** Current tokens, or null when logged out. */
    suspend fun loadTokens(): BearerTokens?

    /**
     * Called after a 401: exchange the refresh token for a new pair.
     * Return null when refreshing fails (the session is over).
     */
    suspend fun refreshTokens(oldRefreshToken: String?): BearerTokens?
}
