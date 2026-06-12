package com.lizz.myapptemplate.auth

import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.Item
import com.lizz.myapptemplate.model.TokenPair
import com.lizz.myapptemplate.network.NetworkConfig
import com.lizz.myapptemplate.network.createHttpClient
import com.lizz.myapptemplate.network.safeGet
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class FakeTokenStorage : TokenStorage {
    var tokens: TokenPair? = null

    override suspend fun read(): TokenPair? = tokens

    override suspend fun write(tokens: TokenPair) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

/**
 * Minimal stateful auth backend: one valid access token at a time, rotating
 * refresh tokens, /api/items requiring a valid bearer.
 */
internal class FakeAuthBackend {
    var validAccess = "access-1"
    var validRefresh = "refresh-1"
    private var counter = 1

    val engine =
        MockEngine { request ->
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            val auth = request.headers[HttpHeaders.Authorization]
            when (request.url.encodedPath) {
                "/api/auth/login" ->
                    respond(tokenBody(), HttpStatusCode.OK, json)

                "/api/auth/refresh" -> {
                    val body = String(request.body.toByteArray())
                    if (body.contains(validRefresh)) {
                        counter += 1
                        validAccess = "access-$counter"
                        validRefresh = "refresh-$counter"
                        respond(tokenBody(), HttpStatusCode.OK, json)
                    } else {
                        respond("""{"error":"stale"}""", HttpStatusCode.Unauthorized, json)
                    }
                }

                "/api/me" ->
                    if (auth == "Bearer $validAccess") {
                        respond("""{"id":"u1","email":"user@test.dev"}""", HttpStatusCode.OK, json)
                    } else {
                        respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, json)
                    }

                "/api/items" ->
                    if (auth == "Bearer $validAccess") {
                        respond(
                            """[{"id":1,"title":"secret","description":"requires auth"}]""",
                            HttpStatusCode.OK,
                            json,
                        )
                    } else {
                        respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, json)
                    }

                else -> respond("""{"error":"not found"}""", HttpStatusCode.NotFound, json)
            }
        }

    private fun tokenBody() = """{"accessToken":"$validAccess","refreshToken":"$validRefresh"}"""
}

class AuthRepositoryTest {
    @Test
    fun loginStoresTokensAndEntersLoggedIn() =
        runBlocking<Unit> {
            val backend = FakeAuthBackend()
            val storage = FakeTokenStorage()
            val repository = AuthRepository(NetworkConfig(), storage, backend.engine)

            val result = repository.login("user@test.dev", "password123")

            assertIs<ApiResult.Success<*>>(result)
            assertEquals("access-1", storage.tokens?.accessToken)
            assertIs<SessionState.LoggedIn>(repository.sessionState.value)
        }

    @Test
    fun restoreWithoutTokensIsLoggedOut() =
        runBlocking<Unit> {
            val backend = FakeAuthBackend()
            val repository = AuthRepository(NetworkConfig(), FakeTokenStorage(), backend.engine)

            repository.restore()

            assertEquals(SessionState.LoggedOut, repository.sessionState.value)
        }

    @Test
    fun restoreWithExpiredAccessRefreshesAndLogsIn() =
        runBlocking<Unit> {
            val backend = FakeAuthBackend()
            val storage =
                FakeTokenStorage().apply {
                    // Stored access token is stale, refresh token still valid.
                    tokens = TokenPair(accessToken = "expired", refreshToken = backend.validRefresh)
                }
            val repository = AuthRepository(NetworkConfig(), storage, backend.engine)

            repository.restore()

            assertIs<SessionState.LoggedIn>(repository.sessionState.value)
            assertEquals(backend.validAccess, storage.tokens?.accessToken)
        }

    @Test
    fun failedRefreshLogsOutAndClearsStorage() =
        runBlocking<Unit> {
            val backend = FakeAuthBackend()
            val storage =
                FakeTokenStorage().apply {
                    tokens = TokenPair(accessToken = "expired", refreshToken = "stale-refresh")
                }
            val repository = AuthRepository(NetworkConfig(), storage, backend.engine)

            val refreshed = repository.refreshTokens("stale-refresh")

            assertNull(refreshed)
            assertNull(storage.tokens)
            assertEquals(SessionState.LoggedOut, repository.sessionState.value)
        }

    @Test
    fun appClientAutoRefreshesOn401AndRetries() =
        runBlocking<Unit> {
            val backend = FakeAuthBackend()
            val storage = FakeTokenStorage()
            val repository = AuthRepository(NetworkConfig(), storage, backend.engine)
            repository.login("user@test.dev", "password123")

            // Simulate access-token expiry server-side: rotate what the backend accepts.
            backend.validAccess = "access-rotated"
            // The stored refresh token is still valid, so the Auth plugin should
            // refresh transparently and the call must succeed.
            val appClient =
                createHttpClient(NetworkConfig(), backend.engine, authTokenProvider = repository)

            val result = appClient.safeGet<List<Item>>("/api/items")

            assertIs<ApiResult.Success<List<Item>>>(result)
            assertEquals("secret", result.data.single().title)
            assertEquals(backend.validAccess, storage.tokens?.accessToken)
        }
}
