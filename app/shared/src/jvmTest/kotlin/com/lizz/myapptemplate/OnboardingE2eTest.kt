package com.lizz.myapptemplate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.lizz.myapptemplate.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin

/** Conditional start destination: onboarding on first launch, home afterwards. */
class OnboardingE2eTest {
    @get:Rule
    val rule = createComposeRule()

    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) initKoin()
        loadKoinModules(listOf(testDataStoreModule(dataStoreScope), testDatabaseModule()))
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
    }

    @Test
    fun firstLaunchShowsOnboardingAndGetStartedLandsOnHome() {
        rule.setContent {
            OnboardingTestOwner { App() }
        }

        // Fresh DataStore -> onboarding is the start destination.
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Welcome").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Get started").performClick()

        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Installed features").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Installed features").assertIsDisplayed()
    }

    @Test
    fun seenFlagSkipsOnboardingOnNextLaunch() {
        skipOnboardingForTests()

        rule.setContent {
            OnboardingTestOwner { App() }
        }

        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Installed features").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Welcome").assertDoesNotExist()
    }
}

@Composable
private fun OnboardingTestOwner(content: @Composable () -> Unit) {
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}
