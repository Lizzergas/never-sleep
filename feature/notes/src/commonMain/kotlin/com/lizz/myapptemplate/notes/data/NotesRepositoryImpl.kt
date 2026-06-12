package com.lizz.myapptemplate.notes.data

import com.lizz.myapptemplate.database.NoteDao
import com.lizz.myapptemplate.model.ApiResult
import com.lizz.myapptemplate.model.CreateNoteRequest
import com.lizz.myapptemplate.model.NoteDto
import com.lizz.myapptemplate.model.map
import com.lizz.myapptemplate.model.onSuccess
import com.lizz.myapptemplate.network.safeApiCall
import com.lizz.myapptemplate.network.safeGet
import com.lizz.myapptemplate.notes.domain.Note
import com.lizz.myapptemplate.notes.domain.NotesRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first: Room is the source of truth. Reads observe the cache;
 * refresh replaces it with server state; writes hit the server (the app
 * HttpClient carries auth automatically) and update the cache on success.
 */
class NotesRepositoryImpl(
    private val httpClient: HttpClient,
    private val noteDao: NoteDao,
) : NotesRepository {
    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): ApiResult<Unit> =
        httpClient
            .safeGet<List<NoteDto>>("/api/notes")
            .onSuccess { notes -> noteDao.replaceAll(notes.map { it.toEntity() }) }
            .map { }

    override suspend fun add(text: String): ApiResult<Note> =
        safeApiCall<NoteDto> {
            httpClient.post("/api/notes") {
                contentType(ContentType.Application.Json)
                setBody(CreateNoteRequest(text))
            }
        }.onSuccess { note -> noteDao.upsert(listOf(note.toEntity())) }
            .map { it.toDomain() }

    override suspend fun delete(id: Long): ApiResult<Unit> =
        safeApiCall<Unit> {
            httpClient.delete("/api/notes/$id")
        }.onSuccess { noteDao.deleteById(id) }
            .map { }
}
