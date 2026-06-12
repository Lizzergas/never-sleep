package com.lizz.myapptemplate.ui

import com.lizz.myapptemplate.model.AppError

/** User-facing message per error class. Localize/replace per app. */
fun AppError.userMessage(): String =
    when (this) {
        AppError.Network -> "Can't reach the server. Check your connection."
        AppError.Timeout -> "The request timed out. Try again."
        AppError.Unauthorized -> "You're not signed in."
        is AppError.Server -> "Server error ($code). Try again later."
        is AppError.Validation -> "Request failed ($code)."
        is AppError.Serialization -> "Received an unexpected response."
        is AppError.Unknown -> message ?: "Something went wrong."
    }
