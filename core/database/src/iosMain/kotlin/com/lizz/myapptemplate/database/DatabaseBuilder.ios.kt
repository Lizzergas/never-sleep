package com.lizz.myapptemplate.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory: NSURL? =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    val dbFilePath = requireNotNull(documentDirectory).path + "/$DATABASE_FILE_NAME"
    return Room.databaseBuilder<AppDatabase>(dbFilePath)
}

actual val databasePlatformKoinModule: Module =
    module {
        single<AppDatabase> { buildAppDatabase(databaseBuilder()) }
        single<NoteDao> { get<AppDatabase>().noteDao() }
    }
