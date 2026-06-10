package com.lizz.myapptemplate.model

/**
 * Typed outcome of an API call. The network layer never throws (except
 * CancellationException) — every failure is mapped to an [AppError].
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(action: (AppError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}
