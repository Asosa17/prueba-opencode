package org.example.project.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ApiResponse

fun Route.healthRoutes() {
    route("/api") {
        get("/health") {
            call.respond(
                ApiResponse.ok(
                    mapOf("status" to "UP", "timestamp" to System.currentTimeMillis()),
                    message = "Server is healthy"
                )
            )
        }
    }
}
