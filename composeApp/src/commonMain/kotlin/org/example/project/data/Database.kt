package org.example.project.data

interface Database {
    suspend fun saveSession(username: String, token: String)
    suspend fun loadSession(): Pair<String, String>?
    suspend fun clearSession()
    suspend fun savePokemonList(data: PokemonListData)
    suspend fun loadPokemonList(): PokemonListData?
    suspend fun savePokemonDetail(pokemon: PokemonDetail)
    suspend fun loadPokemonDetail(id: Int): PokemonDetail?
    suspend fun clear()
}
