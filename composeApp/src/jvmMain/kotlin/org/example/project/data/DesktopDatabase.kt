package org.example.project.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class DesktopDatabase : Database {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val dir = File(System.getProperty("user.home"), ".pokemon-app").also { it.mkdirs() }
    private val sessionFile = File(dir, "session.json")
    private val pokemonListFile = File(dir, "pokemon-list.json")
    private val pokemonDetailDir = File(dir, "details").also { it.mkdirs() }

    override suspend fun saveSession(username: String, token: String) {
        sessionFile.writeText(json.encodeToString(mapOf("username" to username, "token" to token)))
    }

    override suspend fun loadSession(): Pair<String, String>? {
        if (!sessionFile.exists()) return null
        val map = json.decodeFromString<Map<String, String>>(sessionFile.readText())
        return map["username"]?.let { u -> map["token"]?.let { t -> u to t } }
    }

    override suspend fun clearSession() {
        sessionFile.delete()
    }

    override suspend fun savePokemonList(data: PokemonListData) {
        pokemonListFile.writeText(json.encodeToString(data))
    }

    override suspend fun loadPokemonList(): PokemonListData? {
        if (!pokemonListFile.exists()) return null
        return json.decodeFromString<PokemonListData>(pokemonListFile.readText())
    }

    override suspend fun savePokemonDetail(pokemon: PokemonDetail) {
        File(pokemonDetailDir, "${pokemon.id}.json").writeText(json.encodeToString(pokemon))
    }

    override suspend fun loadPokemonDetail(id: Int): PokemonDetail? {
        val file = File(pokemonDetailDir, "$id.json")
        if (!file.exists()) return null
        return json.decodeFromString<PokemonDetail>(file.readText())
    }

    override suspend fun clear() {
        pokemonListFile.delete()
        pokemonDetailDir.listFiles()?.forEach { it.delete() }
    }
}
