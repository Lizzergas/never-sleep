package com.lizz.myapptemplate.model

/**
 * The app-wide failure taxonomy. Produced by core:network (and any other data
 * source), consumed by UI state mapping. Add variants here when a new failure
 * class genuinely needs different handling.
 */
sealed interface AppError {
    /** No connectivity / connection refused / DNS / IO failure. */
    data object Network : AppError

    /** Request, connect, or socket timeout. */
    data object Timeout : AppError

    /** 401 or 403 — the session is missing, expired, or insufficient. */
    data object Unauthorized : AppError

    /** Any other 4xx — the request itself is wrong. */
    data class Validation(val code: Int) : AppError

    /** 5xx — the server failed. */
    data class Server(val code: Int) : AppError

    /** The body could not be decoded into the expected type. */
    data class Serialization(val message: String? = null) : AppError

    /** Anything unclassified. */
    data class Unknown(val message: String? = null) : AppError
}
