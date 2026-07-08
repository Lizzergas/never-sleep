package com.lizz.neversleep.notes.presentation

import com.lizz.neversleep.model.AppError
import com.lizz.neversleep.notes.domain.Note

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
)

sealed interface NotesEvent {
    data class Add(
        val text: String,
    ) : NotesEvent

    data class Delete(
        val id: Long,
    ) : NotesEvent

    data object Refresh : NotesEvent
}
