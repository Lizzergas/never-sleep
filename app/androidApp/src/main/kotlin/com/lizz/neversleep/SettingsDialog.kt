package com.lizz.neversleep

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Product settings and the durable places users can revisit privacy or special access.
 *
 * Permission rationale remains contextual to the primary toggle; this screen is for review and
 * control after the fact.
 */
@Composable
internal fun SettingsDialog(
    showAds: Boolean,
    canWriteSettings: Boolean,
    privacyOptionsRequired: Boolean,
    onSetShowAds: (Boolean) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenPrivacyOptions: () -> Unit,
    onOpenWriteSettings: () -> Unit,
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                SettingsSectionLabel("Ads")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = NeverSleepDimensions.TouchTarget)
                        .toggleable(
                            value = showAds,
                            role = Role.Switch,
                            onValueChange = onSetShowAds,
                        ).padding(vertical = NeverSleepSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = NeverSleepSpacing.Md),
                    ) {
                        Text(
                            text = "Show ads",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(NeverSleepSpacing.Xs / 2))
                        Text(
                            text = "Allow a small banner at the bottom. Turn this off at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = showAds,
                        onCheckedChange = null,
                    )
                }

                SettingsDivider()
                SettingsSectionLabel("Privacy")
                if (privacyOptionsRequired) {
                    SettingsActionRow(
                        title = "Privacy choices",
                        supportingText = "Review how Google may use data for advertising.",
                        onClick = onOpenPrivacyOptions,
                    )
                }
                SettingsActionRow(
                    title = "Privacy policy",
                    supportingText = "See what the app and its ad provider process.",
                    onClick = onOpenPrivacyPolicy,
                )

                SettingsDivider()
                SettingsSectionLabel("Access")
                SettingsActionRow(
                    title = "Screen timeout access",
                    supportingText = if (canWriteSettings) {
                        "Allowed — review or revoke in Android Settings."
                    } else {
                        "Not allowed — required when you turn Never Sleep on."
                    },
                    onClick = onOpenWriteSettings,
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

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = NeverSleepSpacing.Xs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    supportingText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = NeverSleepDimensions.TouchTarget)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ).padding(vertical = NeverSleepSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = NeverSleepSpacing.Md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(NeverSleepSpacing.Xs / 2))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = NeverSleepSpacing.Md),
        color = MaterialTheme.colorScheme.outline,
    )
}
