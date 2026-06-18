package com.lizz.myapptemplate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lizz.myapptemplate.auth.JwtConfig
import com.lizz.myapptemplate.auth.data.SessionRepositoryImpl
import com.lizz.myapptemplate.auth.data.TokenStorage
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.di.initKoin
import com.lizz.myapptemplate.model.TokenPair
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

private class InMemoryTokenStorage : TokenStorage {
    var tokens: TokenPair? = null

    override suspend fun read(): TokenPair? = tokens

    override suspend fun write(tokens: TokenPair) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

/**
 * Full auth slice against the real server: register through the UI, see the
 * profile, log out, log back in — plus the 401-refresh path with an
 * immediately-expiring access token.
 */
class AuthE2eTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var server: EmbeddedServer<*, *>
    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)
    private val tokenStorage = InMemoryTokenStorage()

    @Before
    fun setUp() {
        // Short-lived access tokens: fresh ones work for the immediate
        // /api/me call, but expire before the refresh-restore test below.
        server = embeddedServer(Netty, port = 0) {
            module(jwtConfig = JwtConfig(secret = "test-secret", accessTtlSeconds = 2))
        }.start(wait = false)
        val port = runBlocking {
            server.engine
                .resolvedConnectors()
                .first()
                .port
        }

        if (GlobalContext.getOrNull() == null) initKoin()
        loadKoinModules(
            listOf(
                testDataStoreModule(dataStoreScope),
                testDatabaseModule(),
                module {
                    single { NetworkConfig(baseUrl = "http://localhost:$port") }
                    single<TokenStorage> { tokenStorage }
                },
            ),
        )
        skipOnboardingForTests()
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
    }

    @Test
    fun registerLogoutLoginThroughTheUi() {
        rule.setContent {
            TestAppOwner { App(startRoute = defaultStartRoute) }
        }

        rule.onNodeWithText("Account").performClick()
        rule.waitForIdle()

        // Register
        rule.onNode(hasSetTextAction() and hasText("Email")).performTextInput("e2e@test.dev")
        rule
            .onNode(hasSetTextAction() and hasText("Password (min 8 chars)"))
            .performTextInput("password123")
        rule.onNodeWithText("Register").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Signed in as e2e@test.dev").fetchSemanticsNodes().isNotEmpty()
        }

        // Logout returns to the form
        rule.onNodeWithText("Log out").performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty()
        }

        // Login again
        rule.onNode(hasSetTextAction() and hasText("Email")).performTextInput("e2e@test.dev")
        rule
            .onNode(hasSetTextAction() and hasText("Password (min 8 chars)"))
            .performTextInput("password123")
        rule.onNodeWithText("Log in").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText("Signed in as e2e@test.dev").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Signed in as e2e@test.dev").assertIsDisplayed()
    }

    @Test
    fun restoreRefreshesExpiredAccessTokenAgainstRealServer() {
        rule.setContent {
            TestAppOwner { App(startRoute = defaultStartRoute) }
        }
        rule.waitForIdle()

        val koin = GlobalContext.get()
        val repository = koin.get<com.lizz.myapptemplate.auth.domain.SessionRepository>()

        runBlocking {
            repository.register("refresh@test.dev", "password123")
            // Let the 2s access token expire; a fresh repository restoring
            // from storage must then go through /api/auth/refresh.
            Thread.sleep(2_500)
            val restored = SessionRepositoryImpl(
                config = koin.get<NetworkConfig>(),
                storage = tokenStorage,
            )
            restored.restore()
            check(restored.sessionState.value is SessionState.LoggedIn) {
                "expected LoggedIn after refresh-restore, was ${restored.sessionState.value}"
            }
        }
    }
}
