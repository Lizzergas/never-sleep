package com.lizz.myapptemplate

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
    }

    @Test
    fun showcaseListsFeaturesAndNavigatesToGalleryAndBack() {
        rule.setContent {
            TestViewModelStoreOwner {
                App()
            }
        }

        // Home lists catalog-derived features
        rule.onNodeWithText("Installed features").assertIsDisplayed()
        rule.onNodeWithText("Design system gallery").assertIsDisplayed()
        rule.onNodeWithText("Network demo").assertIsDisplayed()
        rule.onNodeWithText("Database demo").assertIsDisplayed()
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
            TestViewModelStoreOwner {
                App()
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
    fun databaseDemoInsertsAndObservesNotes() {
        rule.setContent {
            TestViewModelStoreOwner {
                App()
            }
        }

        rule.onNodeWithText("Database demo").performClick()
        rule.waitForIdle()

        rule.onNode(hasSetTextAction()).performTextInput("buy milk")
        rule.onNodeWithText("Add note").performClick()

        // Insert -> Room -> observeAll Flow -> UI
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("buy milk").fetchSemanticsNodes().isNotEmpty()
        }
    }
}

@androidx.compose.runtime.Composable
private fun TestViewModelStoreOwner(content: @androidx.compose.runtime.Composable () -> Unit) {
    val owner =
        androidx.compose.runtime.remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}
