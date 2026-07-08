package com.lizz.neversleep.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun createPreferencesDataStore(context: Context): DataStore<Preferences> =
    createPreferencesDataStore {
        context.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
    }

// Process-wide: DataStore forbids two instances on the same file, and Koin
// may be stopped/restarted within one process.
private val instanceLock = Any()
private var instance: DataStore<Preferences>? = null

actual val datastorePlatformKoinModule: Module = module {
    single<DataStore<Preferences>> {
        synchronized(instanceLock) {
            instance ?: createPreferencesDataStore(androidContext().applicationContext)
                .also { instance = it }
        }
    }
}
