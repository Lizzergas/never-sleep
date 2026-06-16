package com.lizz.myapptemplate.network

import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

/** GET [path] and decode the body as [T], mapping every failure to [AppError]. */
suspend inline fun <reified T> HttpClient.safeGet(
    path: String,
    noinline builder: HttpRequestBuilder.() -> Unit = {},
): ApiResult<T> = safeApiCall { get(path, builder) }

/**
 * Runs [request] and decodes the response as [T]. The single place where
 * transport and HTTP failures are translated into the AppError taxonomy:
 *
 * - timeouts (request/connect/socket)          -> AppError.Timeout
 * - connectivity / IO                          -> AppError.Network
 * - 401 / 403                                  -> AppError.Unauthorized
 * - other 4xx                                  -> AppError.Validation(code)
 * - 5xx                                        -> AppError.Server(code)
 * - undecodable body                           -> AppError.Serialization
 * - anything else                              -> AppError.Unknown
 */
@Suppress("ReturnCount") // one early return per failure class is the point of this function
suspend inline fun <reified T> safeApiCall(crossinline request: suspend () -> HttpResponse): ApiResult<T> {
    val response = try {
        request()
    } catch (e: CancellationException) {
        throw e
    } catch (_: HttpRequestTimeoutException) {
        return ApiResult.Failure(AppError.Timeout)
    } catch (_: ConnectTimeoutException) {
        return ApiResult.Failure(AppError.Timeout)
    } catch (_: SocketTimeoutException) {
        return ApiResult.Failure(AppError.Timeout)
    } catch (_: IOException) {
        return ApiResult.Failure(AppError.Network)
    } catch (e: Exception) {
        return ApiResult.Failure(AppError.Unknown(e.message))
    }

    return when {
        response.status.isSuccess() ->
            try {
                ApiResult.Success(response.body<T>())
            } catch (e: ContentConvertException) {
                ApiResult.Failure(AppError.Serialization(e.message))
            } catch (e: SerializationException) {
                ApiResult.Failure(AppError.Serialization(e.message))
            }

        response.status == HttpStatusCode.Unauthorized ||
            response.status == HttpStatusCode.Forbidden ->
            ApiResult.Failure(AppError.Unauthorized)

        response.status.value in SERVER_ERROR_STATUS_CODES ->
            ApiResult.Failure(AppError.Server(response.status.value))

        else ->
            ApiResult.Failure(AppError.Validation(response.status.value))
    }
}

val SERVER_ERROR_STATUS_CODES = 500..599
