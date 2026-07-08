package com.lizz.neversleep.settings

import app.cash.turbine.test
import com.lizz.neversleep.designsystem.ThemeMode
import com.lizz.neversleep.settings.domain.SettingsRepository
import com.lizz.neversleep.settings.presentation.SettingsEvent
import com.lizz.neversleep.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** ViewModel tested against a fake of the domain interface — no DataStore needed. */
private class FakeSettingsRepository : SettingsRepository {
    private val mode = MutableStateFlow(ThemeMode.System)
    override val themeMode: Flow<ThemeMode> = mode

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun themeEventUpdatesState() =
        runTest {
            val viewModel = SettingsViewModel(FakeSettingsRepository())

            viewModel.state.test {
                assertEquals(ThemeMode.System, awaitItem().themeMode)

                viewModel.onEvent(SettingsEvent.SetThemeMode(ThemeMode.Dark))

                assertEquals(ThemeMode.Dark, awaitItem().themeMode)
            }
        }
}
