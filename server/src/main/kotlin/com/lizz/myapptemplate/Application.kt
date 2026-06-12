package com.lizz.myapptemplate

import com.lizz.myapptemplate.auth.AuthService
import com.lizz.myapptemplate.auth.InMemoryUserRepository
import com.lizz.myapptemplate.auth.JwtConfig
import com.lizz.myapptemplate.auth.UserRepository
import com.lizz.myapptemplate.auth.authRoutes
import com.lizz.myapptemplate.auth.installJwtAuth
import com.lizz.myapptemplate.auth.protectedRoutes
import com.lizz.myapptemplate.model.HelloResponse
import com.lizz.myapptemplate.model.Item
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@OptIn(ExperimentalTime::class)
fun Application.module(
    jwtConfig: JwtConfig = JwtConfig.fromEnvironment(),
    userRepository: UserRepository = InMemoryUserRepository(),
) {
    val authService = AuthService(userRepository, jwtConfig)

    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "internal error")),
            )
        }
    }
    installJwtAuth(jwtConfig)

    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        // Sample API consumed by the showcase network demo. The DTOs live in
        // core:model and are shared with all clients — change them in one
        // place and both sides stay in sync.
        route("/api") {
            get("/hello") {
                call.respond(
                    HelloResponse(
                        message = sayHello("MyAppTemplate client"),
                        serverTime = Clock.System.now().toString(),
                    ),
                )
            }
            get("/items") {
                call.respond(sampleItems)
            }
            authRoutes(authService)
            protectedRoutes(userRepository)
        }
    }
}

private val sampleItems =
    listOf(
        Item(id = 1, title = "Shared DTOs", description = "This list was decoded from core:model types"),
        Item(id = 2, title = "Content negotiation", description = "Server serializes with kotlinx.serialization"),
        Item(id = 3, title = "Typed errors", description = "Clients map failures to AppError"),
    )
