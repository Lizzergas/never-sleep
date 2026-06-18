@file:OptIn(ExperimentalTime::class)

package com.lizz.myapptemplate.notes.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.notes.domain.Note
import com.lizz.myapptemplate.ui.ErrorContent
import com.lizz.myapptemplate.ui.UI_STATUS_FADE_IN_MILLIS
import com.lizz.myapptemplate.ui.UI_STATUS_FADE_OUT_MILLIS
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Stateful wrapper: owns the ViewModel and collects one-off effects. */
@Composable
fun NotesScreen() {
    val viewModel = koinViewModel<NotesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }

    // The draft is cleared only when the add actually succeeded — a failed
    // add (offline, validation) must not destroy the user's typed text.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotesEffect.NoteAdded -> draft = ""
            }
        }
    }

    NotesContent(
        state = state,
        draft = draft,
        onDraftChange = { draft = it },
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesContent(
    state: NotesUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onEvent: (NotesEvent) -> Unit,
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = { Text("New note") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onEvent(NotesEvent.Add(draft)) }) { Text("Add") }
        }

        Column(
            modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spatialSpec),
        ) {
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(animationSpec = tween(UI_STATUS_FADE_IN_MILLIS)),
                exit = fadeOut(animationSpec = tween(UI_STATUS_FADE_OUT_MILLIS)),
            ) {
                state.error?.let { error ->
                    Column {
                        ErrorContent(
                            error = error,
                            onRetry = { onEvent(NotesEvent.Refresh) },
                            isRetrying = state.isRefreshing,
                        )
                        if (error == AppError.Unauthorized) {
                            Text(
                                "Sign in from Account first. Notes are saved per user.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            items(state.notes, key = { it.id }) { note ->
                NoteRow(note = note, onDelete = { onEvent(NotesEvent.Delete(note.id)) })
            }
        }

        if (state.error == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                OutlinedButton(
                    onClick = { onEvent(NotesEvent.Refresh) },
                    enabled = !state.isRefreshing,
                ) { Text(if (state.isRefreshing) "Refreshing..." else "Refresh") }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    note.createdAt
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Preview
@Composable
private fun NotesContentPreview() {
    AppTheme {
        NotesContent(
            draft = "",
            onDraftChange = {},
            state = NotesUiState(
                notes = listOf(
                    Note(
                        1,
                        "Ship the template",
                        Instant.fromEpochMilliseconds(1_750_000_000_000),
                    ),
                    Note(
                        2,
                        "Copy this feature for new screens",
                        Instant.fromEpochMilliseconds(1_750_000_500_000),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun NotesContentLoggedOutPreview() {
    AppTheme {
        NotesContent(
            draft = "",
            onDraftChange = {},
            state = NotesUiState(error = AppError.Unauthorized),
            onEvent = {},
        )
    }
}
