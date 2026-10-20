package com.lizz.neversleep.review

/**
 * A platform presenter, scoped to a live Activity or scene. Requests are best-effort:
 * the store may suppress its sheet, and completion does not imply a submitted review.
 */
interface ReviewRequester {
    fun requestReviewIfEligible()
}

/** For previews, tests, and hosts that have no native review implementation. */
class NoOpReviewRequester : ReviewRequester {
    override fun requestReviewIfEligible() = Unit
}
