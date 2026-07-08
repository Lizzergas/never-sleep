package com.lizz.neversleep.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
)
