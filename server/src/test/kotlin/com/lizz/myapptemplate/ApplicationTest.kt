package com.lizz.myapptemplate

import com.lizz.myapptemplate.model.HelloResponse
import com.lizz.myapptemplate.model.Item
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun rootRespondsWithGreeting() =
        testApplication {
            application { module() }

            val response = client.get("/")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello, Ktor!", response.bodyAsText())
        }

    @Test
    fun helloEndpointReturnsTypedDto() =
        testApplication {
            application { module() }
            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.get("/api/hello")

            assertEquals(HttpStatusCode.OK, response.status)
            val hello: HelloResponse = response.body()
            assertTrue(hello.message.isNotBlank())
            assertTrue(hello.serverTime.isNotBlank())
        }

    @Test
    fun itemsEndpointReturnsTypedList() =
        testApplication {
            application { module() }
            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.get("/api/items")

            assertEquals(HttpStatusCode.OK, response.status)
            val items: List<Item> = response.body()
            assertTrue(items.isNotEmpty())
        }
}
