@file:OptIn(ExperimentalTime::class)

package com.lizz.neversleep.notes.data

import com.lizz.neversleep.database.NoteEntity
import com.lizz.neversleep.model.NoteDto
import com.lizz.neversleep.notes.domain.Note
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
