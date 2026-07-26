package com.lizz.neversleep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.graphics.Color as AndroidColor

/**
 * Thin adapter: owns Activity lifecycle and the screen's mutable state, wires taps to
 * [NeverSleepController], and hosts [NeverSleepScreen]. All persistence/system-settings
 * logic lives in the controller; all layout lives in the screen modules.
 */
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SCREENSHOT_MODE = "screenshot_mode"
        const val EXTRA_CAPTURE_NEVER = "capture_never"
        const val EXTRA_REQUEST_WRITE_SETTINGS = "request_write_settings"
    }

    // Class-level state so onResume() can refresh it when the app is minimized/resumed.
    private val isEnabledState = mutableStateOf(false)

    // Desired state remembered while we prompt for the write-settings permission.
    private var pendingDesired: Boolean? = null
    private val showWriteSettingsExplanationState = mutableStateOf(false)

    // Ads visibility (persisted; default true so the stub ad shows unless the user disables it).
    private val showAdsState = mutableStateOf(true)
    private val canWriteSettingsState = mutableStateOf(false)
    private lateinit var adConsentManager: AdConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)
        canWriteSettingsState.value = Settings.System.canWrite(this)

        val screenshotMode = intent.getBooleanExtra(EXTRA_SCREENSHOT_MODE, false)
        val neverSleepApplication = application as NeverSleepApplication
        adConsentManager = AdConsentManager(
            activity = this,
            onAdsMayLoad = neverSleepApplication::initializeMobileAds,
        )
        if (!screenshotMode) {
            adConsentManager.gatherConsent(requestAds = showAdsState.value)
        }

        if (intent.getBooleanExtra(EXTRA_REQUEST_WRITE_SETTINGS, false) &&
            !Settings.System.canWrite(this)
        ) {
            pendingDesired = true
            showWriteSettingsExplanationState.value = true
        }

        if (screenshotMode && Settings.System.canWrite(this)) {
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
                val canWriteSettings by remember { canWriteSettingsState }
                val canRequestAds by remember { adConsentManager.canRequestAdsState }
                val privacyOptionsRequired by remember {
                    adConsentManager.privacyOptionsRequiredState
                }
                val mobileAdsReady by remember {
                    neverSleepApplication.mobileAdsReadyState
                }
                val showWriteSettingsExplanation by remember {
                    showWriteSettingsExplanationState
                }

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
                        // Explain the special permission before handing off to Android Settings.
                        pendingDesired = desired
                        showWriteSettingsExplanationState.value = true
                    }
                }

                NeverSleepScreen(
                    isEnabled = isEnabled,
                    onToggle = onToggle,
                    showAds = showAds,
                    showAdBanner = showAds &&
                        canRequestAds &&
                        mobileAdsReady &&
                        !screenshotMode,
                    canWriteSettings = canWriteSettings,
                    privacyOptionsRequired = privacyOptionsRequired,
                    screenshotMode = screenshotMode,
                    onSetShowAds = { enabled ->
                        showAdsState.value = enabled
                        NeverSleepController.setAdsEnabled(this, enabled)
                        adConsentManager.setAdsRequested(enabled)
                    },
                    onOpenPrivacyPolicy = ::openPrivacyPolicy,
                    onOpenPrivacyOptions = adConsentManager::showPrivacyOptions,
                    onOpenWriteSettings = ::openWriteSettingsPermissionScreen,
                )

                if (showWriteSettingsExplanation) {
                    WriteSettingsPermissionDialog(
                        onContinue = {
                            showWriteSettingsExplanationState.value = false
                            openWriteSettingsPermissionScreen()
                        },
                        onDismiss = {
                            pendingDesired = null
                            showWriteSettingsExplanationState.value = false
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh state when the app returns from background — fixes stale toggle state.
        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)
        canWriteSettingsState.value = Settings.System.canWrite(this)

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

    private fun openPrivacyPolicy() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(BuildConfig.PRIVACY_POLICY_URL),
            ),
        )
    }
}
