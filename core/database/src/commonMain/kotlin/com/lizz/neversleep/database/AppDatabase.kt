package com.lizz.neversleep.database

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Local cache of server notes (feature:notes). The id is the server id.
 * Add your own entities here — the wiring (driver, builders, KSP) stays.
 */
@Entity
data class NoteEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val createdAtEpochMillis: Long,
)

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notes: List<NoteEntity>)

    @Query("DELETE FROM NoteEntity")
    suspend fun clear()

    /** Replaces the whole cache atomically (refresh-from-server). */
    @Transaction
    suspend fun replaceAll(notes: List<NoteEntity>) {
        clear()
        upsert(notes)
    }

    @Query("DELETE FROM NoteEntity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM NoteEntity ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<NoteEntity>>
}

@Database(entities = [NoteEntity::class], version = 2)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

// The Room compiler generates the `actual` implementations per platform.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
