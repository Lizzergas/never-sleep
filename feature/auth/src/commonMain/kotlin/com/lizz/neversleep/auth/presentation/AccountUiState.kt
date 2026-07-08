package com.lizz.neversleep.auth.presentation

import com.lizz.neversleep.auth.domain.SessionState
import com.lizz.neversleep.auth.domain.ValidateCredentialsUseCase
import com.lizz.neversleep.model.AppError

data class AccountUiState(
    val session: SessionState = SessionState.Unknown,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val validation: ValidateCredentialsUseCase.Result = ValidateCredentialsUseCase.Result(),
)

sealed interface AccountEvent {
    data class Login(
        val email: String,
        val password: String,
    ) : AccountEvent

    data class Register(
        val email: String,
        val password: String,
    ) : AccountEvent

    data object Logout : AccountEvent
}
