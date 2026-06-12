package com.lizz.myapptemplate.auth.domain

import com.lizz.myapptemplate.model.UserDto

sealed interface SessionState {
    /** Not yet restored from storage. */
    data object Unknown : SessionState

    data object LoggedOut : SessionState

    data class LoggedIn(
        val user: UserDto,
    ) : SessionState
}
