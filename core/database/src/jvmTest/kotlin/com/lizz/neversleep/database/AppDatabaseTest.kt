package com.lizz.neversleep.database

import androidx.room3.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDatabaseTest {
    @Test
    fun noteRoundTrip() =
        runTest {
            val file = Files.createTempDirectory("db").resolve("test.db").toString()
            val database = buildAppDatabase(Room.databaseBuilder<AppDatabase>(file))

            database.noteDao().upsert(listOf(NoteEntity(id = 1, text = "hello room", createdAtEpochMillis = 0)))
            val notes = database.noteDao().observeAll().first()

            assertEquals(1, notes.size)
            assertEquals("hello room", notes.single().text)
            database.close()
        }
}
