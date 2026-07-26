package com.lizz.neversleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** Settings dialog — currently just the ads toggle, room for more later. */
@Composable
internal fun SettingsDialog(
    showAds: Boolean,
    onSetShowAds: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = !showAds,
                            role = Role.Switch,
                            onValueChange = { disabled -> onSetShowAds(!disabled) },
                        ).padding(vertical = NeverSleepSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Disable ads",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = !showAds,
                        onCheckedChange = null,
                    )
                }
                Spacer(Modifier.height(NeverSleepSpacing.Xs))
                Text(
                    text = "Hides the small ad box at the bottom. The rest of the app works the same.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
