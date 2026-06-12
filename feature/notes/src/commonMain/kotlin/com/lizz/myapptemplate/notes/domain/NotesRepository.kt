package com.lizz.myapptemplate.notes.domain

import com.lizz.myapptemplate.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first notes: the local cache is the source of truth for reads;
 * writes go to the server and update the cache. Implemented in data/.
 */
interface NotesRepository {
    /** The local cache — emits on every change, works offline. */
    fun observeNotes(): Flow<List<Note>>

    /** Pulls the server state into the cache. */
    suspend fun refresh(): ApiResult<Unit>

    suspend fun add(text: String): ApiResult<Note>

    suspend fun delete(id: Long): ApiResult<Unit>
}
