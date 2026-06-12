package com.lizz.myapptemplate.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    init {
        viewModelScope.launch { repository.restore() }
    }

    val sessionState: StateFlow<SessionState> =
        repository.sessionState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.Unknown,
        )

    private val _inFlight = MutableStateFlow(false)
    val inFlight: StateFlow<Boolean> = _inFlight.asStateFlow()

    private val _lastError = MutableStateFlow<AppError?>(null)
    val lastError: StateFlow<AppError?> = _lastError.asStateFlow()

    fun login(
        email: String,
        password: String,
    ) = submit { repository.login(email, password) }

    fun register(
        email: String,
        password: String,
    ) = submit { repository.register(email, password) }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    private fun submit(action: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _inFlight.value = true
            _lastError.value = null
            when (val result = action()) {
                is ApiResult.Success -> Unit
                is ApiResult.Failure -> _lastError.value = result.error
            }
            _inFlight.value = false
        }
    }
}
