package com.lizz.neversleep.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.navigation3.runtime.NavKey
import com.lizz.neversleep.datastore.safeData
import com.lizz.neversleep.navigation.StartRouteOverride
import com.lizz.neversleep.onboarding.OnboardingRoute
import com.lizz.neversleep.onboarding.domain.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persists the first-launch flag and overrides the start destination until
 * onboarding has been completed.
 */
class DataStoreOnboardingRepository(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository,
    StartRouteOverride {
    override val hasSeenOnboarding: Flow<Boolean> = dataStore.safeData().map { it[SEEN_KEY] == true }

    override suspend fun markSeen() {
        dataStore.edit { it[SEEN_KEY] = true }
    }

    override suspend fun startRoute(): NavKey? = if (hasSeenOnboarding.first()) null else OnboardingRoute

    private companion object {
        val SEEN_KEY = booleanPreferencesKey("onboarding_seen")
    }
}
