package com.lizz.myapptemplate.auth.domain

import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.UserDto
import kotlinx.coroutines.flow.StateFlow

/** The session as the rest of the app sees it — implemented in data/. */
interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    /** Restores the session from stored tokens; call once at first use. */
    suspend fun restore()

    suspend fun login(
        email: String,
        password: String,
    ): ApiResult<UserDto>

    suspend fun register(
        email: String,
        password: String,
    ): ApiResult<UserDto>

    suspend fun logout()
}
