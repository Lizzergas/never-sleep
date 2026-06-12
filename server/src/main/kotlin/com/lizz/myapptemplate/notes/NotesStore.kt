package com.lizz.myapptemplate.notes

import com.lizz.myapptemplate.model.NoteDto
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-user in-memory notes — the server side of the feature:notes sample.
 * Swap for real persistence the same way as UserRepository.
 */
class NotesStore {
    private val notesByUser = ConcurrentHashMap<String, MutableList<NoteDto>>()
    private val nextId = AtomicLong(1)

    fun list(userId: String): List<NoteDto> = notesByUser[userId]?.toList() ?: emptyList()

    fun add(
        userId: String,
        text: String,
    ): NoteDto {
        val note =
            NoteDto(
                id = nextId.getAndIncrement(),
                text = text,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
        notesByUser.computeIfAbsent(userId) { mutableListOf() }.add(0, note)
        return note
    }

    fun delete(
        userId: String,
        id: Long,
    ): Boolean = notesByUser[userId]?.removeAll { it.id == id } == true
}
