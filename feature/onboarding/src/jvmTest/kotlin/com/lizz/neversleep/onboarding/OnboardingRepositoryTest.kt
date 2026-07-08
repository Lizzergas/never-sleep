package com.lizz.neversleep.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lizz.neversleep.onboarding.data.DataStoreOnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingRepositoryTest {
    @Test
    fun startsAsUnseenAndOverridesStartRoute() =
        runTest {
            val scope = CoroutineScope(Job() + Dispatchers.Default)
            val repository = repository(scope)

            assertEquals(false, repository.hasSeenOnboarding.first())
            assertEquals(OnboardingRoute, repository.startRoute())
            scope.cancel()
        }

    @Test
    fun markSeenPersistsAndStopsOverriding() =
        runTest {
            val scope = CoroutineScope(Job() + Dispatchers.Default)
            val repository = repository(scope)

            repository.markSeen()

            assertTrue(repository.hasSeenOnboarding.first())
            assertNull(repository.startRoute())
            scope.cancel()
        }

    private fun repository(scope: CoroutineScope): DataStoreOnboardingRepository {
        val file = Files.createTempDirectory("ds").resolve("t.preferences_pb").toString()
        return DataStoreOnboardingRepository(
            PreferenceDataStoreFactory.createWithPath(scope = scope) { file.toPath() },
        )
    }
}
