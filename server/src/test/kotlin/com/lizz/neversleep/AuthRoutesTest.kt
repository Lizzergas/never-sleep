package com.lizz.neversleep

import com.lizz.neversleep.model.AuthRequest
import com.lizz.neversleep.model.RefreshRequest
import com.lizz.neversleep.model.TokenPair
import com.lizz.neversleep.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    private fun ApplicationTestBuilder.authClient(): HttpClient {
        application { module() }
        return createClient {
            install(ContentNegotiation) { json() }
        }
    }

    private suspend fun HttpClient.register(
        email: String = "user@example.com",
        password: String = "password123",
    ) = post("/api/auth/register") {
        contentType(io.ktor.http.ContentType.Application.Json)
        setBody(AuthRequest(email, password))
    }

    @Test
    fun registerReturnsTokens() =
        testApplication {
            val client = authClient()

            val response = client.register()

            assertEquals(HttpStatusCode.OK, response.status)
            val tokens: TokenPair = response.body()
            assertTrue(tokens.accessToken.isNotBlank())
            assertTrue(tokens.refreshToken.isNotBlank())
        }

    @Test
    fun duplicateRegisterConflicts() =
        testApplication {
            val client = authClient()
            client.register()

            val response = client.register()

            assertEquals(HttpStatusCode.Conflict, response.status)
        }

    @Test
    fun weakPasswordIsRejected() =
        testApplication {
            val client = authClient()

            val response = client.register(password = "short")

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }

    @Test
    fun loginSucceedsWithCorrectPassword() =
        testApplication {
            val client = authClient()
            client.register()

            val response = client.post("/api/auth/login") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(AuthRequest("user@example.com", "password123"))
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun loginFailsWithWrongPassword() =
        testApplication {
            val client = authClient()
            client.register()

            val response = client.post("/api/auth/login") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(AuthRequest("user@example.com", "wrong-password"))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun meReturnsUserWithValidToken() =
        testApplication {
            val client = authClient()
            val tokens: TokenPair = client.register().body()

            val response = client.get("/api/me") { bearerAuth(tokens.accessToken) }

            assertEquals(HttpStatusCode.OK, response.status)
            val user: UserDto = response.body()
            assertEquals("user@example.com", user.email)
        }

    @Test
    fun meWithoutTokenIsUnauthorized() =
        testApplication {
            val client = authClient()

            val response = client.get("/api/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun refreshRotatesTokens() =
        testApplication {
            val client = authClient()
            val original: TokenPair = client.register().body()

            val refreshed: TokenPair = client
                .post("/api/auth/refresh") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(RefreshRequest(original.refreshToken))
                }.body()

            assertNotEquals(original.refreshToken, refreshed.refreshToken)
            // New access token still works
            val me = client.get("/api/me") { bearerAuth(refreshed.accessToken) }
            assertEquals(HttpStatusCode.OK, me.status)
        }

    @Test
    fun staleRefreshTokenIsRejected() =
        testApplication {
            val client = authClient()
            val original: TokenPair = client.register().body()
            // Rotate once: the original refresh token is now spent.
            client.post("/api/auth/refresh") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(RefreshRequest(original.refreshToken))
            }

            val reuse = client.post("/api/auth/refresh") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(RefreshRequest(original.refreshToken))
            }

            assertEquals(HttpStatusCode.Unauthorized, reuse.status)
        }
}
