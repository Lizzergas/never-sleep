package com.lizz.neversleep.auth.data

import com.lizz.neversleep.auth.domain.SessionRepository
import com.lizz.neversleep.auth.domain.SessionState
import com.lizz.neversleep.auth.domain.User
import com.lizz.neversleep.model.ApiResult
import com.lizz.neversleep.model.AppError
import com.lizz.neversleep.model.AuthRequest
import com.lizz.neversleep.model.RefreshRequest
import com.lizz.neversleep.model.TokenPair
import com.lizz.neversleep.model.UserDto
import com.lizz.neversleep.model.map
import com.lizz.neversleep.network.AuthTokenProvider
import com.lizz.neversleep.network.NetworkConfig
import com.lizz.neversleep.network.createHttpClient
import com.lizz.neversleep.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun UserDto.toDomain() = User(id = id, email = email)

/**
 * Owns the session: register/login/logout against the /api/auth endpoints,
 * token persistence, and the [AuthTokenProvider] hookup that gives the app
 * HttpClient automatic bearer headers + 401 refresh.
 *
 * Uses its own bare client (no Auth plugin) for the auth endpoints so token
 * refresh can never recurse into itself.
 *
 * [onSessionChanged] runs after login/logout — the app shell wires it to
 * clear the app client's cached bearer tokens and feature UserDataCleaners
 * so nothing from the previous session leaks across users.
 */
class SessionRepositoryImpl(
    config: NetworkConfig,
    private val storage: TokenStorage,
    engine: HttpClientEngine? = null,
    private val onSessionChanged: suspend () -> Unit = {},
) : SessionRepository,
    AuthTokenProvider {
    private val bareClient: HttpClient = createHttpClient(config, engine)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun restore() {
        val tokens = storage.read()
        if (tokens == null) {
            _sessionState.value = SessionState.LoggedOut
            return
        }
        if (establishSession(tokens) is ApiResult.Failure) {
            _sessionState.value = SessionState.LoggedOut
        }
    }

    override suspend fun register(
        email: String,
        password: String,
    ): ApiResult<User> = authenticate("/api/auth/register", email, password)

    override suspend fun login(
        email: String,
        password: String,
    ): ApiResult<User> = authenticate("/api/auth/login", email, password)

    override suspend fun logout() {
        storage.clear()
        _sessionState.value = SessionState.LoggedOut
        onSessionChanged()
    }

    override suspend fun loadTokens(): BearerTokens? =
        storage.read()?.let { BearerTokens(it.accessToken, it.refreshToken) }

    override suspend fun refreshTokens(oldRefreshToken: String?): BearerTokens? {
        val refreshToken = oldRefreshToken ?: storage.read()?.refreshToken ?: return null
        return when (
            val result = safeApiCall<TokenPair> {
                bareClient.post("/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(refreshToken))
                }
            }
        ) {
            is ApiResult.Success -> {
                storage.write(result.data)
                BearerTokens(result.data.accessToken, result.data.refreshToken)
            }

            is ApiResult.Failure -> {
                storage.clear()
                _sessionState.value = SessionState.LoggedOut
                onSessionChanged()
                null
            }
        }
    }

    private suspend fun authenticate(
        path: String,
        email: String,
        password: String,
    ): ApiResult<User> {
        val tokens = safeApiCall<TokenPair> {
            bareClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }
        }
        val result = when (tokens) {
            is ApiResult.Failure -> tokens
            is ApiResult.Success -> {
                storage.write(tokens.data)
                establishSession(tokens.data)
            }
        }
        if (result is ApiResult.Success) {
            // New identity: drop cached bearer tokens + per-user feature caches.
            onSessionChanged()
        }
        return result
    }

    /**
     * Loads /api/me with the access token, refreshing once if it has already
     * expired. Sets LoggedIn on success.
     */
    private suspend fun establishSession(tokens: TokenPair): ApiResult<User> {
        val first = fetchMe(tokens.accessToken)
        val result = if (first is ApiResult.Failure && first.error == AppError.Unauthorized) {
            refreshTokens(tokens.refreshToken)
                ?.let { refreshed -> fetchMe(refreshed.accessToken) }
                ?: first
        } else {
            first
        }
        if (result is ApiResult.Success) {
            _sessionState.value = SessionState.LoggedIn(result.data)
        }
        return result
    }

    private suspend fun fetchMe(accessToken: String): ApiResult<User> =
        safeApiCall<UserDto> {
            bareClient.get("/api/me") { bearerAuth(accessToken) }
        }.map { it.toDomain() }
}
