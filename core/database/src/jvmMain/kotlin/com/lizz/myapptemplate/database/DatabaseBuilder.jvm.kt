package com.lizz.myapptemplate.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dir = File(System.getProperty("user.home"), ".myapptemplate")
    dir.mkdirs()
    return Room.databaseBuilder<AppDatabase>(File(dir, DATABASE_FILE_NAME).absolutePath)
}

actual val databasePlatformKoinModule: Module =
    module {
        single<AppDatabase> { buildAppDatabase(databaseBuilder()) }
        single<NoteDao> { get<AppDatabase>().noteDao() }
    }
