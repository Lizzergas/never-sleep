package com.lizz.myapptemplate.auth.data

import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.auth.domain.User
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.AuthRequest
import com.lizz.myapptemplate.model.RefreshRequest
import com.lizz.myapptemplate.model.TokenPair
import com.lizz.myapptemplate.model.UserDto
import com.lizz.myapptemplate.model.map
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun UserDto.toDomain() = User(id = id, email = email)

/**
 * Owns the session: register/login/logout against the /api/auth endpoints,
 * token persistence, and the [AuthTokenProvider] hookup that gives the app
 * HttpClient automatic bearer headers + 401 refresh.
 *
 * Uses its own bare client (no Auth plugin) for the auth endpoints so token
 * refresh can never recurse into itself.
 *
 * Locking: [sessionMutex] serializes restore/login/register/logout;
 * [refreshMutex] makes refresh single-flight (the server rotates single-use
 * refresh tokens). refreshTokens never takes sessionMutex — it is reachable
 * from inside establishSession, which already holds it.
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
    private val sessionMutex = Mutex()
    private val refreshMutex = Mutex()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun restore() {
        sessionMutex.withLock {
            // A concurrent login may have established the session already.
            if (_sessionState.value is SessionState.LoggedIn) return
            val tokens = storage.read()
            if (tokens == null) {
                _sessionState.value = SessionState.LoggedOut
                return
            }
            if (establishSession(tokens) is ApiResult.Failure) {
                // Show the logged-out UI, but: tokens are only cleared on a
                // definitive Unauthorized (inside refreshTokens). Transient
                // failures (offline) keep them, and the next restore()
                // retries — never terminal for the process.
                _sessionState.value = SessionState.LoggedOut
            }
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
        sessionMutex.withLock {
            storage.clear()
            _sessionState.value = SessionState.LoggedOut
        }
        onSessionChanged()
    }

    override suspend fun loadTokens(): BearerTokens? =
        storage.read()?.let { BearerTokens(it.accessToken, it.refreshToken) }

    override suspend fun refreshTokens(oldRefreshToken: String?): BearerTokens? =
        refreshMutex.withLock {
            // Single-flight: if another refresh already rotated the tokens
            // while we waited, reuse its result instead of burning the
            // (single-use) refresh token a second time.
            val stored = storage.read()
            val refreshToken = stored?.refreshToken ?: oldRefreshToken ?: return null
            if (oldRefreshToken != null && stored != null && stored.refreshToken != oldRefreshToken) {
                return BearerTokens(stored.accessToken, stored.refreshToken)
            }

            val result =
                safeApiCall<TokenPair> {
                    bareClient.post("/api/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(refreshToken))
                    }
                }
            when (result) {
                is ApiResult.Success -> {
                    storage.write(result.data)
                    BearerTokens(result.data.accessToken, result.data.refreshToken)
                }

                is ApiResult.Failure -> {
                    // Only a definitive rejection ends the session — a network
                    // blip must never destroy valid credentials. Guarded so a
                    // stale refresh can't wipe tokens a concurrent login just
                    // wrote (no sessionMutex here: deadlock-free by design).
                    if (result.error == AppError.Unauthorized &&
                        storage.read()?.refreshToken == refreshToken
                    ) {
                        storage.clear()
                        _sessionState.value = SessionState.LoggedOut
                        onSessionChanged()
                    }
                    null
                }
            }
        }

    private suspend fun authenticate(
        path: String,
        email: String,
        password: String,
    ): ApiResult<User> {
        val result =
            sessionMutex.withLock {
                val tokens =
                    safeApiCall<TokenPair> {
                        bareClient.post(path) {
                            contentType(ContentType.Application.Json)
                            setBody(AuthRequest(email, password))
                        }
                    }
                when (tokens) {
                    is ApiResult.Failure -> tokens
                    is ApiResult.Success -> {
                        storage.write(tokens.data)
                        establishSession(tokens.data)
                    }
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
     * expired. Sets LoggedIn on success. Caller must hold [sessionMutex].
     */
    private suspend fun establishSession(tokens: TokenPair): ApiResult<User> {
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

    private suspend fun fetchMe(accessToken: String): ApiResult<User> =
        safeApiCall<UserDto> {
            bareClient.get("/api/me") { bearerAuth(accessToken) }
        }.map { it.toDomain() }
}
