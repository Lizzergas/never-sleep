package com.lizz.neversleep

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment as ComposeAlignment

private const val PREFS_NAME = "never_sleep_prefs"
private const val KEY_PREVIOUS_TIMEOUT = "previous_timeout"
private const val KEY_ENABLED = "never_sleep_enabled"
private const val DEFAULT_NORMAL_TIMEOUT_MS = 30_000

// Use a high but practical value instead of Int.MAX_VALUE (some devices clamp very large values)
private const val NEVER_SLEEP_TIMEOUT_MS = 1_000_000_000 // ~11.5 days, "never" for inactivity

// Threshold for considering the timeout "never sleep" mode (even if system clamps)
private const val NEVER_SLEEP_THRESHOLD_MS = 3_600_000 // 1 hour

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SCREENSHOT_MODE = "screenshot_mode"
        const val EXTRA_CAPTURE_NEVER = "capture_never"
    }

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Use class-level state so we can refresh it from onResume() when app is minimized/resumed
    private val isEnabledState = mutableStateOf(false)

    // Pending desired state if we had to prompt for permission
    private var pendingDesired: Boolean? = null

    // Ads visibility (persisted; default true so the stub ad shows unless user disables)
    private val showAdsState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)

        if (intent.getBooleanExtra(EXTRA_SCREENSHOT_MODE, false) &&
            Settings.System.canWrite(this)
        ) {
            val captureNever = intent.getBooleanExtra(EXTRA_CAPTURE_NEVER, false)
            if (captureNever) {
                NeverSleepController.setNeverSleep(this)
                isEnabledState.value = true
            } else {
                NeverSleepController.setNormal(this)
                isEnabledState.value = false
            }
        }

        setContent {
            // Dark modern theme for a cool shader experience
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C4DFF),
                    onPrimary = Color.White,
                    background = Color(0xFF0D0D1A),
                    surface = Color(0xFF1A1A2E),
                ),
            ) {
                val isEnabled by remember { isEnabledState }
                val showAds by remember { showAdsState }

                // Simple toggle callback that always reads the latest state at tap time.
                // This ensures reliable flip between never <-> normal even after resume/minimize or state changes.
                val onToggle: () -> Unit = {
                    val current = isEnabledState.value
                    val desired = !current
                    if (Settings.System.canWrite(this)) {
                        // Attempt the change. We flip the UI immediately on click (when we have/can obtain the perm)
                        // to make the toggle feel direct and reliable, as requested. The actual system value
                        // may be clamped by the device but the button state follows the user's intent.
                        val attempted = if (desired) {
                            NeverSleepController.setNeverSleep(this)
                        } else {
                            NeverSleepController.setNormal(this)
                        }
                        isEnabledState.value = desired
                    } else {
                        // No perm yet: remember intent, prompt, auto-apply on resume (after user grants)
                        pendingDesired = desired
                        openWriteSettingsPermissionScreen()
                    }
                }

                val screenshotMode = intent.getBooleanExtra(EXTRA_SCREENSHOT_MODE, false)

                NeverSleepScreen(
                    isEnabled = isEnabled,
                    onToggle = onToggle,
                    showAds = showAds && !screenshotMode,
                    screenshotMode = screenshotMode,
                    onSetShowAds = { enabled ->
                        showAdsState.value = enabled
                        NeverSleepController.setAdsEnabled(this, enabled)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh state when app comes back from background/minimized - this fixes stale toggle state
        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)

        // If we had a pending toggle because we prompted for permission, apply it now if we have write access
        if (pendingDesired != null && Settings.System.canWrite(this)) {
            val desired = pendingDesired!!
            pendingDesired = null
            val success = if (desired) {
                NeverSleepController.setNeverSleep(this)
            } else {
                NeverSleepController.setNormal(this)
            }
            // Flip UI to desired on resume after grant, for direct toggle experience.
            isEnabledState.value = desired
        }
    }

    private fun openWriteSettingsPermissionScreen() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }
}

@Composable
private fun NeverSleepScreen(
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
                        .align(ComposeAlignment.TopEnd)
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
                    .align(ComposeAlignment.Center)
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
                        .align(ComposeAlignment.BottomCenter)
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
        } // close Box
    } // close Scaffold

    // Settings dialog for ads toggle (and future settings)
    if (showSettingsDialog && !screenshotMode) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text("Settings", color = Color.White)
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSetShowAds(!showAds) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Disable ads",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                        Switch(
                            checked = !showAds,
                            onCheckedChange = { disabled -> onSetShowAds(!disabled) },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Hides the small ad box at the bottom. The rest of the app works the same.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            },
        )
    }
}

/**
 * Expandable disclaimer as a self-contained scrollable item.
 * Collapsed: just header (small fixed height)
 * Expanded: animates to known height (180.dp), content scrolls internally.
 * This way the main screen (button position) does not shift/"expand".
 */
@Composable
private fun DisclaimerSection(
    modifier: Modifier = Modifier,
    privacyPolicyUrl: String,
) {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }

    val collapsedHeight = 52.dp
    val expandedHeight = 180.dp // predetermined size - enough to read the text with scrolling

    val targetHeight by animateDpAsState(
        targetValue = if (expanded) expandedHeight else collapsedHeight,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "disclaimerHeight",
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrowRotation",
    )

    Column(
        modifier = modifier
            .height(targetHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                expanded = !expanded
            }.background(Color(0xFF1A1A2E).copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Header always visible
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "About permissions & compliance",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "▼",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = arrowRotation
                },
            )
        }

        // Scrollable text - only when expanded, takes the remaining predetermined space
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                val disclaimerText = buildAnnotatedString {
                    append(
                        "This app uses the 'Modify system settings' permission to control screen timeout. " +
                            "This is required for the 'never sleep' feature. " +
                            "Enabling this may increase battery usage and device temperature. " +
                            "Some devices (e.g. Samsung, Xiaomi) or enterprise policies may override this setting. " +
                            "You can revoke access anytime in Android Settings > Apps > Special app access.\n\n" +
                            "Ads: A small banner ad may appear at the bottom. You can disable ads in Settings. " +
                            "Google AdMob may collect device identifiers for ad delivery — see our ",
                    )
                    pushStringAnnotation(tag = "privacy", annotation = privacyPolicyUrl)
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF9E8CFF),
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append("privacy policy")
                    }
                    pop()
                    append(".")
                }
                ClickableText(
                    text = disclaimerText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Start,
                    ),
                    onClick = { offset ->
                        disclaimerText
                            .getStringAnnotations("privacy", offset, offset)
                            .firstOrNull()
                            ?.let { uriHandler.openUri(it.item) }
                    },
                )
            }
        }
    }
}
