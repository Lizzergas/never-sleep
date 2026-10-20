package com.lizz.neversleep.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewPolicyTest {
    @Test
    fun eligibleOnlyAfterFiveCompletedSessions() {
        val store = MemoryStore()
        val policy = ReviewPolicy(store)
        repeat(4) {
            policy.recordCompletedSession()
            assertFalse(policy.isEligible())
            assertFalse(policy.claimRequest())
        }
        policy.recordCompletedSession()
        assertTrue(policy.isEligible())
        assertEquals(5, store.state.completedSessions)
    }

    @Test
    fun eligibilitySurvivesRecreationAndAdditionalSessions() {
        val store = MemoryStore()
        repeat(8) { ReviewPolicy(store).recordCompletedSession() }
        assertTrue(ReviewPolicy(store).isEligible())
        assertEquals(5, store.state.completedSessions)
    }

    @Test
    fun onlyOnePresenterCanClaimEvenAfterRecreation() {
        val store = MemoryStore(ReviewState(completedSessions = 5))
        val first = ReviewPolicy(store)
        val second = ReviewPolicy(store)
        assertTrue(first.isEligible())
        assertTrue(second.isEligible())
        assertTrue(first.claimRequest())
        assertFalse(second.claimRequest())
        repeat(5) { second.recordCompletedSession() }
        assertFalse(ReviewPolicy(store).isEligible())
        assertTrue(store.state.requestAttempted)
    }

    @Test
    fun unavailableRequesterDoesNotConsumeEligibility() {
        val policy = ReviewPolicy(MemoryStore(ReviewState(completedSessions = 5)))
        NoOpReviewRequester().requestReviewIfEligible()
        assertTrue(policy.isEligible())
        assertTrue(policy.claimRequest())
    }

    @Test
    fun failedSaveDoesNotPermitLaunchAndCanBeRetried() {
        val store = MemoryStore(ReviewState(completedSessions = 5))
        val policy = ReviewPolicy(store)
        store.canSave = false
        assertFalse(policy.claimRequest())
        assertTrue(policy.isEligible())
        store.canSave = true
        assertTrue(policy.claimRequest())
    }

    @Test
    fun failedSessionSaveDoesNotAdvanceCount() {
        val store = MemoryStore(ReviewState(completedSessions = 4))
        store.canSave = false
        val policy = ReviewPolicy(store)
        policy.recordCompletedSession()
        assertFalse(policy.isEligible())
        store.canSave = true
        policy.recordCompletedSession()
        assertTrue(policy.isEligible())
    }

    private class MemoryStore(
        var state: ReviewState = ReviewState(),
    ) : ReviewStateStore {
        var canSave = true

        override fun load(): ReviewState = state

        override fun save(state: ReviewState): Boolean {
            if (!canSave) return false
            this.state = state
            return true
        }
    }
}
