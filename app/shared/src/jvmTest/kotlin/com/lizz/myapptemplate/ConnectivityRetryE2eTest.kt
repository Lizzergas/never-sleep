package com.lizz.myapptemplate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lizz.myapptemplate.di.initKoin
import com.lizz.myapptemplate.network.NetworkConfig
import com.lizz.myapptemplate.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.io.IOException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Offline banner + retry-on-reconnect: the network fails while "offline",
 * then connectivity returns and the network demo reloads by itself.
 */
class ConnectivityRetryE2eTest {
    @get:Rule
    val rule = createComposeRule()

    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)
    private val connectivity = FakeConnectivityMonitor(initiallyOnline = true)
    private val networkUp = AtomicBoolean(true)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) initKoin()
        // HttpClient whose transport we can break on demand.
        val flakyEngine = MockEngine {
            if (networkUp.get()) {
                respond(
                    """[{"id":1,"title":"Recovered item","description":"loaded after reconnect"}]""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                throw IOException("network down")
            }
        }
        loadKoinModules(
            listOf(
                testDataStoreModule(dataStoreScope),
                testDatabaseModule(),
                testConnectivityModule(connectivity),
                module {
                    single<HttpClient> { createHttpClient(NetworkConfig(), flakyEngine) }
                    single { NetworkConfig() }
                },
            ),
        )
        skipOnboardingForTests()
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
    }

    @Test
    fun offlineBannerShowsAndDemoRetriesOnReconnect() {
        rule.setContent {
            TestAppOwner { App(startRoute = defaultStartRoute) }
        }

        // Go offline: transport fails and the shell banner appears.
        networkUp.set(false)
        connectivity.state.value = false
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("You're offline").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("Network demo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Load items").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule
                .onAllNodesWithText("Can't reach the server. Check your connection.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Back online: banner goes away and the demo reloads without a click.
        networkUp.set(true)
        connectivity.state.value = true
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Recovered item").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Recovered item").assertIsDisplayed()
        rule.onNodeWithText("You're offline").assertDoesNotExist()
    }
}
