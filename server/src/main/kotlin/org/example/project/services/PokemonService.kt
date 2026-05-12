package org.example.project.services

import org.example.project.database.DatabaseFactory
import org.example.project.models.*
import java.sql.ResultSet

class PokemonService {
    private val dataSource = DatabaseFactory.getDataSource()

    fun listPokemon(page: Int = 1, pageSize: Int = 20, filterMode: FilterMode = FilterMode.ANY_SLOT, typeId: Int? = null): PokemonListResponse {
        val offset = (page - 1) * pageSize
        val total = when {
            filterMode == FilterMode.NONE || typeId == null -> queryTotal()
            filterMode == FilterMode.SLOT_1 -> queryTotalByTypeSlot1(typeId)
            else -> queryTotalByType(typeId)
        }
        val pokemon = when {
            filterMode == FilterMode.NONE || typeId == null -> queryPokemonList(pageSize, offset)
            filterMode == FilterMode.SLOT_1 -> queryPokemonListByTypeSlot1(typeId, pageSize, offset)
            else -> queryPokemonListByType(typeId, pageSize, offset)
        }
        return PokemonListResponse(pokemon, total, page, pageSize)
    }

    fun getPokemonById(id: Int, filterMode: FilterMode = FilterMode.ANY_SLOT, typeId: Int? = null): PokemonDetail? {
        val pokemon = queryPokemonById(id) ?: return null
        val types = queryTypesByPokemonId(id)
        when {
            filterMode == FilterMode.NONE || typeId == null -> { /* no filter */ }
            filterMode == FilterMode.SLOT_1 -> {
                if (types.none { it.id == typeId && it.slot == 1 }) return null
            }
            else -> {
                if (types.none { it.id == typeId }) return null
            }
        }
        val abilities = queryAbilitiesByPokemonId(id)
        val stats = queryStatsByPokemonId(id)
        val sprites = querySpritesByPokemonId(id)
        val evolutions = queryEvolutionsByPokemonId(id)
        return pokemon.copy(
            types = types,
            abilities = abilities,
            stats = stats,
            sprites = sprites,
            evolutions = evolutions
        )
    }

    fun searchPokemon(query: String, page: Int = 1, pageSize: Int = 20, filterMode: FilterMode = FilterMode.ANY_SLOT, typeId: Int? = null): PokemonListResponse {
        val offset = (page - 1) * pageSize
        val pattern = "%${query.lowercase()}%"
        val total = when {
            filterMode == FilterMode.NONE || typeId == null -> querySearchTotal(pattern)
            filterMode == FilterMode.SLOT_1 -> querySearchTotalByTypeSlot1(pattern, typeId)
            else -> querySearchTotalByType(pattern, typeId)
        }
        val pokemon = when {
            filterMode == FilterMode.NONE || typeId == null -> queryPokemonSearch(pattern, pageSize, offset)
            filterMode == FilterMode.SLOT_1 -> queryPokemonSearchByTypeSlot1(pattern, typeId, pageSize, offset)
            else -> queryPokemonSearchByType(pattern, typeId, pageSize, offset)
        }
        return PokemonListResponse(pokemon, total, page, pageSize)
    }

