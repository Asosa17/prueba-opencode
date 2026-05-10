package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ApiResponse
import org.example.project.models.GreetingRequest
import org.example.project.services.GreetingService

fun Route.greetingRoutes() {
    val service = GreetingService()

    route("/api/greeting") {
        get {
            call.respond(
                ApiResponse.ok(
                    service.greet("World"),
                    message = "Default greeting"
                )
            )
        }

        post {
            val request = call.receive<GreetingRequest>()
            call.respond(
                ApiResponse.ok(
                    service.greet(request.name),
                    message = "Custom greeting"
                )
            )
        }
    }
}
