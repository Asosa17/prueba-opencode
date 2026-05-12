package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ApiResponse
import org.example.project.models.LoginRequest
import org.example.project.models.LoginResponse
import org.example.project.models.UserInfo
import org.example.project.plugins.JwtConfig
import org.example.project.services.AuthService

fun Route.authRoutes() {
    val authService = AuthService()

    route("/api/auth") {
        post("/login") {
            val request = try {
                call.receive<LoginRequest>()
            } catch (e: Exception) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error("Invalid request body")
                )
            }

            val result = authService.findByUsernameWithHash(request.username)
            if (result == null) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.error("Invalid credentials")
                )
            }

            val (user, hash) = result
            if (!authService.verifyPassword(request.password, hash)) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.error("Invalid credentials")
                )
            }

            val token = JwtConfig.createToken(user)
            val response = LoginResponse(
                token = token,
                user = UserInfo(
                    id = user.id,
                    username = user.username,
                    pokemonTypeId = user.pokemonTypeId,
                    role = user.role
                )
            )
            call.respond(ApiResponse.ok(response, message = "Login successful"))
        }
    }
}
