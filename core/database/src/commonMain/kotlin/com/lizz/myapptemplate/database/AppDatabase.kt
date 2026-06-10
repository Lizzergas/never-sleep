package com.lizz.myapptemplate.database

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import kotlinx.coroutines.flow.Flow

/**
 * Sample entity proving the Room 3 KMP setup end to end. Replace with your
 * real schema — the wiring (driver, per-platform builders, KSP) stays.
 */
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
)

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note)

    @Query("SELECT * FROM Note ORDER BY id DESC")
    fun observeAll(): Flow<List<Note>>
}

@Database(entities = [Note::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

// The Room compiler generates the `actual` implementations per platform.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
