package com.lizz.myapptemplate.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosPreferencesDataStore(): DataStore<Preferences> = createPreferencesDataStore {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    requireNotNull(documentDirectory).path + "/$PREFERENCES_FILE_NAME"
}

// Process-wide: DataStore forbids two instances on the same file, and Koin
// may be stopped/restarted within one process.
private val iosDataStore: DataStore<Preferences> by lazy { createIosPreferencesDataStore() }

actual val datastorePlatformKoinModule: Module = module {
    single<DataStore<Preferences>> { iosDataStore }
}
