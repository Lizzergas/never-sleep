@file:OptIn(ExperimentalTime::class)

package com.lizz.myapptemplate.notes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.notes.domain.Note
import com.lizz.myapptemplate.ui.ErrorContent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Stateful wrapper: owns the ViewModel. All rendering is in [NotesContent]. */
@Composable
fun NotesScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<NotesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    NotesContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

@Composable
fun NotesContent(
    state: NotesUiState,
    onEvent: (NotesEvent) -> Unit,
    onBack: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text("Notes", style = MaterialTheme.typography.headlineMedium)
        Text(
            "The reference feature: server CRUD, Room cache (offline reads), " +
                "mapper chain, UseCase, UiState/Event. Requires being logged in.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("New note") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                onEvent(NotesEvent.Add(draft))
                draft = ""
            }) { Text("Add") }
        }

        state.error?.let { error ->
            ErrorContent(error, onRetry = { onEvent(NotesEvent.Refresh) })
            if (error == AppError.Unauthorized) {
                Text(
                    "Log in via the Account feature first — notes are per user.",
                    style = MaterialTheme.typography.bodySmall,
                )
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

        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            OutlinedButton(
                onClick = { onEvent(NotesEvent.Refresh) },
                enabled = !state.isRefreshing,
            ) { Text(if (state.isRefreshing) "Refreshing…" else "Refresh") }
            Button(onClick = onBack) { Text("Back") }
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
            state =
                NotesUiState(
                    notes =
                        listOf(
                            Note(1, "Ship the template", Instant.fromEpochMilliseconds(1_750_000_000_000)),
                            Note(
                                2,
                                "Copy this feature for new screens",
                                Instant.fromEpochMilliseconds(1_750_000_500_000),
                            ),
                        ),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun NotesContentLoggedOutPreview() {
    AppTheme {
        NotesContent(
            state = NotesUiState(error = AppError.Unauthorized),
            onEvent = {},
            onBack = {},
        )
    }
}
