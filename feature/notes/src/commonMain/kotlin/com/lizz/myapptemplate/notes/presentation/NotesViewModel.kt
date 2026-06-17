package com.lizz.myapptemplate.notes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.notes.domain.AddNoteUseCase
import com.lizz.myapptemplate.notes.domain.NotesRepository
import com.lizz.myapptemplate.ui.UI_REFRESH_MINIMUM_VISIBLE_MILLIS
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off effects — the Screen clears the draft only after a successful add. */
sealed interface NotesEffect {
    data object NoteAdded : NotesEffect
}

class NotesViewModel(
    private val repository: NotesRepository,
    private val addNote: AddNoteUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    private val _effects = Channel<NotesEffect>(Channel.BUFFERED)
    val effects: Flow<NotesEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeNotes().collect { notes ->
                _state.update { it.copy(notes = notes) }
            }
        }
        refresh(RefreshOrigin.Initial)
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.Add ->
                launchReporting {
                    addNote(event.text).also { result ->
                        if (result is ApiResult.Success) _effects.send(NotesEffect.NoteAdded)
                    }
                }

            is NotesEvent.Delete -> launchReporting { repository.delete(event.id) }

            NotesEvent.Refresh -> refresh(RefreshOrigin.User)
        }
    }

    private fun refresh(origin: RefreshOrigin) {
        if (_state.value.isRefreshing) return
        _state.update { state ->
            state.copy(
                isRefreshing = true,
                error = if (origin == RefreshOrigin.Initial) null else state.error,
            )
        }
        viewModelScope.launch {
            val minimumRefreshWindow = async { delay(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong()) }
            val result = repository.refresh()
            minimumRefreshWindow.await()
            _state.update {
                it.copy(
                    isRefreshing = false,
                    error = (result as? ApiResult.Failure)?.error,
                )
            }
        }
    }

    private fun launchReporting(action: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            val result = action()
            if (result is ApiResult.Failure) {
                _state.update { it.copy(error = result.error) }
            }
        }
    }

    private enum class RefreshOrigin {
        Initial,
        User,
    }
}
