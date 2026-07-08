package com.lizz.neversleep.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * [DataStore.data] THROWS on unreadable/corrupted preference files (killed
 * mid-write, full disk). Repositories must read through this so a bad file
 * degrades to defaults instead of crashing every collector — including the
 * app's start-route gate, which would otherwise crash at every launch.
 */
fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { error ->
        Logger.w(tag = "DataStore") { "Preferences unreadable, using defaults: $error" }
        emit(emptyPreferences())
    }
