package org.example.project.models

import kotlinx.serialization.Serializable

enum class Role(val level: Int) {
    NOVICE(1),
    EXPERT(2),
    LEGEND(3);

    companion object {
        fun fromString(value: String): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPERT
    }
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: Int,
    val username: String,
    val pokemonTypeId: Int? = null,
    val role: String = "user"
)

data class UserPrincipal(
    val id: Int,
    val username: String,
    val pokemonTypeId: Int?,
    val role: String
)
