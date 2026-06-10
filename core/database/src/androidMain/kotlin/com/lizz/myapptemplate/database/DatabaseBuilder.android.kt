package com.lizz.myapptemplate.database

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

actual val databasePlatformKoinModule: Module = module {
    single<AppDatabase> { buildAppDatabase(databaseBuilder(androidContext())) }
    single<NoteDao> { get<AppDatabase>().noteDao() }
}
