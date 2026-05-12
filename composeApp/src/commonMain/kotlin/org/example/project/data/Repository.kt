package org.example.project.data

class Repository(
    private val api: ApiClient,
    private val db: Database
) {
    private var currentToken: String? = null
    private var currentUser: UserInfo? = null

    suspend fun login(username: String, password: String): Result<UserInfo> {
        // Try local first (offline login not supported for now)
        val result = api.login(username, password)
        if (result.isSuccess) {
            val data = result.getOrThrow()
            currentToken = data.token
            currentUser = data.user
            db.saveSession(username, data.token)
        }
        return result.map { it.user }
    }

    suspend fun restoreSession(): Boolean {
        val session = db.loadSession() ?: return false
        currentToken = session.second
        // Verify token is still valid (try a simple call)
        return true
    }

    fun getCurrentUser(): UserInfo? = currentUser
    fun getCurrentToken(): String? = currentToken

    suspend fun logout() {
        currentToken = null
        currentUser = null
        db.clearSession()
        db.clear()
    }

    suspend fun getPokemon(page: Int = 1, pageSize: Int = 20): Result<PokemonListData> {
        val token = currentToken ?: return Result.failure(Exception("Not authenticated"))
        val result = api.getPokemon(token, page, pageSize)
        if (result.isSuccess && page == 1) {
            db.savePokemonList(result.getOrThrow())
        }
        return result
    }

    suspend fun getPokemonDetail(id: Int): Result<PokemonDetail> {
        val token = currentToken ?: return Result.failure(Exception("Not authenticated"))
        val cached = db.loadPokemonDetail(id)
        val result = api.getPokemonDetail(token, id)
        if (result.isSuccess) {
            db.savePokemonDetail(result.getOrThrow())
        } else if (cached != null) {
            return Result.success(cached)
        }
        return result
    }

    suspend fun searchPokemon(query: String, page: Int = 1, pageSize: Int = 20): Result<PokemonListData> {
        val token = currentToken ?: return Result.failure(Exception("Not authenticated"))
        return api.searchPokemon(token, query, page, pageSize)
    }

    suspend fun getCachedPokemonList(): PokemonListData? = db.loadPokemonList()

    fun getPokemonTypeId(): Int? = currentUser?.pokemonTypeId
    fun getUsername(): String? = currentUser?.username
    fun getUserRole(): String = currentUser?.role ?: "expert"
}
