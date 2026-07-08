package com.lizz.neversleep.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.neversleep.auth.domain.SessionRepository
import com.lizz.neversleep.auth.domain.User
import com.lizz.neversleep.auth.domain.ValidateCredentialsUseCase
import com.lizz.neversleep.model.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: SessionRepository,
    private val validateCredentials: ValidateCredentialsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessionState.collect { session ->
                _state.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch { repository.restore() }
    }

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
        action: suspend (String, String) -> ApiResult<User>,
    ) {
        val validation = validateCredentials(email, password)
        if (!validation.isValid) {
            _state.update { it.copy(validation = validation, error = null) }
            return
        }
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, validation = validation) }
            val result = action(email, password)
            _state.update {
                it.copy(
                    isSubmitting = false,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }
}
