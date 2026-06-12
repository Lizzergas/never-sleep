package com.lizz.myapptemplate.notes.domain

import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError

private const val MAX_NOTE_LENGTH = 500

/**
 * Validates and normalizes note text before it reaches the network — the
 * single place the "what is a valid note" rule lives.
 */
class AddNoteUseCase(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(rawText: String): ApiResult<Note> {
        val text = rawText.trim()
        return when {
            text.isEmpty() ->
                ApiResult.Failure(AppError.Unknown("Note text must not be empty"))

            text.length > MAX_NOTE_LENGTH ->
                ApiResult.Failure(AppError.Unknown("Notes are limited to $MAX_NOTE_LENGTH characters"))

            else -> repository.add(text)
        }
    }
}
