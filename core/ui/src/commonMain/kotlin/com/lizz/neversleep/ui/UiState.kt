package com.lizz.neversleep.ui

import com.lizz.neversleep.model.ApiResult
import com.lizz.neversleep.model.AppError

/** Standard screen-state shape used by every feature. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T,
    ) : UiState<T>

    data class Error(
        val error: AppError,
    ) : UiState<Nothing>

    data object Empty : UiState<Nothing>
}

/**
 * Maps a typed network result into screen state. Pass [emptyWhen] to surface
 * the Empty state (e.g. for blank lists).
 */
fun <T> ApiResult<T>.toUiState(emptyWhen: (T) -> Boolean = { false }): UiState<T> =
    when (this) {
        is ApiResult.Success -> if (emptyWhen(data)) UiState.Empty else UiState.Success(data)
        is ApiResult.Failure -> UiState.Error(error)
    }
