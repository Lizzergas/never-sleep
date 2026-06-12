package com.lizz.myapptemplate.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.lizz.myapptemplate.common.documentsFilePath
import org.koin.core.module.Module
import org.koin.dsl.module

fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(documentsFilePath(DATABASE_FILE_NAME))

// Process-wide: a second Room instance on the same file risks lock errors,
// and Koin may be stopped/restarted within one process.
private val iosDatabase: AppDatabase by lazy { buildAppDatabase(databaseBuilder()) }

actual val databasePlatformKoinModule: Module =
    module {
        single<AppDatabase> { iosDatabase }
        single<NoteDao> { get<AppDatabase>().noteDao() }
    }
