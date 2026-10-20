package com.lizz.neversleep

import android.content.SharedPreferences
import com.lizz.neversleep.review.ReviewState
import com.lizz.neversleep.review.ReviewStateStore

internal class SharedPreferencesReviewStateStore(
    private val preferences: SharedPreferences,
) : ReviewStateStore {
    override fun load(): ReviewState =
        ReviewState(
            completedSessions = preferences.getInt(KEY_COMPLETED_SESSIONS, 0),
            requestAttempted = preferences.getBoolean(KEY_REQUEST_ATTEMPTED, false),
        )

    override fun save(state: ReviewState): Boolean {
        val previous = load()
        if (edit(state).commit()) return true
        // SharedPreferences updates its memory cache even when the disk commit
        // fails. Restore it so a failed claim does not consume eligibility.
        edit(previous).apply()
        return false
    }

    private fun edit(state: ReviewState): SharedPreferences.Editor =
        preferences
            .edit()
            .putInt(KEY_COMPLETED_SESSIONS, state.completedSessions)
            .putBoolean(KEY_REQUEST_ATTEMPTED, state.requestAttempted)

    private companion object {
        const val KEY_COMPLETED_SESSIONS = "review_completed_sessions"
        const val KEY_REQUEST_ATTEMPTED = "review_request_attempted"
    }
}
