package org.example.project.models

import kotlinx.serialization.Serializable

@Serializable
data class GreetingRequest(
    val name: String
)

@Serializable
data class GreetingResponse(
    val greeting: String
)
