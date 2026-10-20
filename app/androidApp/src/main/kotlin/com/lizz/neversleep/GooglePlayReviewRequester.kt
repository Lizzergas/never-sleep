package com.lizz.neversleep

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.lizz.neversleep.review.ReviewPolicy
import com.lizz.neversleep.review.ReviewRequester

/** Owned by one Activity; never retained by the Application or shared policy. */
internal class GooglePlayReviewRequester(
    private val activity: ComponentActivity,
    private val policy: ReviewPolicy,
    private val canPresent: () -> Boolean,
    private val manager: ReviewManager = ReviewManagerFactory.create(activity),
) : ReviewRequester {
    // Avoid retrying SDK failures on every recomposition; a new Activity can retry.
    private var preparationAttempted = false

    override fun requestReviewIfEligible() {
        if (preparationAttempted || !isHostReady() || !policy.isEligible()) return
        preparationAttempted = true

        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            if (!isHostReady()) {
                // Backgrounded or another dialog opened while Play prepared the flow.
                preparationAttempted = false
                return@addOnCompleteListener
            }
            if (policy.claimRequest()) {
                // The store can suppress the sheet. Never infer that a review was left.
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }

    private fun isHostReady(): Boolean =
        !activity.isFinishing &&
            !activity.isDestroyed &&
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            canPresent()
}
