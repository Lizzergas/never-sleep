package com.lizz.myapptemplate.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.lizz.myapptemplate.designsystem.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val storeScope = CoroutineScope(Job() + Dispatchers.Default)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        storeScope.cancel()
    }

    @Test
    fun themeChangeEmitsNewState() =
        runTest {
            val file = Files.createTempDirectory("ds").resolve("t.preferences_pb").toString()
            val repository =
                SettingsRepository(
                    PreferenceDataStoreFactory.createWithPath(scope = storeScope) { file.toPath() },
                )
            val viewModel = SettingsViewModel(repository)

            viewModel.themeMode.test {
                assertEquals(ThemeMode.System, awaitItem())

                viewModel.setThemeMode(ThemeMode.Dark)

                assertEquals(ThemeMode.Dark, awaitItem())
            }
        }
}
