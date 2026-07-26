package com.lizz.neversleep

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Coordinates Google's UMP flow for the current Activity.
 *
 * Consent is refreshed before ads initialize, and the observable state is the only
 * source used by the UI to decide whether an ad request is allowed.
 */
internal class AdConsentManager(
    private val activity: Activity,
    private val onAdsMayLoad: () -> Unit,
) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private var requestInFlight = false
    private var consentUpdateRequested = false
    private var consentUpdateComplete = false
    private var consentFormCheckComplete = false
    private var consentFormCheckInFlight = false
    private var adsRequested = false

    val canRequestAdsState = mutableStateOf(false)
    val privacyOptionsRequiredState = mutableStateOf(false)

    fun gatherConsent(requestAds: Boolean) {
        adsRequested = requestAds

        if (consentUpdateComplete) {
            refreshState()
            showRequiredFormBeforeAds()
            return
        }
        if (requestInFlight || consentUpdateRequested) return
        requestInFlight = true
        consentUpdateRequested = true

        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                requestInFlight = false
                consentUpdateComplete = true
                refreshState()
                showRequiredFormBeforeAds()
            },
            { requestError ->
                requestInFlight = false
                consentUpdateRequested = false
                Log.w(TAG, "Unable to update consent information: ${requestError.message}")
                // A valid choice from a previous session may still permit ads.
                refreshState()
            },
        )

        // UMP can expose a still-valid decision from an earlier session immediately
        // after the update request starts.
        refreshState()
    }

    fun setAdsRequested(enabled: Boolean) {
        if (enabled) {
            gatherConsent(requestAds = true)
        } else {
            adsRequested = false
        }
    }

    fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Unable to present privacy options: ${formError.message}")
            }
            refreshState()
        }
    }

    private fun showRequiredFormBeforeAds() {
        if (!adsRequested ||
            consentFormCheckComplete ||
            consentFormCheckInFlight ||
            !consentUpdateComplete
        ) {
            return
        }
        consentFormCheckInFlight = true
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            consentFormCheckInFlight = false
            // A transient load/show failure should be retryable if the user turns ads
            // off and on again during this session.
            consentFormCheckComplete = formError == null
            if (formError != null) {
                Log.w(TAG, "Unable to present the consent form: ${formError.message}")
            }
            refreshState()
        }
    }

    private fun refreshState() {
        val canRequestAds = consentInformation.canRequestAds()
        canRequestAdsState.value = canRequestAds
        privacyOptionsRequiredState.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        if (canRequestAds && adsRequested) {
            onAdsMayLoad()
        }
    }

    private companion object {
        const val TAG = "AdConsentManager"
    }
}
