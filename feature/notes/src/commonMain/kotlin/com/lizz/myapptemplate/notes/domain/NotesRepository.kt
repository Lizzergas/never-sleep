package com.lizz.myapptemplate.notes.domain

import com.lizz.myapptemplate.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Notes are cached for offline reads; writes are server-authoritative and
 * update the cache only after server success. Implemented in data/.
 */
interface NotesRepository {
    /** The local cache — emits on every change, works offline. */
    fun observeNotes(): Flow<List<Note>>

    /** Pulls the server state into the cache. */
    suspend fun refresh(): ApiResult<Unit>

    suspend fun add(text: String): ApiResult<Note>

    suspend fun delete(id: Long): ApiResult<Unit>
}
