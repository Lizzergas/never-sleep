package com.lizz.neversleep

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Explains why Android's special "Modify system settings" screen is about to open.
 * The platform screen remains the only place where this permission can be granted.
 */
@Composable
internal fun WriteSettingsPermissionDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Allow screen timeout control",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = "Never Sleep reads and changes your screen timeout so it can keep the " +
                    "screen awake and restore your previous setting. The timeout value stays " +
                    "on this device.\n\nAndroid will open Special app access, where you can " +
                    "allow or revoke access. Keeping the screen awake can use more battery.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("Open settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
