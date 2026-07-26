package com.lizz.neversleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The Never Sleep home screen: morphing background, centred title + toggle, and an
 * optional ad. Stateless apart from the settings-dialog
 * visibility — all app state is passed in, so it renders any (isEnabled, showAds) pair.
 */
@Composable
internal fun NeverSleepScreen(
    onToggle: () -> Unit,
    isEnabled: Boolean,
    showAds: Boolean,
    showAdBanner: Boolean,
    canWriteSettings: Boolean,
    privacyOptionsRequired: Boolean,
    screenshotMode: Boolean,
    onSetShowAds: (Boolean) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenPrivacyOptions: () -> Unit,
    onOpenWriteSettings: () -> Unit,
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        // Let the shader draw full-bleed under the status bar; insets are applied to content below
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen background that morphs Normal↔Never with a windy swoosh.
            NeverSleepBackground(isEnabled = isEnabled, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    AdaptivePrimaryContent(
                        isEnabled = isEnabled,
                        onToggle = onToggle,
                    )

                    if (!screenshotMode) {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(NeverSleepSpacing.Sm)
                                .sizeIn(
                                    minWidth = NeverSleepDimensions.TouchTarget,
                                    minHeight = NeverSleepDimensions.TouchTarget,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NeverSleepColors.ContentMuted,
                            )
                        }
                    }
                }

                if (!screenshotMode && showAdBanner) {
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NeverSleepDimensions.AdHeight),
                    )
                }
            }
        }
    }

    if (showSettingsDialog && !screenshotMode) {
        SettingsDialog(
            showAds = showAds,
            canWriteSettings = canWriteSettings,
            privacyOptionsRequired = privacyOptionsRequired,
            onSetShowAds = onSetShowAds,
            onOpenPrivacyPolicy = {
                showSettingsDialog = false
                onOpenPrivacyPolicy()
            },
            onOpenPrivacyOptions = {
                showSettingsDialog = false
                onOpenPrivacyOptions()
            },
            onOpenWriteSettings = {
                showSettingsDialog = false
                onOpenWriteSettings()
            },
            onDismiss = { showSettingsDialog = false },
        )
    }
}

@Composable
private fun AdaptivePrimaryContent(
    isEnabled: Boolean,
    onToggle: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val maximum = if (isLandscape) {
            NeverSleepDimensions.ToggleLandscapeMax
        } else {
            NeverSleepDimensions.ToggleMax
        }
        val widthBound = maxWidth * if (isLandscape) 0.34f else 0.64f
        val heightBound = maxHeight * if (isLandscape) 0.66f else 0.52f
        val controlSize = minOf(maximum, widthBound, heightBound)
            .coerceAtLeast(NeverSleepDimensions.ToggleMinimum)

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = NeverSleepSpacing.Xl,
                        vertical = NeverSleepSpacing.Sm,
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLabel()
                NeverSleepToggleButton(
                    isEnabled = isEnabled,
                    onToggle = onToggle,
                    controlSize = controlSize,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NeverSleepSpacing.Lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandLabel()
                Spacer(Modifier.height(NeverSleepSpacing.Xl))
                NeverSleepToggleButton(
                    isEnabled = isEnabled,
                    onToggle = onToggle,
                    controlSize = controlSize,
                )
            }
        }
    }
}

@Composable
private fun BrandLabel() {
    Text(
        text = "never sleep",
        style = MaterialTheme.typography.titleLarge,
        color = NeverSleepColors.ContentMuted,
        maxLines = 1,
    )
}
