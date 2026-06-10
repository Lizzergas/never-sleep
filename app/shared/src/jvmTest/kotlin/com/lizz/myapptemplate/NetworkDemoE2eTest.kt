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
import com.lizz.myapptemplate.network.NetworkConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Full vertical slice: the real Ktor server (in-process, random port), the
 * real core:network HttpClient, shared core:model DTOs, and the actual
 * showcase UI driven through Navigation3.
 */
class NetworkDemoE2eTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var server: EmbeddedServer<*, *>
    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)

    @Before
    fun setUp() {
        server = embeddedServer(Netty, port = 0) { module() }.start(wait = false)
        val port = runBlocking { server.engine.resolvedConnectors().first().port }

        if (GlobalContext.getOrNull() == null) initKoin()
        loadKoinModules(
            listOf(
                testDataStoreModule(dataStoreScope),
                module {
                    single { NetworkConfig(baseUrl = "http://localhost:$port") }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
    }

    @Test
    fun networkDemoLoadsItemsFromTheRealServer() {
        rule.setContent {
            TestOwner { App() }
        }

        rule.onNodeWithText("Network demo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Load items").performClick()

        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Shared DTOs").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Shared DTOs").assertIsDisplayed()
        rule.onNodeWithText("Typed errors").assertIsDisplayed()
    }

    @Test
    fun networkDemoShowsFriendlyFailureWhenServerIsDown() {
        // Point the client at a dead port.
        loadKoinModules(
            module {
                single { NetworkConfig(baseUrl = "http://localhost:1", connectTimeoutMs = 2_000) }
            },
        )

        rule.setContent {
            TestOwner { App() }
        }

        rule.onNodeWithText("Network demo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Load items").performClick()

        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Is the server running? Start it with: ./gradlew :server:run")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}

@Composable
private fun TestOwner(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}