    fun listTypes(): List<TypeInfo> {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, name FROM types ORDER BY id").use { stmt ->
                return stmt.executeQuery().toList { TypeInfo(id = getInt("id"), name = getString("name")) }
            }
        }
    }

    fun listAbilities(): List<AbilityInfo> {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, name FROM abilities ORDER BY id").use { stmt ->
                return stmt.executeQuery().toList { AbilityInfo(id = getInt("id"), name = getString("name")) }
            }
        }
    }

    private fun queryTotal(): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM pokemon").use { stmt ->
                stmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    private fun querySearchTotal(pattern: String): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM pokemon WHERE LOWER(name) LIKE ?").use { stmt ->
                stmt.setString(1, pattern)
                stmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    private fun queryPokemonList(limit: Int, offset: Int): List<PokemonSummary> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT p.id, p.name, p.height, p.weight, p.base_experience, s.artwork
                FROM pokemon p
                LEFT JOIN sprites s ON p.id = s.pokemon_id
                ORDER BY p.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
                val rows = stmt.executeQuery().toList {
                    PokemonSummary(
                        id = getInt("id"),
                        name = getString("name"),
                        height = getInt("height").takeIf { !wasNull() },
                        weight = getInt("weight").takeIf { !wasNull() },
                        baseExperience = getInt("base_experience").takeIf { !wasNull() },
                        sprite = getString("artwork")
                    )
                }
                if (rows.isNotEmpty()) {
                    val typeMap = batchQueryTypes(rows.map { it.id })
                    return rows.map { it.copy(types = typeMap[it.id] ?: emptyList()) }
                }
                return rows
            }
        }
    }

    private fun queryPokemonById(id: Int): PokemonDetail? {
        dataSource.connection.use { conn ->
            val sql = "SELECT id, name, height, weight, base_experience, species_id FROM pokemon WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return PokemonDetail(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        height = rs.getInt("height").takeIf { !rs.wasNull() },
                        weight = rs.getInt("weight").takeIf { !rs.wasNull() },
                        baseExperience = rs.getInt("base_experience").takeIf { !rs.wasNull() },
                        speciesId = rs.getInt("species_id").takeIf { !rs.wasNull() }
                    )
                }
            }
        }
    }

    private fun queryTypesByPokemonId(pokemonId: Int): List<TypeInfo> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT t.id, t.name, pt.slot
                FROM types t
                JOIN pokemon_types pt ON t.id = pt.type_id
                WHERE pt.pokemon_id = ?
                ORDER BY pt.slot
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, pokemonId)
                return stmt.executeQuery().toList {
                    TypeInfo(id = getInt("id"), name = getString("name"), slot = getInt("slot").takeIf { !wasNull() })
                }
            }
        }
    }

    private fun batchQueryTypes(pokemonIds: List<Int>): Map<Int, List<TypeInfo>> {
        if (pokemonIds.isEmpty()) return emptyMap()
        val placeholders = pokemonIds.joinToString(",") { "?" }
        dataSource.connection.use { conn ->
            val sql = """
                SELECT pt.pokemon_id, t.id, t.name, pt.slot
                FROM pokemon_types pt
                JOIN types t ON t.id = pt.type_id
                WHERE pt.pokemon_id IN ($placeholders)
                ORDER BY pt.pokemon_id, pt.slot
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                pokemonIds.forEachIndexed { i, id -> stmt.setInt(i + 1, id) }
                val result = mutableMapOf<Int, MutableList<TypeInfo>>()
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val pid = rs.getInt("pokemon_id")
                        val type = TypeInfo(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            slot = rs.getInt("slot").takeIf { !rs.wasNull() }
                        )
                        result.getOrPut(pid) { mutableListOf() }.add(type)
                    }
                }
                return result
            }
        }
    }

    private fun queryAbilitiesByPokemonId(pokemonId: Int): List<AbilityInfo> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT a.id, a.name, pa.is_hidden, pa.slot
                FROM abilities a
                JOIN pokemon_abilities pa ON a.id = pa.ability_id
                WHERE pa.pokemon_id = ?
                ORDER BY pa.slot
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, pokemonId)
                return stmt.executeQuery().toList {
                    AbilityInfo(
                        id = getInt("id"),
                        name = getString("name"),
                        isHidden = getBoolean("is_hidden"),
                        slot = getInt("slot").takeIf { !wasNull() }
                    )
                }
            }
        }
    }

    private fun queryStatsByPokemonId(pokemonId: Int): StatsInfo? {
        dataSource.connection.use { conn ->
            val sql = "SELECT hp, attack, defense, special_attack, special_defense, speed FROM stats WHERE pokemon_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, pokemonId)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return StatsInfo(
                        hp = rs.getInt("hp"),
                        attack = rs.getInt("attack"),
                        defense = rs.getInt("defense"),
                        specialAttack = rs.getInt("special_attack"),
                        specialDefense = rs.getInt("special_defense"),
                        speed = rs.getInt("speed")
                    )
                }
            }
        }
    }

    private fun querySpritesByPokemonId(pokemonId: Int): SpritesInfo? {
        dataSource.connection.use { conn ->
            val sql = "SELECT front_default, front_shiny, back_default, back_shiny, artwork FROM sprites WHERE pokemon_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, pokemonId)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return SpritesInfo(
                        frontDefault = rs.getString("front_default"),
                        frontShiny = rs.getString("front_shiny"),
                        backDefault = rs.getString("back_default"),
                        backShiny = rs.getString("back_shiny"),
                        artwork = rs.getString("artwork")
                    )
                }
            }
        }
    }

    private fun queryEvolutionsByPokemonId(pokemonId: Int): List<EvolutionInfo> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT pokemon_id, evolves_from_id, evolution_chain_id, min_level, trigger_name
                FROM pokemon_evolutions
                WHERE pokemon_id = ? OR evolves_from_id = ?
                ORDER BY evolution_chain_id, pokemon_id
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, pokemonId)
                stmt.setInt(2, pokemonId)
                return stmt.executeQuery().toList {
                    EvolutionInfo(
                        pokemonId = getInt("pokemon_id"),
                        evolvesFromId = getInt("evolves_from_id").takeIf { !wasNull() },
                        evolutionChainId = getInt("evolution_chain_id").takeIf { !wasNull() },
                        minLevel = getInt("min_level").takeIf { !wasNull() },
                        triggerName = getString("trigger_name")
                    )
                }
            }
        }
    }

    private fun queryPokemonSearch(pattern: String, limit: Int, offset: Int): List<PokemonSummary> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT p.id, p.name, p.height, p.weight, p.base_experience, s.artwork
                FROM pokemon p
                LEFT JOIN sprites s ON p.id = s.pokemon_id
                WHERE LOWER(p.name) LIKE ?
                ORDER BY p.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, pattern)
                stmt.setInt(2, limit)
                stmt.setInt(3, offset)
                val rows = stmt.executeQuery().toList {
                    PokemonSummary(
                        id = getInt("id"),
                        name = getString("name"),
                        height = getInt("height").takeIf { !wasNull() },
                        weight = getInt("weight").takeIf { !wasNull() },
                        baseExperience = getInt("base_experience").takeIf { !wasNull() },
                        sprite = getString("artwork")
                    )
                }
                if (rows.isNotEmpty()) {
                    val typeMap = batchQueryTypes(rows.map { it.id })
                    return rows.map { it.copy(types = typeMap[it.id] ?: emptyList()) }
                }
                return rows
            }
        }
    }

    private fun queryTotalByType(typeId: Int): Int {
        dataSource.connection.use { conn ->
            val sql = "SELECT COUNT(*) FROM pokemon_types WHERE type_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, typeId)
                stmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    private fun querySearchTotalByType(pattern: String, typeId: Int): Int {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT COUNT(*) FROM pokemon p
                JOIN pokemon_types pt ON p.id = pt.pokemon_id
                WHERE LOWER(p.name) LIKE ? AND pt.type_id = ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, pattern)
                stmt.setInt(2, typeId)
                stmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    private fun queryPokemonListByType(typeId: Int, limit: Int, offset: Int): List<PokemonSummary> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT DISTINCT p.id, p.name, p.height, p.weight, p.base_experience, s.artwork
                FROM pokemon p
                JOIN pokemon_types pt ON p.id = pt.pokemon_id
                LEFT JOIN sprites s ON p.id = s.pokemon_id
                WHERE pt.type_id = ?
                ORDER BY p.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, typeId)
                stmt.setInt(2, limit)
                stmt.setInt(3, offset)
                val rows = stmt.executeQuery().toList {
                    PokemonSummary(
                        id = getInt("id"),
                        name = getString("name"),
                        height = getInt("height").takeIf { !wasNull() },
                        weight = getInt("weight").takeIf { !wasNull() },
                        baseExperience = getInt("base_experience").takeIf { !wasNull() },
                        sprite = getString("artwork")
                    )
                }
                if (rows.isNotEmpty()) {
                    val typeMap = batchQueryTypes(rows.map { it.id })
                    return rows.map { it.copy(types = typeMap[it.id] ?: emptyList()) }
                }
                return rows
            }
        }
    }

    private fun queryPokemonSearchByType(pattern: String, typeId: Int, limit: Int, offset: Int): List<PokemonSummary> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT DISTINCT p.id, p.name, p.height, p.weight, p.base_experience, s.artwork
                FROM pokemon p
                JOIN pokemon_types pt ON p.id = pt.pokemon_id
                LEFT JOIN sprites s ON p.id = s.pokemon_id
                WHERE LOWER(p.name) LIKE ? AND pt.type_id = ?
                ORDER BY p.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, pattern)
                stmt.setInt(2, typeId)
                stmt.setInt(3, limit)
                stmt.setInt(4, offset)
                val rows = stmt.executeQuery().toList {
                    PokemonSummary(
                        id = getInt("id"),
                        name = getString("name"),
                        height = getInt("height").takeIf { !wasNull() },
                        weight = getInt("weight").takeIf { !wasNull() },
                        baseExperience = getInt("base_experience").takeIf { !wasNull() },
                        sprite = getString("artwork")
                    )
                }
                if (rows.isNotEmpty()) {
                    val typeMap = batchQueryTypes(rows.map { it.id })
                    return rows.map { it.copy(types = typeMap[it.id] ?: emptyList()) }
                }
                return rows
            }
        }
    }

    // ─── Slot 1 queries (novice role) ─────────────────────────────────────────

    private fun queryTotalByTypeSlot1(typeId: Int): Int {
        dataSource.connection.use { conn ->
            val sql = "SELECT COUNT(*) FROM pokemon_types WHERE type_id = ? AND slot = 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, typeId)
                stmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    private fun queryPokemonListByTypeSlot1(typeId: Int, limit: Int, offset: Int): List<PokemonSummary> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT DISTINCT p.id, p.name, p.height, p.weight, p.base_experience, s.artwork
                FROM pokemon p
                JOIN pokemon_types pt ON p.id = pt.pokemon_id
                LEFT JOIN sprites s ON p.id = s.pokemon_id
                WHERE pt.type_id = ? AND pt.slot = 1
                ORDER BY p.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, typeId)
                stmt.setInt(2, limit)
                stmt.setInt(3, offset)
                val rows = stmt.executeQuery().toList {
                    PokemonSummary(
                        id = getInt("id"),
                        name = getString("name"),
                        height = getInt("height").takeIf { !wasNull() },
                        weight = getInt("weight").takeIf { !wasNull() },
                        baseExperience = getInt("base_experience").takeIf { !wasNull() },
                        sprite = getString("artwork")
                    )
                }
                if (rows.isNotEmpty()) {
                    val typeMap = batchQueryTypes(rows.map { it.id })
                    return rows.map { it.copy(types = typeMap[it.id] ?: emptyList()) }
                }
                return rows
            }
        }
    }

    private fun querySearchTotalByTypeSlot1(pattern: String, typeId: Int): Int {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT COUNT(*) FROM pokemon p
                JOIN pokemon_types pt ON p.id = pt.pokemon_id
                WHERE LOWER(p.name) LIKE ? AND pt.type_id = ? AND pt.slot = 1
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, pattern)
                stmt.setInt(2, typeId)
                stmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    private fun queryPokemonSearchByTypeSlot1(pattern: String, typeId: Int, limit: Int, offset: Int): List<PokemonSummary> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT DISTINCT p.id, p.name, p.height, p.weight, p.base_experience, s.artwork
                FROM pokemon p
                JOIN pokemon_types pt ON p.id = pt.pokemon_id
                LEFT JOIN sprites s ON p.id = s.pokemon_id
                WHERE LOWER(p.name) LIKE ? AND pt.type_id = ? AND pt.slot = 1
                ORDER BY p.id
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, pattern)
                stmt.setInt(2, typeId)
                stmt.setInt(3, limit)
                stmt.setInt(4, offset)
                val rows = stmt.executeQuery().toList {
                    PokemonSummary(
                        id = getInt("id"),
                        name = getString("name"),
                        height = getInt("height").takeIf { !wasNull() },
                        weight = getInt("weight").takeIf { !wasNull() },
                        baseExperience = getInt("base_experience").takeIf { !wasNull() },
                        sprite = getString("artwork")
                    )
                }
                if (rows.isNotEmpty()) {
                    val typeMap = batchQueryTypes(rows.map { it.id })
                    return rows.map { it.copy(types = typeMap[it.id] ?: emptyList()) }
                }
                return rows
            }
        }
    }

    private inline fun <T> ResultSet.toList(transform: ResultSet.() -> T): List<T> {
        val result = mutableListOf<T>()
        while (next()) {
            result.add(transform(this))
        }
        return result
    }
}
