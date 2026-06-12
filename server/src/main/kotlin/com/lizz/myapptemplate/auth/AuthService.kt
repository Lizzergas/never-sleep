package com.lizz.myapptemplate.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.lizz.myapptemplate.model.TokenPair
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val BCRYPT_COST = 12
private const val MILLIS_PER_SECOND = 1000L

sealed interface AuthResult {
    data class Success(
        val tokens: TokenPair,
    ) : AuthResult

    data object InvalidCredentials : AuthResult

    data object EmailTaken : AuthResult
}

/**
 * Registration, login, and refresh-token rotation. Refresh tokens are opaque,
 * stored server-side, and single-use: each refresh invalidates the old token.
 */
class AuthService(
    private val users: UserRepository,
    private val config: JwtConfig,
) {
    private data class RefreshEntry(
        val userId: String,
        val expiresAtMillis: Long,
    )

    private val refreshTokens = ConcurrentHashMap<String, RefreshEntry>()
    private val algorithm = Algorithm.HMAC256(config.secret)

    fun register(
        email: String,
        password: String,
    ): AuthResult {
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
        val user = users.create(email, hash) ?: return AuthResult.EmailTaken
        return AuthResult.Success(issueTokens(user.id))
    }

    fun login(
        email: String,
        password: String,
    ): AuthResult {
        val user = users.findByEmail(email) ?: return AuthResult.InvalidCredentials
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        return if (verified) AuthResult.Success(issueTokens(user.id)) else AuthResult.InvalidCredentials
    }

    fun refresh(refreshToken: String): AuthResult {
        val entry = refreshTokens.remove(refreshToken) ?: return AuthResult.InvalidCredentials
        if (entry.expiresAtMillis < System.currentTimeMillis()) return AuthResult.InvalidCredentials
        return AuthResult.Success(issueTokens(entry.userId))
    }

    private fun issueTokens(userId: String): TokenPair {
        val now = System.currentTimeMillis()
        val accessToken =
            JWT
                .create()
                .withIssuer(config.issuer)
                .withAudience(config.audience)
                .withClaim(USER_ID_CLAIM, userId)
                .withExpiresAt(Date(now + config.accessTtlSeconds * MILLIS_PER_SECOND))
                .sign(algorithm)
        val refreshToken = UUID.randomUUID().toString()
        refreshTokens[refreshToken] =
            RefreshEntry(userId, now + config.refreshTtlSeconds * MILLIS_PER_SECOND)
        return TokenPair(accessToken = accessToken, refreshToken = refreshToken)
    }

    companion object {
        const val USER_ID_CLAIM = "userId"
    }
}
