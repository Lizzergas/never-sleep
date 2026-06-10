package com.lizz.myapptemplate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Overrides the app's DataStore with a throwaway temp-file instance so tests
 * never touch (or collide on) the real user preferences file. Cancel [scope]
 * in teardown to release the file.
 */
fun testDataStoreModule(scope: CoroutineScope): Module = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(scope = scope) {
            Files.createTempDirectory("test-ds").resolve("test.preferences_pb").toString().toPath()
        }
    }
}
