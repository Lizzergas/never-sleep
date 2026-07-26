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
                text = "Allow timeout changes",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = "Never Sleep needs “Modify system settings” access to change the " +
                    "screen-off timeout. Android will open Special app access, where you can " +
                    "allow it. When you switch back to Normal, the app restores your previous timeout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("Continue")
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
