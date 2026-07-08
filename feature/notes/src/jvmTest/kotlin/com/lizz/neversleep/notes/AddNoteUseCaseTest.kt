package com.lizz.neversleep.notes

import com.lizz.neversleep.model.ApiResult
import com.lizz.neversleep.notes.domain.AddNoteUseCase
import com.lizz.neversleep.notes.domain.Note
import com.lizz.neversleep.notes.domain.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
private class FakeNotesRepository : NotesRepository {
    val added = mutableListOf<String>()

    override fun observeNotes(): Flow<List<Note>> = MutableStateFlow(emptyList())

    override suspend fun refresh(): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun add(text: String): ApiResult<Note> {
        added += text
        return ApiResult.Success(Note(1, text, Instant.fromEpochMilliseconds(0)))
    }

    override suspend fun delete(id: Long): ApiResult<Unit> = ApiResult.Success(Unit)
}

class AddNoteUseCaseTest {
    @Test
    fun trimsAndAddsValidText() =
        runTest {
            val repository = FakeNotesRepository()
            val addNote = AddNoteUseCase(repository)

            val result = addNote("  hello  ")

            assertIs<ApiResult.Success<*>>(result)
            assertEquals(listOf("hello"), repository.added)
        }

    @Test
    fun rejectsBlankAndOversizedTextWithoutTouchingTheRepository() =
        runTest {
            val repository = FakeNotesRepository()
            val addNote = AddNoteUseCase(repository)

            assertIs<ApiResult.Failure>(addNote("   "))
            assertIs<ApiResult.Failure>(addNote("x".repeat(501)))
            assertEquals(emptyList(), repository.added)
        }
}
