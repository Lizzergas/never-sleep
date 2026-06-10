package com.lizz.myapptemplate

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.lizz.myapptemplate.di.initKoin
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

/**
 * Integration test of the feature registry: the showcase home lists features
 * from the catalog and Navigation3 routes between registered entries.
 */
class AppNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) initKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
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
        rule.onNodeWithText("Dependency demo").assertIsDisplayed()

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
}

@androidx.compose.runtime.Composable
private fun TestViewModelStoreOwner(content: @androidx.compose.runtime.Composable () -> Unit) {
    val owner = androidx.compose.runtime.remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}
