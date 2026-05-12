package org.example.project.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.example.project.models.UserPrincipal
import java.util.*

object JwtConfig {
    private const val SECRET = "pokemon-opencode-secret-key-change-in-production"
    private const val ISSUER = "prueba-opencode"
    private const val VALIDITY_HOURS = 24L

    private val algorithm = Algorithm.HMAC256(SECRET)

    fun createToken(user: UserPrincipal): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", user.id)
            .withClaim("username", user.username)
            .withClaim("pokemonTypeId", user.pokemonTypeId ?: -1)
            .withClaim("role", user.role)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_HOURS * 3600 * 1000))
            .sign(algorithm)
    }

    fun verifyToken(token: String): Map<String, Any>? {
        return try {
            val verifier = JWT.require(algorithm).withIssuer(ISSUER).build()
            val decoded = verifier.verify(token)
            mapOf(
                "userId" to (decoded.getClaim("userId").asInt() ?: 0),
                "username" to (decoded.getClaim("username").asString() ?: ""),
                "pokemonTypeId" to (decoded.getClaim("pokemonTypeId").asInt() ?: -1),
                "role" to (decoded.getClaim("role").asString() ?: "user")
            )
        } catch (e: Exception) {
            null
        }
    }
}
