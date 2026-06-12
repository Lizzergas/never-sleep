package com.lizz.myapptemplate.auth.presentation

import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.auth.domain.ValidateCredentialsUseCase
import com.lizz.myapptemplate.model.AppError

data class AccountUiState(
    val session: SessionState = SessionState.Unknown,
    val inFlight: Boolean = false,
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
