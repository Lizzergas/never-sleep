package com.lizz.myapptemplate.auth.domain

/** Domain model of the signed-in user — the wire DTO never escapes data/. */
data class User(
    val id: String,
    val email: String,
)
