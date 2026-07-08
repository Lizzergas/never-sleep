@file:OptIn(ExperimentalTime::class)

package com.lizz.neversleep.notes.domain

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Domain model — what screens and use cases work with. Never a DTO or entity.
 * (Pure Kotlin: compose annotations like @Immutable belong on UI models, not here.)
 */
data class Note(
    val id: Long,
    val text: String,
    val createdAt: Instant,
)
