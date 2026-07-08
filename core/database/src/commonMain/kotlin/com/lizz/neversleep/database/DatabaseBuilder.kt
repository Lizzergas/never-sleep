package com.lizz.neversleep.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module

internal const val DATABASE_FILE_NAME = "app.db"

/** Applies the template defaults to a platform-created builder. */
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // Template default for a local CACHE: schema changes drop the data.
        // Replace with real Migrations once the database stores user data.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

/**
 * Provides AppDatabase (and its DAOs) as singletons. Platform actuals choose
 * the database file location.
 */
expect val databasePlatformKoinModule: Module
