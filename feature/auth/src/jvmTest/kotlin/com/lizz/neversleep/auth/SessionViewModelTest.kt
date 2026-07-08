package com.lizz.neversleep.auth

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.lizz.neversleep.auth.domain.SessionRepository
import com.lizz.neversleep.auth.domain.SessionState
import com.lizz.neversleep.auth.domain.User
import com.lizz.neversleep.auth.domain.ValidateCredentialsUseCase
import com.lizz.neversleep.auth.presentation.AccountEvent
import com.lizz.neversleep.auth.presentation.AccountUiState
import com.lizz.neversleep.auth.presentation.SessionViewModel
import com.lizz.neversleep.model.ApiResult
import com.lizz.neversleep.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class ViewModelSessionRepository : SessionRepository {
    private val session = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    override val sessionState: StateFlow<SessionState> = session

    val loginResults = Channel<ApiResult<User>>(Channel.UNLIMITED)
    val registerResults = Channel<ApiResult<User>>(Channel.UNLIMITED)
    var loginCalls = 0
        private set
    var registerCalls = 0
        private set

    override suspend fun restore() = Unit

    override suspend fun login(
        email: String,
        password: String,
    ): ApiResult<User> {
        loginCalls += 1
        return loginResults.receive().also { result ->
            if (result is ApiResult.Success) {
                session.value = SessionState.LoggedIn(result.data)
            }
        }
    }

    override suspend fun register(
        email: String,
        password: String,
    ): ApiResult<User> {
        registerCalls += 1
        return registerResults.receive().also { result ->
            if (result is ApiResult.Success) {
                session.value = SessionState.LoggedIn(result.data)
            }
        }
    }

    override suspend fun logout() {
        session.value = SessionState.LoggedOut
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun failedLoginKeepsPreviousErrorVisibleWhileSubmitting() =
        runTest {
            val repository = ViewModelSessionRepository()
            val viewModel = viewModel(repository)

            viewModel.state.test {
                awaitState(this) { it.session == SessionState.LoggedOut }

                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))
                repository.loginResults.send(ApiResult.Failure(AppError.Network))
                awaitState(this) { !it.isSubmitting && it.error == AppError.Network }

                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))

                assertEquals(
                    AccountUiState(
                        session = SessionState.LoggedOut,
                        isSubmitting = true,
                        error = AppError.Network,
                    ),
                    awaitState(this) { it.isSubmitting && it.error == AppError.Network },
                )
                repository.loginResults.send(ApiResult.Failure(AppError.Network))
                awaitState(this) { !it.isSubmitting && it.error == AppError.Network }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun successfulLoginClearsExistingError() =
        runTest {
            val repository = ViewModelSessionRepository()
            val viewModel = viewModel(repository)

            viewModel.state.test {
                awaitState(this) { it.session == SessionState.LoggedOut }

                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))
                repository.loginResults.send(ApiResult.Failure(AppError.Network))
                awaitState(this) { !it.isSubmitting && it.error == AppError.Network }

                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))
                awaitState(this) { it.isSubmitting && it.error == AppError.Network }
                repository.loginResults.send(ApiResult.Success(USER))

                val signedIn = awaitState(this) { !it.isSubmitting && it.session is SessionState.LoggedIn }
                assertNull(signedIn.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repeatedLoginWhileSubmittingIsCoalesced() =
        runTest {
            val repository = ViewModelSessionRepository()
            val viewModel = viewModel(repository)

            viewModel.state.test {
                awaitState(this) { it.session == SessionState.LoggedOut }

                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))
                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))
                viewModel.onEvent(AccountEvent.Login(EMAIL, PASSWORD))

                assertEquals(1, repository.loginCalls)
                repository.loginResults.send(ApiResult.Failure(AppError.Network))
                awaitState(this) { !it.isSubmitting && it.error == AppError.Network }
                assertEquals(1, repository.loginCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repeatedRegisterWhileSubmittingIsCoalesced() =
        runTest {
            val repository = ViewModelSessionRepository()
            val viewModel = viewModel(repository)

            viewModel.state.test {
                awaitState(this) { it.session == SessionState.LoggedOut }

                viewModel.onEvent(AccountEvent.Register(EMAIL, PASSWORD))
                viewModel.onEvent(AccountEvent.Register(EMAIL, PASSWORD))

                assertEquals(1, repository.registerCalls)
                repository.registerResults.send(ApiResult.Failure(AppError.Network))
                awaitState(this) { !it.isSubmitting && it.error == AppError.Network }
                assertEquals(1, repository.registerCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun viewModel(repository: ViewModelSessionRepository): SessionViewModel =
        SessionViewModel(
            repository = repository,
            validateCredentials = ValidateCredentialsUseCase(),
        )

    private suspend fun awaitState(
        turbine: ReceiveTurbine<AccountUiState>,
        predicate: (AccountUiState) -> Boolean,
    ): AccountUiState {
        repeat(10) {
            val item = turbine.awaitItem()
            if (predicate(item)) return item
        }
        error("No matching AccountUiState received")
    }

    private companion object {
        const val EMAIL = "user@test.dev"
        const val PASSWORD = "password123"
        val USER = User("u1", EMAIL)
    }
}
