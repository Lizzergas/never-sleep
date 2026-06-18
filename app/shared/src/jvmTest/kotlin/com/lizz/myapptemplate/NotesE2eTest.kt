package com.lizz.myapptemplate

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.database.NoteDao
import com.lizz.myapptemplate.di.initKoin
import com.lizz.myapptemplate.network.NetworkConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertTrue

/**
 * The reference feature end to end: register (auth), add a note through the
 * UI, server round-trip, and the Room cache holding the data.
 */
class NotesE2eTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var server: EmbeddedServer<*, *>
    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)

    @Before
    fun setUp() {
        server = embeddedServer(Netty, port = 0) { module() }.start(wait = false)
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
                },
            ),
        )
        skipOnboardingForTests()
        // Notes are per-user: establish a session first.
        runBlocking {
            GlobalContext.get().get<SessionRepository>().register("notes-e2e@test.dev", "password123")
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
    }

    @Test
    fun addNoteThroughUiRoundTripsServerAndCache() {
        val noteText = "buy oat milk"

        rule.setContent {
            TestAppOwner { App(startRoute = defaultStartRoute) }
        }

        rule.onNodeWithText("Notes").performClick()
        rule.waitForIdle()

        rule.onNode(hasSetTextAction() and hasText("New note")).performTextInput(noteText)
        rule.onNodeWithText("Add").performClick()

        // Server accepted it and the Room cache holds it for offline reads.
        rule.waitUntil(timeoutMillis = 15_000) {
            cacheContainsNote(noteText)
        }

        // The UI shows the saved row from the cache flow, not just the draft field.
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithText(noteText).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(cacheContainsNote(noteText))
    }

    private fun cacheContainsNote(text: String): Boolean =
        runBlocking {
            GlobalContext
                .get()
                .get<NoteDao>()
                .observeAll()
                .first()
                .any { it.text == text }
        }
}
