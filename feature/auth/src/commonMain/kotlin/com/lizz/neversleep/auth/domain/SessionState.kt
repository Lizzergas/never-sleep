package com.lizz.neversleep.auth.domain

sealed interface SessionState {
    /** Not yet restored from storage. */
    data object Unknown : SessionState

    data object LoggedOut : SessionState

    data class LoggedIn(
        val user: User,
    ) : SessionState
}
