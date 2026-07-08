package com.lizz.neversleep.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.lizz.neversleep.model.AuthRequest
import com.lizz.neversleep.model.RefreshRequest
import com.lizz.neversleep.model.UserDto
import com.lizz.neversleep.respondError
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
                call.respondError(
                    HttpStatusCode.UnprocessableEntity,
                    "valid email and a password of at least $MIN_PASSWORD_LENGTH characters required",
                )
                return@post
            }
            when (val result = authService.register(request.email, request.password)) {
                is AuthResult.Success -> call.respond(result.tokens)
                AuthResult.EmailTaken ->
                    call.respondError(HttpStatusCode.Conflict, "email already registered")

                AuthResult.InvalidCredentials ->
                    call.respondError(HttpStatusCode.Unauthorized, "invalid credentials")
            }
        }
        post("/login") {
            val request = call.receive<AuthRequest>()
            when (val result = authService.login(request.email, request.password)) {
                is AuthResult.Success -> call.respond(result.tokens)
                else -> call.respondError(HttpStatusCode.Unauthorized, "invalid credentials")
            }
        }
        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            when (val result = authService.refresh(request.refreshToken)) {
                is AuthResult.Success -> call.respond(result.tokens)
                else -> call.respondError(HttpStatusCode.Unauthorized, "invalid refresh token")
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
                call.respondError(HttpStatusCode.Unauthorized, "unknown user")
            } else {
                call.respond(UserDto(id = user.id, email = user.email))
            }
        }
    }
}
