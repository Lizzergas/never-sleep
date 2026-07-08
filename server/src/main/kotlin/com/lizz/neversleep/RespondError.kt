package com.lizz.neversleep

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * The one canonical error body shape ({"error": "..."}). Every route uses
 * this so the contract can evolve (e.g. adding a machine-readable code) in
 * one place.
 */
suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String,
) {
    respond(status, mapOf("error" to message))
}
