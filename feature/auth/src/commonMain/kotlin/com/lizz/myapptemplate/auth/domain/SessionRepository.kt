package com.lizz.myapptemplate.auth.domain

import com.lizz.myapptemplate.model.ApiResult
import kotlinx.coroutines.flow.StateFlow

/** The session as the rest of the app sees it — implemented in data/. */
interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    /**
     * Restores the session from stored tokens. Safe to call repeatedly:
     * transient (offline) failures leave the session retryable on the next
     * call instead of terminally logged out.
     */
    suspend fun restore()

    suspend fun login(
        email: String,
        password: String,
    ): ApiResult<User>

    suspend fun register(
        email: String,
        password: String,
    ): ApiResult<User>

    suspend fun logout()
}
