package com.lizz.myapptemplate.ui

import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStateTest {
    @Test
    fun successMapsToSuccess() {
        val result: ApiResult<List<Int>> = ApiResult.Success(listOf(1, 2))

        assertEquals(UiState.Success(listOf(1, 2)), result.toUiState())
    }

    @Test
    fun successMapsToEmptyWhenPredicateMatches() {
        val result: ApiResult<List<Int>> = ApiResult.Success(emptyList())

        assertEquals(UiState.Empty, result.toUiState { it.isEmpty() })
    }

    @Test
    fun failureMapsToErrorCarryingTheAppError() {
        val result: ApiResult<List<Int>> = ApiResult.Failure(AppError.Timeout)

        assertEquals(UiState.Error(AppError.Timeout), result.toUiState())
    }

    @Test
    fun everyAppErrorHasAUserMessage() {
        val errors =
            listOf(
                AppError.Network,
                AppError.Timeout,
                AppError.Unauthorized,
                AppError.Server(500),
                AppError.Validation(404),
                AppError.Serialization("x"),
                AppError.Unknown(null),
            )
        errors.forEach { error ->
            kotlin.test.assertTrue(error.userMessage().isNotBlank(), "no message for $error")
        }
    }
}
