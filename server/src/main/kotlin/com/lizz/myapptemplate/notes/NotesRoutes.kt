package com.lizz.myapptemplate.notes

import com.lizz.myapptemplate.auth.AuthService
import com.lizz.myapptemplate.auth.JWT_AUTH
import com.lizz.myapptemplate.model.CreateNoteRequest
import com.lizz.myapptemplate.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val RoutingContext.userId: String?
    get() = call
        .principal<JWTPrincipal>()
        ?.payload
        ?.getClaim(AuthService.USER_ID_CLAIM)
        ?.asString()

fun Route.notesRoutes(store: NotesStore) {
    authenticate(JWT_AUTH) {
        route("/notes") {
            get {
                val userId = userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(store.list(userId))
            }
            post {
                val userId = userId ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<CreateNoteRequest>()
                val text = request.text.trim()
                if (text.isEmpty()) {
                    call.respondError(HttpStatusCode.UnprocessableEntity, "note text required")
                    return@post
                }
                call.respond(store.add(userId, text))
            }
            delete("/{id}") {
                val userId = userId ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respondError(HttpStatusCode.BadRequest, "invalid id")
                    return@delete
                }
                if (store.delete(userId, id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respondError(HttpStatusCode.NotFound, "note not found")
                }
            }
        }
    }
}
