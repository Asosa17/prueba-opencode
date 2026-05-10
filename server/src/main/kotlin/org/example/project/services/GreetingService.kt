package org.example.project.services

import org.example.project.models.GreetingResponse

class GreetingService {

    fun greet(name: String): GreetingResponse {
        val greeting = "Hello, $name! Welcome to Ktor API."
        return GreetingResponse(greeting = greeting)
    }
}
