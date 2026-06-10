package com.lizz.myapptemplate.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.module.Module

internal const val PREFERENCES_FILE_NAME = "app.preferences_pb"

/** Creates the preferences DataStore at a platform-supplied path. */
fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

/**
 * Provides DataStore<Preferences> as a singleton. Platform actuals choose the
 * storage location (Android files dir, iOS documents, JVM user home).
 */
expect val datastorePlatformKoinModule: Module
