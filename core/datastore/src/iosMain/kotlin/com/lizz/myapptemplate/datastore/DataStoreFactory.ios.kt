package com.lizz.myapptemplate.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.lizz.myapptemplate.common.documentsFilePath
import org.koin.core.module.Module
import org.koin.dsl.module

fun createIosPreferencesDataStore(): DataStore<Preferences> =
    createPreferencesDataStore { documentsFilePath(PREFERENCES_FILE_NAME) }

// Process-wide: DataStore forbids two instances on the same file, and Koin
// may be stopped/restarted within one process.
private val iosDataStore: DataStore<Preferences> by lazy { createIosPreferencesDataStore() }

actual val datastorePlatformKoinModule: Module = module {
    single<DataStore<Preferences>> { iosDataStore }
}
