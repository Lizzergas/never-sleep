package com.lizz.neversleep

import com.lizz.neversleep.di.initKoin
import com.lizz.neversleep.onboarding.OnboardingRoute
import com.lizz.neversleep.onboarding.domain.OnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals

class AppStartupTest {
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() != null) stopKoin()
        dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        if (GlobalContext.getOrNull() != null) stopKoin()
        dataStoreScope.cancel()
    }

    @Test
    fun noOverrideReturnsDefaultRoute() =
        runBlocking {
            assertEquals(defaultStartRoute, resolveAppStartRoute())
        }

    @Test
    fun onboardingOverrideReturnsOnboardingRouteBeforeSeen() =
        runBlocking {
            startAppKoinForStartupTest()

            assertEquals(OnboardingRoute, resolveAppStartRoute())
        }

    @Test
    fun onboardingOverrideReturnsDefaultRouteAfterSeen() =
        runBlocking {
            startAppKoinForStartupTest()
            GlobalContext.get().get<OnboardingRepository>().markSeen()

            assertEquals(defaultStartRoute, resolveAppStartRoute())
        }

    private fun startAppKoinForStartupTest() {
        initKoin()
        loadKoinModules(listOf(testDataStoreModule(dataStoreScope), testDatabaseModule()))
    }
}
