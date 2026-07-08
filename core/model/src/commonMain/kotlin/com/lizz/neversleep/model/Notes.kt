package com.lizz.neversleep.model

import kotlinx.serialization.Serializable

/** Wire format for notes — shared by server and clients. */
@Serializable
data class NoteDto(
    val id: Long,
    val text: String,
    val createdAtEpochMillis: Long,
)

@Serializable
data class CreateNoteRequest(
    val text: String,
)
