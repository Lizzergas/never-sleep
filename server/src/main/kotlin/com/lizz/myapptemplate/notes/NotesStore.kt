package com.lizz.myapptemplate.notes

import com.lizz.myapptemplate.model.NoteDto
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-user in-memory notes — the server side of the feature:notes sample.
 * Values are immutable lists swapped atomically via compute(): a concurrent
 * map alone would NOT make mutable lists inside it thread-safe.
 * Swap for real persistence the same way as UserRepository.
 */
class NotesStore {
    private val notesByUser = ConcurrentHashMap<String, List<NoteDto>>()
    private val nextId = AtomicLong(1)

    fun list(userId: String): List<NoteDto> = notesByUser[userId] ?: emptyList()

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
        notesByUser.compute(userId) { _, existing ->
            listOf(note) + (existing ?: emptyList())
        }
        return note
    }

    fun delete(
        userId: String,
        id: Long,
    ): Boolean {
        var removed = false
        notesByUser.computeIfPresent(userId) { _, existing ->
            val remaining = existing.filterNot { it.id == id }
            removed = remaining.size != existing.size
            remaining
        }
        return removed
    }
}
