package com.lizz.myapptemplate.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

fun createJvmPreferencesDataStore(): DataStore<Preferences> =
    createPreferencesDataStore {
        val dir = File(System.getProperty("user.home"), ".myapptemplate")
        dir.mkdirs()
        File(dir, PREFERENCES_FILE_NAME).absolutePath
    }

// Process-wide: DataStore forbids two instances on the same file, and Koin
// may be stopped/restarted within one process (tests, dev tooling).
private val jvmDataStore: DataStore<Preferences> by lazy { createJvmPreferencesDataStore() }

actual val datastorePlatformKoinModule: Module =
    module {
        single<DataStore<Preferences>> { jvmDataStore }
    }
