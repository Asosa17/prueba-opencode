package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ApiResponse
import org.example.project.models.FilterMode
import org.example.project.models.Role
import org.example.project.plugins.PokemonPrincipal
import org.example.project.services.PokemonService

private fun mapRoleToFilterMode(role: String): FilterMode = when (Role.fromString(role)) {
    Role.LEGEND -> FilterMode.NONE
    Role.NOVICE -> FilterMode.SLOT_1
    Role.EXPERT -> FilterMode.ANY_SLOT
}

fun Route.pokemonRoutes() {
    val service = PokemonService()

    route("/api") {
        authenticate("auth-jwt") {
            get("/pokemon") {
                val principal = call.principal<PokemonPrincipal>()!!
                val filterMode = mapRoleToFilterMode(principal.role)
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val result = service.listPokemon(page, pageSize.coerceIn(1, 100), filterMode, principal.pokemonTypeId)
                call.respond(ApiResponse.ok(result, message = "Pokémon list retrieved"))
            }

            get("/pokemon/search") {
                val principal = call.principal<PokemonPrincipal>()!!
                val filterMode = mapRoleToFilterMode(principal.role)
                val q = call.request.queryParameters["q"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.error("Query parameter 'q' is required")
                )
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val result = service.searchPokemon(q, page, pageSize.coerceIn(1, 100), filterMode, principal.pokemonTypeId)
                call.respond(ApiResponse.ok(result, message = "Search results"))
            }

            get("/pokemon/{id}") {
                val principal = call.principal<PokemonPrincipal>()!!
                val filterMode = mapRoleToFilterMode(principal.role)
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid ID"))
                }
                val pokemon = service.getPokemonById(id, filterMode, principal.pokemonTypeId)
                if (pokemon == null) {
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.error("Pokémon not found"))
                }
                call.respond(ApiResponse.ok(pokemon, message = "Pokémon detail retrieved"))
            }

            get("/types") {
                val types = service.listTypes()
                call.respond(ApiResponse.ok(types, message = "Types retrieved"))
            }

            get("/abilities") {
                val abilities = service.listAbilities()
                call.respond(ApiResponse.ok(abilities, message = "Abilities retrieved"))
            }
        }
    }
}
