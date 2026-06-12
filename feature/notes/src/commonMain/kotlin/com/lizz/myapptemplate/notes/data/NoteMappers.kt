@file:OptIn(ExperimentalTime::class)

package com.lizz.myapptemplate.notes.data

import com.lizz.myapptemplate.database.NoteEntity
import com.lizz.myapptemplate.model.NoteDto
import com.lizz.myapptemplate.notes.domain.Note
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// The full mapping chain: NoteDto (wire) <-> NoteEntity (Room) <-> Note (domain).
// DTOs and entities never escape the data layer.

fun NoteDto.toEntity() =
    NoteEntity(
        id = id,
        text = text,
        createdAtEpochMillis = createdAtEpochMillis,
    )

fun NoteEntity.toDomain() =
    Note(
        id = id,
        text = text,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
    )

fun NoteDto.toDomain() = toEntity().toDomain()
