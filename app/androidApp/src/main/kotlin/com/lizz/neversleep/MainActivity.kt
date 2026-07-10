package com.lizz.neversleep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Thin adapter: owns Activity lifecycle and the screen's mutable state, wires taps to
 * [NeverSleepController], and hosts [NeverSleepScreen]. All persistence/system-settings
 * logic lives in the controller; all layout lives in the screen modules.
 */
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SCREENSHOT_MODE = "screenshot_mode"
        const val EXTRA_CAPTURE_NEVER = "capture_never"
    }

    // Class-level state so onResume() can refresh it when the app is minimized/resumed.
    private val isEnabledState = mutableStateOf(false)

    // Desired state remembered while we prompt for the write-settings permission.
    private var pendingDesired: Boolean? = null

    // Ads visibility (persisted; default true so the stub ad shows unless the user disables it).
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
            NeverSleepTheme {
                val isEnabled by remember { isEnabledState }
                val showAds by remember { showAdsState }

                // Read the latest state at tap time so the flip is reliable even after
                // resume/minimize. The UI follows the user's intent immediately; the system
                // value may be clamped by the device.
                val onToggle: () -> Unit = {
                    val desired = !isEnabledState.value
                    if (Settings.System.canWrite(this)) {
                        if (desired) {
                            NeverSleepController.setNeverSleep(this)
                        } else {
                            NeverSleepController.setNormal(this)
                        }
                        isEnabledState.value = desired
                    } else {
                        // No permission yet: remember intent, prompt, auto-apply on resume.
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
        // Refresh state when the app returns from background — fixes stale toggle state.
        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)

        // Apply a toggle that was pending on the write-settings permission grant.
        if (pendingDesired != null && Settings.System.canWrite(this)) {
            val desired = pendingDesired!!
            pendingDesired = null
            if (desired) {
                NeverSleepController.setNeverSleep(this)
            } else {
                NeverSleepController.setNormal(this)
            }
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
