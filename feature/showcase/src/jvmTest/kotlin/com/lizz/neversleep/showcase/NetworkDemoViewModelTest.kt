package com.lizz.neversleep.showcase

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.lizz.neversleep.connectivity.ConnectivityMonitor
import com.lizz.neversleep.model.AppError
import com.lizz.neversleep.model.Item
import com.lizz.neversleep.network.NetworkConfig
import com.lizz.neversleep.network.createHttpClient
import com.lizz.neversleep.showcase.presentation.network.NetworkDemoEvent
import com.lizz.neversleep.showcase.presentation.network.NetworkDemoUiState
import com.lizz.neversleep.showcase.presentation.network.NetworkDemoViewModel
import com.lizz.neversleep.ui.UiState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import kotlinx.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkDemoViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsIdle() =
        runBlocking {
            val viewModel = viewModel(successEngine())

            assertEquals(NetworkDemoUiState(), viewModel.state.value)
        }

    @Test
    fun loadEmitsLoadingThenSuccess() =
        runBlocking {
            val proceed = Channel<Unit>(Channel.UNLIMITED)
            val viewModel = viewModel(
                MockEngine {
                    proceed.receive()
                    respond(ITEMS_BODY, HttpStatusCode.OK, jsonHeaders)
                },
            )

            viewModel.state.test {
                assertEquals(NetworkDemoUiState(), awaitItem())

                viewModel.onEvent(NetworkDemoEvent.Load)
                assertEquals(NetworkDemoUiState(items = UiState.Loading), awaitItem())

                proceed.send(Unit)
                assertEquals(
                    NetworkDemoUiState(items = UiState.Success(listOf(ITEM))),
                    awaitState(this) { it.items is UiState.Success },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun loadEmitsLoadingThenError() =
        runBlocking {
            val proceed = Channel<Unit>(Channel.UNLIMITED)
            val viewModel = viewModel(
                MockEngine {
                    proceed.receive()
                    throw IOException("offline")
                },
            )

            viewModel.state.test {
                assertEquals(NetworkDemoUiState(), awaitItem())

                viewModel.onEvent(NetworkDemoEvent.Load)
                assertEquals(NetworkDemoUiState(items = UiState.Loading), awaitItem())

                proceed.send(Unit)
                assertEquals(
                    NetworkDemoUiState(items = UiState.Error(AppError.Network)),
                    awaitState(this) { it.items is UiState.Error },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reconnectRetriesAfterNetworkError() =
        runBlocking {
            val connectivity = FakeConnectivityMonitor()
            var calls = 0
            val viewModel = viewModel(
                MockEngine {
                    calls += 1
                    if (calls == 1) {
                        throw IOException("offline")
                    }
                    respond(ITEMS_BODY, HttpStatusCode.OK, jsonHeaders)
                },
                connectivity,
            )

            viewModel.state.test {
                assertEquals(NetworkDemoUiState(), awaitItem())

                viewModel.onEvent(NetworkDemoEvent.Load)
                awaitState(this) { it.items == UiState.Error(AppError.Network) }

                connectivity.setOnline(true)
                assertEquals(
                    NetworkDemoUiState(items = UiState.Success(listOf(ITEM))),
                    awaitState(this) { it.items is UiState.Success },
                )
                assertEquals(2, calls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reconnectDoesNotRetryAfterValidationError() =
        runBlocking {
            val connectivity = FakeConnectivityMonitor()
            var calls = 0
            val viewModel = viewModel(
                MockEngine {
                    calls += 1
                    respond("""{"error":"bad request"}""", HttpStatusCode.BadRequest, jsonHeaders)
                },
                connectivity,
            )

            viewModel.state.test {
                assertEquals(NetworkDemoUiState(), awaitItem())

                viewModel.onEvent(NetworkDemoEvent.Load)
                awaitState(this) { it.items == UiState.Error(AppError.Validation(400)) }

                connectivity.setOnline(true)
                yield()
                assertEquals(1, calls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun viewModel(
        engine: MockEngine,
        connectivityMonitor: ConnectivityMonitor? = null,
    ): NetworkDemoViewModel =
        NetworkDemoViewModel(
            httpClient = createHttpClient(NetworkConfig(), engine),
            connectivityMonitor = connectivityMonitor,
        )

    private fun successEngine(): MockEngine =
        MockEngine {
            respond(ITEMS_BODY, HttpStatusCode.OK, jsonHeaders)
        }

    private suspend fun awaitState(
        turbine: ReceiveTurbine<NetworkDemoUiState>,
        predicate: (NetworkDemoUiState) -> Boolean,
    ): NetworkDemoUiState {
        repeat(10) {
            val item = turbine.awaitItem()
            if (predicate(item)) return item
        }
        error("No matching NetworkDemoUiState received")
    }

    private class FakeConnectivityMonitor : ConnectivityMonitor {
        private val online = MutableStateFlow(false)
        override val isOnline: Flow<Boolean> = online

        fun setOnline(value: Boolean) {
            online.value = value
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
        const val ITEMS_BODY = """[{"id":1,"title":"Item","description":"Description"}]"""
        val ITEM = Item(id = 1, title = "Item", description = "Description")
    }
}
