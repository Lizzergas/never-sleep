package com.lizz.myapptemplate.auth.domain

/**
 * Client-side credential validation before any network round-trip.
 * A genuine UseCase: pure multi-field logic, no I/O, reused by login and
 * register. (Mirrors the server's rules in AuthRoutes.)
 */
class ValidateCredentialsUseCase {
    data class Result(
        val emailError: String? = null,
        val passwordError: String? = null,
    ) {
        val isValid: Boolean get() = emailError == null && passwordError == null
    }

    operator fun invoke(
        email: String,
        password: String,
    ): Result =
        Result(
            emailError =
                when {
                    email.isBlank() -> "Email is required"
                    !email.contains("@") || email.startsWith("@") || email.endsWith("@") ->
                        "Enter a valid email address"
                    else -> null
                },
            passwordError =
                when {
                    password.isBlank() -> "Password is required"
                    password.length < MIN_PASSWORD_LENGTH ->
                        "Password must be at least $MIN_PASSWORD_LENGTH characters"
                    else -> null
                },
        )

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
