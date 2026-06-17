package com.lizz.myapptemplate

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/**
 * Integration test of the feature registry: the showcase home lists features
 * from the catalog and Navigation3 routes between registered entries.
 */
class AppNavigationTest {
    @get:Rule
    val rule = createComposeRule()

    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) initKoin()
        loadKoinModules(listOf(testDataStoreModule(dataStoreScope), testDatabaseModule()))
        skipOnboardingForTests()
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
    }

    @Test
    fun showcaseListsFeaturesAndNavigatesToGalleryAndBack() {
        rule.setContent {
            TestAppOwner {
                App(startRoute = defaultStartRoute)
            }
        }

        // Home lists catalog-derived features
        rule.onNodeWithText("Installed features").assertIsDisplayed()
        rule.onNodeWithText("Design system gallery").assertIsDisplayed()
        rule.onNodeWithText("Network demo").assertIsDisplayed()
        rule.onNodeWithText("Notes").assertIsDisplayed()
        rule.onNodeWithText("Settings").assertIsDisplayed()

        // Navigate to the gallery
        rule.onNodeWithText("Design system gallery").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Design system").assertIsDisplayed()
        rule.onNodeWithText("Typography").assertIsDisplayed()

        // And back
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Installed features").assertIsDisplayed()
    }

    @Test
    fun settingsThemeSelectionPersistsThroughTheRealChain() {
        rule.setContent {
            TestAppOwner {
                App(startRoute = defaultStartRoute)
            }
        }

        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()

        // Default is System; selecting Dark goes UI -> ViewModel -> repository
        // -> DataStore -> Flow -> UI.
        rule.onNodeWithText("Dark").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            runCatching { rule.onNodeWithText("Dark").assertIsSelected() }.isSuccess
        }
    }

    @Test
    fun topLevelRootsDoNotRenderBackAndHomeDetailStackIsRetained() {
        rule.setContent {
            TestAppOwner {
                App(startRoute = defaultStartRoute)
            }
        }

        rule.onNodeWithText("Installed features").assertIsDisplayed()
        rule.onAllNodesWithText("Back").assertCountEquals(0)

        rule.onNodeWithText("Design system gallery").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Design system").assertIsDisplayed()
        rule.onNodeWithText("Back").assertIsDisplayed()

        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Theme").assertIsDisplayed()
        rule.onAllNodesWithText("Back").assertCountEquals(0)

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Design system").assertIsDisplayed()
        rule.onNodeWithText("Back").assertIsDisplayed()

        rule.onNodeWithText("Home").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Installed features").assertIsDisplayed()
        rule.onAllNodesWithText("Back").assertCountEquals(0)
    }

    @Test
    fun switchingFromSettingsToAccountDoesNotLeaveSettingsContentMounted() {
        rule.setContent {
            TestAppOwner {
                App(startRoute = defaultStartRoute)
            }
        }

        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Theme").assertIsDisplayed()

        rule.onNodeWithText("Account").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Email").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("Email").assertIsDisplayed()
        rule.onAllNodesWithText("Theme").assertCountEquals(0)
        rule.onAllNodesWithText("Back").assertCountEquals(0)
    }
}
