package com.lizz.myapptemplate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room3.Room
import com.lizz.myapptemplate.connectivity.ConnectivityMonitor
import com.lizz.myapptemplate.database.AppDatabase
import com.lizz.myapptemplate.database.NoteDao
import com.lizz.myapptemplate.database.buildAppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import java.nio.file.Files

/**
 * Overrides the app's DataStore with a throwaway temp-file instance so tests
 * never touch (or collide on) the real user preferences file. Cancel [scope]
 * in teardown to release the file.
 */
fun testDataStoreModule(scope: CoroutineScope): Module =
    module {
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(scope = scope) {
                Files
                    .createTempDirectory("test-ds")
                    .resolve("test.preferences_pb")
                    .toString()
                    .toPath()
            }
        }
    }

/** Overrides the app database with a throwaway temp-file instance. */
fun testDatabaseModule(): Module =
    module {
        single<AppDatabase> {
            val file = Files.createTempDirectory("test-db").resolve("test.db").toString()
            buildAppDatabase(Room.databaseBuilder<AppDatabase>(file))
        }
        single<NoteDao> { get<AppDatabase>().noteDao() }
    }

/** Hand-controlled connectivity for testing offline UI and retry-on-reconnect. */
class FakeConnectivityMonitor(
    initiallyOnline: Boolean = true,
) : ConnectivityMonitor {
    val state = MutableStateFlow(initiallyOnline)
    override val isOnline: Flow<Boolean> = state
}

fun testConnectivityModule(monitor: FakeConnectivityMonitor): Module =
    module {
        single<ConnectivityMonitor> { monitor }
    }
