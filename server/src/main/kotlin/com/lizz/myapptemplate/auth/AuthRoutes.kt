package com.lizz.myapptemplate.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.lizz.myapptemplate.model.AuthRequest
import com.lizz.myapptemplate.model.RefreshRequest
import com.lizz.myapptemplate.model.UserDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

const val JWT_AUTH = "auth-jwt"
private const val MIN_PASSWORD_LENGTH = 8

fun Application.installJwtAuth(config: JwtConfig) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = config.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.secret))
                    .withIssuer(config.issuer)
                    .withAudience(config.audience)
                    .build(),
            )
            validate { credential ->
                val userId = credential.payload.getClaim(AuthService.USER_ID_CLAIM).asString()
                if (userId.isNullOrBlank()) null else JWTPrincipal(credential.payload)
            }
        }
    }
}

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            val request = call.receive<AuthRequest>()
            if (!request.email.contains("@") || request.password.length < MIN_PASSWORD_LENGTH) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    mapOf("error" to "valid email and a password of at least $MIN_PASSWORD_LENGTH characters required"),
                )
                return@post
            }
            when (val result = authService.register(request.email, request.password)) {
                is AuthResult.Success -> call.respond(result.tokens)
                AuthResult.EmailTaken ->
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "email already registered"))
                AuthResult.InvalidCredentials ->
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
            }
        }
        post("/login") {
            val request = call.receive<AuthRequest>()
            when (val result = authService.login(request.email, request.password)) {
                is AuthResult.Success -> call.respond(result.tokens)
                else -> call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
            }
        }
        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            when (val result = authService.refresh(request.refreshToken)) {
                is AuthResult.Success -> call.respond(result.tokens)
                else -> call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid refresh token"))
            }
        }
    }
}

fun Route.protectedRoutes(users: UserRepository) {
    authenticate(JWT_AUTH) {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim(AuthService.USER_ID_CLAIM)?.asString()
            val user = userId?.let(users::findById)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unknown user"))
            } else {
                call.respond(UserDto(id = user.id, email = user.email))
            }
        }
    }
}
