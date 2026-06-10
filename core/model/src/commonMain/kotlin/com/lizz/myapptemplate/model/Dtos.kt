package com.lizz.myapptemplate.model

import kotlinx.serialization.Serializable

/**
 * Sample DTOs shared between the server and all clients — the single source
 * of truth for the wire format. Replace with your real API types.
 */
@Serializable
data class HelloResponse(
    val message: String,
    val serverTime: String,
)

@Serializable
data class Item(
    val id: Int,
    val title: String,
    val description: String,
)
