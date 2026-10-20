package com.lizz.neversleep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lizz.neversleep.review.ReviewRequester
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
        const val EXTRA_DESIRED_ENABLED = "desired_enabled"
    }

    // Class-level state so onResume() can refresh it when the app is minimized/resumed.
    private val isEnabledState = mutableStateOf(false)

    // Desired state remembered while we prompt for the write-settings permission.
    private var pendingDesired: Boolean? = null
    private val showWriteSettingsExplanationState = mutableStateOf(false)
    private val showSettingsDialogState = mutableStateOf(false)

    // Ads visibility (persisted; default true so the stub ad shows unless the user disables it).
    private val showAdsState = mutableStateOf(true)
    private val canWriteSettingsState = mutableStateOf(false)
    private lateinit var adConsentManager: AdConsentManager
    private lateinit var reviewRequester: ReviewRequester
    private var screenshotMode = false
    private var consentGatherStarted = false
    private val canPresentReview: () -> Boolean = {
        !screenshotMode &&
            !isEnabledState.value &&
            !showWriteSettingsExplanationState.value &&
            !showSettingsDialogState.value &&
            pendingDesired == null &&
            !adConsentManager.isBusyState.value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        isEnabledState.value = NeverSleepController.isEnabled(this)
        showAdsState.value = NeverSleepController.areAdsEnabled(this)
        canWriteSettingsState.value = Settings.System.canWrite(this)

        screenshotMode = intent.getBooleanExtra(EXTRA_SCREENSHOT_MODE, false)
        val neverSleepApplication = application as NeverSleepApplication
        adConsentManager = AdConsentManager(
            activity = this,
            onAdsMayLoad = neverSleepApplication::initializeMobileAds,
        )
        reviewRequester = GooglePlayReviewRequester(
            activity = this,
            policy = neverSleepApplication.reviewPolicy,
            canPresent = canPresentReview,
        )

        if (!acceptWriteSettingsRequest(intent)) {
            gatherConsentIfNeeded()
        }

        if (screenshotMode && Settings.System.canWrite(this)) {
            val captureNever = intent.getBooleanExtra(EXTRA_CAPTURE_NEVER, false)
            if (captureNever) {
                NeverSleepController.setNeverSleep(this, countForReview = false)
                isEnabledState.value = true
            } else {
                NeverSleepController.setNormal(this, countForReview = false)
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
                val consentBusy by remember { adConsentManager.isBusyState }
                val showSettingsDialog by remember { showSettingsDialogState }

                LaunchedEffect(isEnabled, consentBusy, showWriteSettingsExplanation, showSettingsDialog) {
                    reviewRequester.requestReviewIfEligible()
                }

                // Read the latest state at tap time so the flip is reliable even after
                // resume/minimize. The UI always reflects the resulting system state.
                val onToggle: () -> Unit = {
                    val desired = !isEnabledState.value
                    if (Settings.System.canWrite(this)) {
                        setDesiredMode(desired)
                        isEnabledState.value = NeverSleepController.isEnabled(this)
                    } else {
                        // Explain the special permission before handing off to Android Settings.
                        requestWriteSettingsAccess(desired)
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
                    showSettingsDialog = showSettingsDialog,
                    onSetShowSettingsDialog = { showSettingsDialogState.value = it },
                    onSetShowAds = { enabled ->
                        showAdsState.value = enabled
                        NeverSleepController.setAdsEnabled(this, enabled)
                        adConsentManager.setAdsRequested(enabled)
                    },
                    onOpenPrivacyPolicy = ::openPrivacyPolicy,
                    onOpenPrivacyOptions = adConsentManager::showPrivacyOptions,
                    onOpenWriteSettings = {
                        if (canWriteSettingsState.value) {
                            openWriteSettingsPermissionScreen()
                        } else {
                            requestWriteSettingsAccess(desired = null)
                        }
                    },
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
                            clearWriteSettingsRequestExtras()
                            gatherConsentIfNeeded()
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
            applyPendingDesired()
        } else if (pendingDesired != null && !showWriteSettingsExplanationState.value) {
            // The user returned without granting access. Do not re-open the rationale
            // after a later configuration change.
            pendingDesired = null
            clearWriteSettingsRequestExtras()
        }

        if (!showWriteSettingsExplanationState.value) {
            gatherConsentIfNeeded()
        }
        // The posted callback runs after the lifecycle reaches RESUMED, including
        // returns from widget/tile use without another Compose state change.
        window.decorView.post { reviewRequester.requestReviewIfEligible() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        canWriteSettingsState.value = Settings.System.canWrite(this)
        acceptWriteSettingsRequest(intent)
    }

    private fun acceptWriteSettingsRequest(intent: Intent): Boolean {
        if (!intent.getBooleanExtra(EXTRA_REQUEST_WRITE_SETTINGS, false)) return false

        pendingDesired = intent.getBooleanExtra(EXTRA_DESIRED_ENABLED, true)
        if (Settings.System.canWrite(this)) {
            applyPendingDesired()
        } else {
            showWriteSettingsExplanationState.value = true
        }
        return true
    }

    private fun applyPendingDesired() {
        val desired = pendingDesired ?: return
        pendingDesired = null
        setDesiredMode(desired)
        isEnabledState.value = NeverSleepController.isEnabled(this)
        clearWriteSettingsRequestExtras()
    }

    private fun requestWriteSettingsAccess(desired: Boolean?) {
        pendingDesired = desired
        if (desired != null) {
            // Keep the requested transition across activity recreation or process death
            // while Android's Special app access screen is in the foreground.
            intent.putExtra(EXTRA_REQUEST_WRITE_SETTINGS, true)
            intent.putExtra(EXTRA_DESIRED_ENABLED, desired)
        }
        showWriteSettingsExplanationState.value = true
    }

    private fun setDesiredMode(desired: Boolean) {
        if (desired) {
            NeverSleepController.setNeverSleep(this, countForReview = !screenshotMode)
        } else {
            NeverSleepController.setNormal(this, countForReview = !screenshotMode)
        }
    }

    private fun clearWriteSettingsRequestExtras() {
        intent.removeExtra(EXTRA_REQUEST_WRITE_SETTINGS)
        intent.removeExtra(EXTRA_DESIRED_ENABLED)
    }

    private fun gatherConsentIfNeeded() {
        if (screenshotMode || consentGatherStarted) return
        consentGatherStarted = true
        adConsentManager.gatherConsent(requestAds = showAdsState.value)
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
