package com.lizz.neversleep.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun databaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
    return Room.databaseBuilder<AppDatabase>(context, dbFile.absolutePath)
}

// Process-wide: a second Room instance on the same file risks lock errors,
// and Koin may be stopped/restarted within one process.
private val instanceLock = Any()
private var instance: AppDatabase? = null

actual val databasePlatformKoinModule: Module = module {
    single<AppDatabase> {
        synchronized(instanceLock) {
            instance ?: buildAppDatabase(databaseBuilder(androidContext().applicationContext))
                .also { instance = it }
        }
    }
    single<NoteDao> { get<AppDatabase>().noteDao() }
}
