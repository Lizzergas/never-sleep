package com.lizz.neversleep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Never Sleep home screen: morphing background, centred title + toggle, and the
 * bottom chrome (disclaimer + optional ad). Stateless apart from the settings-dialog
 * visibility — all app state is passed in, so it renders any (isEnabled, showAds) pair.
 */
@Composable
internal fun NeverSleepScreen(
    onToggle: () -> Unit,
    isEnabled: Boolean,
    showAds: Boolean,
    screenshotMode: Boolean,
    onSetShowAds: (Boolean) -> Unit,
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    val bottomChromePadding = when {
        screenshotMode -> 0.dp
        showAds -> 230.dp
        else -> 180.dp
    }

    Scaffold(
        containerColor = Color.Transparent,
        // Let the shader draw full-bleed under the status bar; insets are applied to content below
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen background that morphs Normal↔Never with a windy swoosh.
            NeverSleepBackground(isEnabled = isEnabled, modifier = Modifier.fillMaxSize())

            if (!screenshotMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = innerPadding.calculateTopPadding())
                        .padding(12.dp),
                ) {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }
            }

            // Main content: title + button always centered.
            // Apply top inset so it doesn't sit under the status bar.
            // Keep bottom reserve for the disclaimer + optional ad (these live in the bottom column).
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = bottomChromePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "never sleep",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 4.sp,
                )

                Spacer(Modifier.height(48.dp))

                NeverSleepToggleButton(
                    isEnabled = isEnabled,
                    onToggle = onToggle,
                )
            }

            if (!screenshotMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .fillMaxWidth(),
                ) {
                    DisclaimerSection(
                        modifier = Modifier.fillMaxWidth(),
                        privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
                    )
                    if (showAds) {
                        AdBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog && !screenshotMode) {
        SettingsDialog(
            showAds = showAds,
            onSetShowAds = onSetShowAds,
            onDismiss = { showSettingsDialog = false },
        )
    }
}
