package com.lizz.myapptemplate.notes.presentation

import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.notes.domain.Note

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
