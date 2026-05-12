package org.example.project.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.http.*
import org.example.project.models.ApiResponse

data class PokemonPrincipal(
    val userId: Int,
    val username: String,
    val pokemonTypeId: Int?,
    val role: String
)

fun Application.configureAuthentication() {
    val algorithm = Algorithm.HMAC256("pokemon-opencode-secret-key-change-in-production")

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(algorithm)
                    .withIssuer("prueba-opencode")
                    .build()
            )

            validate { credential ->
                val claims = credential.payload.claims
                val userId = claims["userId"]?.asInt() ?: return@validate null
                val username = claims["username"]?.asString() ?: return@validate null
                val pokemonTypeId = claims["pokemonTypeId"]?.asInt() ?: -1
                val role = claims["role"]?.asString() ?: "user"

                PokemonPrincipal(
                    userId = userId,
                    username = username,
                    pokemonTypeId = if (pokemonTypeId > 0) pokemonTypeId else null,
                    role = role
                )
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.error("Invalid or expired token")
                )
            }
        }
    }
}
