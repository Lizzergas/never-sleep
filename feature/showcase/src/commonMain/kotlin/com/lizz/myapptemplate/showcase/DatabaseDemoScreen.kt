package com.lizz.myapptemplate.showcase

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lizz.myapptemplate.database.Note
import com.lizz.myapptemplate.database.NoteDao
import com.lizz.myapptemplate.designsystem.Theme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Exercises the Room 3 KMP setup: insert + observe through the shared
 * core:database module on every platform.
 */
@Composable
fun DatabaseDemoScreen(onBack: () -> Unit) {
    val noteDao = koinInject<NoteDao>()
    val scope = rememberCoroutineScope()
    val notes by noteDao.observeAll().collectAsState(initial = emptyList())
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text("Database demo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Notes are stored in a Room 3 database (bundled SQLite driver) " +
                "shared across all platforms.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(0.7f),
            )
            Button(
                onClick = {
                    val value = text.trim()
                    if (value.isNotEmpty()) {
                        scope.launch {
                            noteDao.insert(Note(text = value))
                            text = ""
                        }
                    }
                },
            ) {
                Text("Add note")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            items(notes, key = { it.id }) { note ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        note.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(Theme.spacing.sm),
                    )
                }
            }
        }

        Button(onClick = onBack) { Text("Back") }
    }
}
