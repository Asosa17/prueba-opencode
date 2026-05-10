package org.example.project.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.example.project.routes.greetingRoutes
import org.example.project.routes.healthRoutes

fun Application.configureRouting() {
    routing {
        greetingRoutes()
        healthRoutes()
    }
}
