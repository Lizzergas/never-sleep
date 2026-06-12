package com.lizz.myapptemplate.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.ValidateCredentialsUseCase
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: SessionRepository,
    private val validateCredentials: ValidateCredentialsUseCase,
) : ViewModel() {
    private data class LocalState(
        val inFlight: Boolean = false,
        val error: AppError? = null,
        val validation: ValidateCredentialsUseCase.Result = ValidateCredentialsUseCase.Result(),
    )

    private val local = MutableStateFlow(LocalState())

    init {
        viewModelScope.launch { repository.restore() }
    }

    val state: StateFlow<AccountUiState> =
        combine(repository.sessionState, local) { session, l ->
            AccountUiState(
                session = session,
                inFlight = l.inFlight,
                error = l.error,
                validation = l.validation,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountUiState(),
        )

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.Login -> submit(event.email, event.password, repository::login)
            is AccountEvent.Register -> submit(event.email, event.password, repository::register)
            AccountEvent.Logout -> viewModelScope.launch { repository.logout() }
        }
    }

    private fun submit(
        email: String,
        password: String,
        action: suspend (String, String) -> ApiResult<UserDto>,
    ) {
        val validation = validateCredentials(email, password)
        if (!validation.isValid) {
            local.update { it.copy(validation = validation, error = null) }
            return
        }
        viewModelScope.launch {
            local.update { it.copy(inFlight = true, error = null, validation = validation) }
            val result = action(email, password)
            local.update {
                it.copy(
                    inFlight = false,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
