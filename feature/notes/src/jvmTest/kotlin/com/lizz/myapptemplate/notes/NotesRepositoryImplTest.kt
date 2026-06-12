package com.lizz.myapptemplate.notes

import androidx.room3.Room
import com.lizz.myapptemplate.database.AppDatabase
import com.lizz.myapptemplate.database.buildAppDatabase
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.network.NetworkConfig
import com.lizz.myapptemplate.network.createHttpClient
import com.lizz.myapptemplate.notes.data.NotesRepositoryImpl
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The offline-first contract: MockEngine plays the server, a real Room
 * database (temp file) is the cache. Proves the full mapper chain.
 */
class NotesRepositoryImplTest {
    private val serverUp = AtomicBoolean(true)

    private fun database(): AppDatabase {
        val file = Files.createTempDirectory("notes-db").resolve("test.db").toString()
        return buildAppDatabase(Room.databaseBuilder<AppDatabase>(file))
    }

    private fun repository(database: AppDatabase): NotesRepositoryImpl {
        val engine =
            MockEngine { request ->
                if (!serverUp.get()) throw kotlinx.io.IOException("offline")
                val json = headersOf(HttpHeaders.ContentType, "application/json")
                when {
                    request.method == HttpMethod.Get ->
                        respond(
                            """[{"id":1,"text":"from server","createdAtEpochMillis":1750000000000}]""",
                            HttpStatusCode.OK,
                            json,
                        )

                    request.method == HttpMethod.Post ->
                        respond(
                            """{"id":2,"text":"created","createdAtEpochMillis":1750000001000}""",
                            HttpStatusCode.OK,
                            json,
                        )

                    request.method == HttpMethod.Delete ->
                        respond("", HttpStatusCode.NoContent, json)

                    else -> respond("{}", HttpStatusCode.NotFound, json)
                }
            }
        return NotesRepositoryImpl(createHttpClient(NetworkConfig(), engine), database.noteDao())
    }

    @Test
    fun refreshPullsServerStateIntoCacheThroughTheMapperChain() =
        runBlocking<Unit> {
            val db = database()
            val repository = repository(db)

            val result = repository.refresh()

            assertIs<ApiResult.Success<Unit>>(result)
            val notes = repository.observeNotes().first()
            assertEquals(1, notes.size)
            assertEquals("from server", notes.single().text)
            assertEquals(1_750_000_000_000, notes.single().createdAt.toEpochMilliseconds())
            db.close()
        }

    @Test
    fun addWritesThroughAndCaches() =
        runBlocking<Unit> {
            val db = database()
            val repository = repository(db)

            val added = repository.add("created")

            assertIs<ApiResult.Success<*>>(added)
            assertTrue(repository.observeNotes().first().any { it.text == "created" })
            db.close()
        }

    @Test
    fun cachedNotesSurviveServerOutage() =
        runBlocking<Unit> {
            val db = database()
            val repository = repository(db)
            repository.refresh()

            serverUp.set(false)
            val refreshResult = repository.refresh()

            // Refresh reports the failure, but reads still serve the cache.
            val failure = assertIs<ApiResult.Failure>(refreshResult)
            assertEquals(AppError.Network, failure.error)
            assertEquals(1, repository.observeNotes().first().size)
            db.close()
        }
}
