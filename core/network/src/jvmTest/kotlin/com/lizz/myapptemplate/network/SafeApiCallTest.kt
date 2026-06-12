package com.lizz.myapptemplate.network

import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.Item
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SafeApiCallTest {
    private fun clientReturning(
        status: HttpStatusCode,
        body: String = """[]""",
    ): HttpClient =
        HttpClient(
            MockEngine { _ ->
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(DefaultJson) }
        }

    private fun clientThrowing(cause: Throwable): HttpClient =
        HttpClient(
            MockEngine { throw cause },
        ) {
            install(ContentNegotiation) { json(DefaultJson) }
        }

    @Test
    fun successDecodesTypedBody() =
        runTest {
            val client =
                clientReturning(
                    HttpStatusCode.OK,
                    """[{"id":1,"title":"a","description":"b"}]""",
                )

            val result = client.safeGet<List<Item>>("/items")

            assertIs<ApiResult.Success<List<Item>>>(result)
            assertEquals(1, result.data.single().id)
        }

    @Test
    fun unauthorizedAndForbiddenMapToUnauthorized() =
        runTest {
            for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)) {
                val result = clientReturning(status).safeGet<List<Item>>("/items")
                val failure = assertIs<ApiResult.Failure>(result)
                assertEquals(AppError.Unauthorized, failure.error)
            }
        }

    @Test
    fun clientErrorsMapToValidationWithCode() =
        runTest {
            for (status in listOf(
                HttpStatusCode.BadRequest,
                HttpStatusCode.NotFound,
                HttpStatusCode.UnprocessableEntity,
            )) {
                val result = clientReturning(status).safeGet<List<Item>>("/items")
                val failure = assertIs<ApiResult.Failure>(result)
                assertEquals(AppError.Validation(status.value), failure.error)
            }
        }

    @Test
    fun serverErrorsMapToServerWithCode() =
        runTest {
            for (status in listOf(HttpStatusCode.InternalServerError, HttpStatusCode.ServiceUnavailable)) {
                val result = clientReturning(status).safeGet<List<Item>>("/items")
                val failure = assertIs<ApiResult.Failure>(result)
                assertEquals(AppError.Server(status.value), failure.error)
            }
        }

    @Test
    fun malformedBodyMapsToSerialization() =
        runTest {
            val result =
                clientReturning(HttpStatusCode.OK, """{"not":"a list"}""")
                    .safeGet<List<Item>>("/items")

            val failure = assertIs<ApiResult.Failure>(result)
            assertIs<AppError.Serialization>(failure.error)
        }

    @Test
    fun ioFailureMapsToNetwork() =
        runTest {
            val result =
                clientThrowing(IOException("connection refused"))
                    .safeGet<List<Item>>("/items")

            val failure = assertIs<ApiResult.Failure>(result)
            assertEquals(AppError.Network, failure.error)
        }

    @Test
    fun requestTimeoutMapsToTimeout() =
        runTest {
            val result =
                clientThrowing(HttpRequestTimeoutException("/items", 1))
                    .safeGet<List<Item>>("/items")

            val failure = assertIs<ApiResult.Failure>(result)
            assertEquals(AppError.Timeout, failure.error)
        }

    @Test
    fun factoryAttachesBearerTokenFromAuthTokenProvider() =
        runTest {
            var seenAuth: String? = null
            val engine =
                MockEngine { request ->
                    seenAuth = request.headers[HttpHeaders.Authorization]
                    respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }

            val provider =
                object : AuthTokenProvider {
                    override suspend fun loadTokens() = BearerTokens("token-123", "refresh-123")

                    override suspend fun refreshTokens(oldRefreshToken: String?) = null
                }
            val client = createHttpClient(NetworkConfig(), engine, provider)
            client.safeGet<List<Item>>("/items")

            assertEquals("Bearer token-123", seenAuth)
        }
}
