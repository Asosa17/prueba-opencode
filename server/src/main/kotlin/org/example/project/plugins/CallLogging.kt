package org.example.project.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.*

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}
