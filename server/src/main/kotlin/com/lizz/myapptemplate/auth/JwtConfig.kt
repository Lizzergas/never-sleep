package com.lizz.myapptemplate.auth

private const val DEFAULT_ACCESS_TTL_SECONDS = 15L * 60
private const val DEFAULT_REFRESH_TTL_SECONDS = 30L * 24 * 60 * 60

/**
 * JWT settings, read from the environment in production. The dev default
 * secret keeps the template runnable out of the box — set JWT_SECRET before
 * deploying anywhere real.
 */
data class JwtConfig(
    val secret: String,
    val issuer: String = "myapptemplate-server",
    val audience: String = "myapptemplate-client",
    val realm: String = "myapptemplate",
    val accessTtlSeconds: Long = DEFAULT_ACCESS_TTL_SECONDS,
    val refreshTtlSeconds: Long = DEFAULT_REFRESH_TTL_SECONDS,
) {
    companion object {
        fun fromEnvironment(): JwtConfig =
            JwtConfig(
                secret = System.getenv("JWT_SECRET") ?: "dev-secret-change-me",
            )
    }
}
