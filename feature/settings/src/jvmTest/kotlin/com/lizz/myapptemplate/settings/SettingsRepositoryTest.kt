package com.lizz.myapptemplate.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lizz.myapptemplate.designsystem.ThemeMode
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

class SettingsRepositoryTest {

    @Test
    fun themeModeDefaultsToSystem() = runTest {
        val file = Files.createTempDirectory("ds").resolve("t.preferences_pb").toString()
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val repository = SettingsRepository(
            PreferenceDataStoreFactory.createWithPath(scope = scope) { file.toPath() },
        )

        assertEquals(ThemeMode.System, repository.themeMode.first())
        scope.cancel()
    }

    @Test
    fun themeModePersistsAcrossRestart() = runTest {
        val file = Files.createTempDirectory("ds").resolve("t.preferences_pb").toString()

        // First "process": write Dark
        val scope1 = CoroutineScope(Job() + Dispatchers.Default)
        val repository1 = SettingsRepository(
            PreferenceDataStoreFactory.createWithPath(scope = scope1) { file.toPath() },
        )
        repository1.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository1.themeMode.first())
        scope1.cancel()

        // Simulated restart: a fresh DataStore over the same file
        val scope2 = CoroutineScope(Job() + Dispatchers.Default)
        val repository2 = SettingsRepository(
            PreferenceDataStoreFactory.createWithPath(scope = scope2) { file.toPath() },
        )
        assertEquals(ThemeMode.Dark, repository2.themeMode.first())
        scope2.cancel()
    }
}
