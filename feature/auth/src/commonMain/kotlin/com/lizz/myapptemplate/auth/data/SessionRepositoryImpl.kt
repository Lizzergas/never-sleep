package com.lizz.myapptemplate.auth.data

import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.AuthRequest
import com.lizz.myapptemplate.model.RefreshRequest
import com.lizz.myapptemplate.model.TokenPair
import com.lizz.myapptemplate.model.UserDto
import com.lizz.myapptemplate.network.AuthTokenProvider
import com.lizz.myapptemplate.network.NetworkConfig
import com.lizz.myapptemplate.network.createHttpClient
import com.lizz.myapptemplate.network.safeApiCall
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

/**
 * Owns the session: register/login/logout against the /api/auth endpoints,
 * token persistence, and the [AuthTokenProvider] hookup that gives the app
 * HttpClient automatic bearer headers + 401 refresh.
 *
 * Uses its own bare client (no Auth plugin) for the auth endpoints so token
 * refresh can never recurse into itself.
 */
class SessionRepositoryImpl(
    config: NetworkConfig,
    private val storage: TokenStorage,
    engine: HttpClientEngine? = null,
) : SessionRepository,
    AuthTokenProvider {
    private val bareClient: HttpClient = createHttpClient(config, engine)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun restore() {
        if (_sessionState.value != SessionState.Unknown) return
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
    ): ApiResult<UserDto> = authenticate("/api/auth/register", email, password)

    override suspend fun login(
        email: String,
        password: String,
    ): ApiResult<UserDto> = authenticate("/api/auth/login", email, password)

    override suspend fun logout() {
        storage.clear()
        _sessionState.value = SessionState.LoggedOut
    }

    override suspend fun loadTokens(): BearerTokens? =
        storage.read()?.let { BearerTokens(it.accessToken, it.refreshToken) }

    override suspend fun refreshTokens(oldRefreshToken: String?): BearerTokens? {
        val refreshToken = oldRefreshToken ?: storage.read()?.refreshToken ?: return null
        val result =
            safeApiCall<TokenPair> {
                bareClient.post("/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(refreshToken))
                }
            }
        return when (result) {
            is ApiResult.Success -> {
                storage.write(result.data)
                BearerTokens(result.data.accessToken, result.data.refreshToken)
            }

            is ApiResult.Failure -> {
                // Refresh token spent or expired: the session is over.
                logout()
                null
            }
        }
    }

    private suspend fun authenticate(
        path: String,
        email: String,
        password: String,
    ): ApiResult<UserDto> {
        val tokens =
            safeApiCall<TokenPair> {
                bareClient.post(path) {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequest(email, password))
                }
            }
        return when (tokens) {
            is ApiResult.Failure -> tokens
            is ApiResult.Success -> {
                storage.write(tokens.data)
                establishSession(tokens.data)
            }
        }
    }

    /**
     * Loads /api/me with the access token, refreshing once if it has already
     * expired. Sets LoggedIn on success.
     */
    private suspend fun establishSession(tokens: TokenPair): ApiResult<UserDto> {
        val first = fetchMe(tokens.accessToken)
        val result =
            if (first is ApiResult.Failure && first.error == AppError.Unauthorized) {
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

    private suspend fun fetchMe(accessToken: String): ApiResult<UserDto> =
        safeApiCall {
            bareClient.get("/api/me") { bearerAuth(accessToken) }
        }
}
