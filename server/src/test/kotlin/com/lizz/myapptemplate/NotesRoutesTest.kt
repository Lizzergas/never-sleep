package com.lizz.myapptemplate

import com.lizz.myapptemplate.model.AuthRequest
import com.lizz.myapptemplate.model.CreateNoteRequest
import com.lizz.myapptemplate.model.NoteDto
import com.lizz.myapptemplate.model.TokenPair
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotesRoutesTest {
    private fun ApplicationTestBuilder.jsonClient(): HttpClient {
        application { module() }
        return createClient {
            install(ContentNegotiation) { json() }
        }
    }

    private suspend fun HttpClient.registerAndGetToken(email: String): String {
        val tokens: TokenPair = post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, "password123"))
        }.body()
        return tokens.accessToken
    }

    @Test
    fun notesRequireAuthentication() =
        testApplication {
            val client = jsonClient()

            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/notes").status)
        }

    @Test
    fun createListAndDeleteNote() =
        testApplication {
            val client = jsonClient()
            val token = client.registerAndGetToken("notes@test.dev")

            val created: NoteDto = client
                .post("/api/notes") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(CreateNoteRequest("hello notes"))
                }.body()
            assertEquals("hello notes", created.text)

            val listed: List<NoteDto> = client.get("/api/notes") { bearerAuth(token) }.body()
            assertEquals(listOf(created.id), listed.map { it.id })

            assertEquals(
                HttpStatusCode.NoContent,
                client.delete("/api/notes/${created.id}") { bearerAuth(token) }.status,
            )
            val after: List<NoteDto> = client.get("/api/notes") { bearerAuth(token) }.body()
            assertTrue(after.isEmpty())
        }

    @Test
    fun blankNoteIsRejected() =
        testApplication {
            val client = jsonClient()
            val token = client.registerAndGetToken("blank@test.dev")

            val response = client.post("/api/notes") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(CreateNoteRequest("   "))
            }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }

    @Test
    fun notesAreIsolatedPerUser() =
        testApplication {
            val client = jsonClient()
            val tokenA = client.registerAndGetToken("a@test.dev")
            val tokenB = client.registerAndGetToken("b@test.dev")

            client.post("/api/notes") {
                bearerAuth(tokenA)
                contentType(ContentType.Application.Json)
                setBody(CreateNoteRequest("a's secret"))
            }

            val bNotes: List<NoteDto> = client.get("/api/notes") { bearerAuth(tokenB) }.body()
            assertTrue(bNotes.isEmpty())
        }
}
