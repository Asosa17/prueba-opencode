package org.example.project.models

import kotlinx.serialization.Serializable

enum class FilterMode {
    NONE,
    ANY_SLOT,
    SLOT_1
}

@Serializable
data class PokemonSummary(
    val id: Int,
    val name: String,
    val height: Int? = null,
    val weight: Int? = null,
    val baseExperience: Int? = null,
    val sprite: String? = null,
    val types: List<TypeInfo> = emptyList()
)

@Serializable
data class PokemonDetail(
    val id: Int,
    val name: String,
    val height: Int? = null,
    val weight: Int? = null,
    val baseExperience: Int? = null,
    val speciesId: Int? = null,
    val types: List<TypeInfo> = emptyList(),
    val abilities: List<AbilityInfo> = emptyList(),
    val stats: StatsInfo? = null,
    val sprites: SpritesInfo? = null,
    val evolutions: List<EvolutionInfo> = emptyList()
)

@Serializable
data class TypeInfo(
    val id: Int,
    val name: String,
    val slot: Int? = null
)

@Serializable
data class AbilityInfo(
    val id: Int,
    val name: String,
    val isHidden: Boolean = false,
    val slot: Int? = null
)

@Serializable
data class StatsInfo(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val specialAttack: Int = 0,
    val specialDefense: Int = 0,
    val speed: Int = 0
)

@Serializable
data class SpritesInfo(
    val frontDefault: String? = null,
    val frontShiny: String? = null,
    val backDefault: String? = null,
    val backShiny: String? = null,
    val artwork: String? = null
)

@Serializable
data class EvolutionInfo(
    val pokemonId: Int,
    val evolvesFromId: Int? = null,
    val evolutionChainId: Int? = null,
    val minLevel: Int? = null,
    val triggerName: String? = null
)

@Serializable
data class PokemonListResponse(
    val pokemon: List<PokemonSummary>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)
