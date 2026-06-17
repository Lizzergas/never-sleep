package com.lizz.myapptemplate.notes

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.notes.domain.AddNoteUseCase
import com.lizz.myapptemplate.notes.domain.Note
import com.lizz.myapptemplate.notes.domain.NotesRepository
import com.lizz.myapptemplate.notes.presentation.NotesEvent
import com.lizz.myapptemplate.notes.presentation.NotesUiState
import com.lizz.myapptemplate.notes.presentation.NotesViewModel
import com.lizz.myapptemplate.ui.UI_REFRESH_MINIMUM_VISIBLE_MILLIS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
private class ViewModelNotesRepository : NotesRepository {
    private val notes = MutableStateFlow(emptyList<Note>())
    val refreshResults = Channel<ApiResult<Unit>>(Channel.UNLIMITED)
    var refreshCalls = 0
        private set
    var addResult: ApiResult<Note> = ApiResult.Success(Note(1, "saved", Instant.fromEpochMilliseconds(0)))
    var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)

    override fun observeNotes(): Flow<List<Note>> = notes

    override suspend fun refresh(): ApiResult<Unit> {
        refreshCalls += 1
        return refreshResults.receive()
    }

    override suspend fun add(text: String): ApiResult<Note> = addResult

    override suspend fun delete(id: Long): ApiResult<Unit> = deleteResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialAutoRefreshFailureDoesNotPublishErrorBeforeMinimumWindow() =
        runTest(mainDispatcher) {
            val repository = ViewModelNotesRepository()
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.state.test {
                assertEquals(NotesUiState(isRefreshing = true), awaitItem())

                repository.refreshResults.send(ApiResult.Failure(AppError.Network))
                runCurrent()
                expectNoEvents()

                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS - 1L)
                runCurrent()
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertEquals(
                    NotesUiState(error = AppError.Network),
                    awaitState(this) { !it.isRefreshing && it.error == AppError.Network },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun failedRefreshKeepsPreviousErrorVisibleWhileRefreshing() =
        runTest(mainDispatcher) {
            val repository = ViewModelNotesRepository()
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.state.test {
                awaitState(this) { it.isRefreshing }
                repository.refreshResults.send(ApiResult.Failure(AppError.Network))
                runCurrent()
                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong())
                runCurrent()
                awaitState(this) { !it.isRefreshing && it.error == AppError.Network }

                viewModel.onEvent(NotesEvent.Refresh)
                runCurrent()

                assertEquals(
                    NotesUiState(isRefreshing = true, error = AppError.Network),
                    awaitState(this) { it.isRefreshing && it.error == AppError.Network },
                )
                repository.refreshResults.send(ApiResult.Failure(AppError.Network))
                runCurrent()
                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong())
                runCurrent()
                awaitState(this) { !it.isRefreshing && it.error == AppError.Network }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun successfulRefreshClearsExistingError() =
        runTest(mainDispatcher) {
            val repository = ViewModelNotesRepository()
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.state.test {
                awaitState(this) { it.isRefreshing }
                repository.refreshResults.send(ApiResult.Failure(AppError.Network))
                runCurrent()
                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong())
                runCurrent()
                awaitState(this) { !it.isRefreshing && it.error == AppError.Network }

                viewModel.onEvent(NotesEvent.Refresh)
                runCurrent()
                awaitState(this) { it.isRefreshing && it.error == AppError.Network }
                repository.refreshResults.send(ApiResult.Success(Unit))
                runCurrent()
                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong())
                runCurrent()

                assertEquals(NotesUiState(), awaitState(this) { !it.isRefreshing && it.error == null })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repeatedRefreshWhileAlreadyRefreshingIsCoalesced() =
        runTest(mainDispatcher) {
            val repository = ViewModelNotesRepository()
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.state.test {
                awaitState(this) { it.isRefreshing }

                viewModel.onEvent(NotesEvent.Refresh)
                viewModel.onEvent(NotesEvent.Refresh)

                assertEquals(1, repository.refreshCalls)
                repository.refreshResults.send(ApiResult.Success(Unit))
                runCurrent()
                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong())
                runCurrent()
                awaitState(this) { !it.isRefreshing }
                assertEquals(1, repository.refreshCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addAndDeleteFailuresStillReportErrors() =
        runTest(mainDispatcher) {
            val repository = ViewModelNotesRepository()
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.state.test {
                awaitState(this) { it.isRefreshing }
                repository.refreshResults.send(ApiResult.Success(Unit))
                runCurrent()
                advanceTimeBy(UI_REFRESH_MINIMUM_VISIBLE_MILLIS.toLong())
                runCurrent()
                awaitState(this) { !it.isRefreshing }

                repository.addResult = ApiResult.Failure(AppError.Validation(400))
                viewModel.onEvent(NotesEvent.Add("hello"))
                runCurrent()
                awaitState(this) { it.error == AppError.Validation(400) }

                repository.deleteResult = ApiResult.Failure(AppError.Timeout)
                viewModel.onEvent(NotesEvent.Delete(1))
                runCurrent()
                awaitState(this) { it.error == AppError.Timeout }
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun viewModel(repository: ViewModelNotesRepository): NotesViewModel =
        NotesViewModel(
            repository = repository,
            addNote = AddNoteUseCase(repository),
        )

    private suspend fun awaitState(
        turbine: ReceiveTurbine<NotesUiState>,
        predicate: (NotesUiState) -> Boolean,
    ): NotesUiState {
        repeat(10) {
            val item = turbine.awaitItem()
            if (predicate(item)) return item
        }
        error("No matching NotesUiState received")
    }
}
