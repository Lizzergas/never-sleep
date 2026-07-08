package com.lizz.neversleep.onboarding.domain

import kotlinx.coroutines.flow.Flow

/** First-launch flag — implemented in data/. */
interface OnboardingRepository {
    val hasSeenOnboarding: Flow<Boolean>

    suspend fun markSeen()
}
