package com.lizz.neversleep.review

data class ReviewState(
    val completedSessions: Int = 0,
    val requestAttempted: Boolean = false,
)

interface ReviewStateStore {
    fun load(): ReviewState

    /** Returns false if the state could not be saved. */
    fun save(state: ReviewState): Boolean
}

/**
 * Store-independent eligibility for one automatic review request per installation.
 *
 * Hosts serialize calls on their UI thread (or under a shared lock). State is read
 * afresh so multiple scene/Activity adapters cannot claim the same request. Store
 * presentation and lifecycle checks belong to the platform [ReviewRequester].
 */
class ReviewPolicy(
    private val store: ReviewStateStore,
    private val minimumCompletedSessions: Int = 5,
) {
    init {
        require(minimumCompletedSessions > 0)
    }

    fun recordCompletedSession() {
        val state = store.load()
        if (state.requestAttempted || state.completedSessions >= minimumCompletedSessions) return
        store.save(state.copy(completedSessions = state.completedSessions.coerceAtLeast(0) + 1))
    }

    fun isEligible(): Boolean = store.load().isEligible()

    /**
     * Call only when the native presenter is ready, immediately before launch.
     * A successful claim records an attempt, never a submitted review or shown sheet.
     * Unavailable presenters must not claim; a failed save leaves the request pending.
     */
    fun claimRequest(): Boolean {
        val state = store.load()
        return state.isEligible() && store.save(state.copy(requestAttempted = true))
    }

    private fun ReviewState.isEligible(): Boolean = completedSessions >= minimumCompletedSessions && !requestAttempted
}
