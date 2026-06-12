package com.lizz.myapptemplate.notes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.notes.domain.AddNoteUseCase
import com.lizz.myapptemplate.notes.domain.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NotesRepository,
    private val addNote: AddNoteUseCase,
) : ViewModel() {
    private data class LocalState(
        val isRefreshing: Boolean = false,
        val error: AppError? = null,
    )

    private val local = MutableStateFlow(LocalState())

    init {
        onEvent(NotesEvent.Refresh)
    }

    val state: StateFlow<NotesUiState> =
        combine(repository.observeNotes(), local) { notes, l ->
            NotesUiState(
                notes = notes,
                isRefreshing = l.isRefreshing,
                error = l.error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState(),
        )

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.Add -> launchReporting { addNote(event.text) }
            is NotesEvent.Delete -> launchReporting { repository.delete(event.id) }
            NotesEvent.Refresh ->
                viewModelScope.launch {
                    local.update { it.copy(isRefreshing = true, error = null) }
                    val result = repository.refresh()
                    local.update {
                        it.copy(
                            isRefreshing = false,
                            error = (result as? ApiResult.Failure)?.error,
                        )
                    }
                }
        }
    }

    private fun launchReporting(action: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            local.update { it.copy(error = null) }
            val result = action()
            if (result is ApiResult.Failure) {
                local.update { it.copy(error = result.error) }
            }
        }
    }
}
