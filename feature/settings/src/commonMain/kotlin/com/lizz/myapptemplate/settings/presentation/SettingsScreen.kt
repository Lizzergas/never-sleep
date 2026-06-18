package com.lizz.myapptemplate.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.designsystem.Theme
import com.lizz.myapptemplate.designsystem.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

/** Stateful wrapper: owns the ViewModel. All rendering is in [SettingsContent]. */
@Composable
fun SettingsScreen() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = state.themeMode == mode,
                        onClick = { onEvent(SettingsEvent.SetThemeMode(mode)) },
                    ).padding(Theme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = state.themeMode == mode,
                    onClick = { onEvent(SettingsEvent.SetThemeMode(mode)) },
                )
                Text(mode.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    AppTheme {
        SettingsContent(
            state = SettingsUiState(themeMode = ThemeMode.Dark),
            onEvent = {},
        )
    }
}
