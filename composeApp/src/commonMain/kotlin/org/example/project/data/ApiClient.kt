package org.example.project.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val baseUrl: String) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = client.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("username" to username, "password" to password))
            }
            val apiResponse = response.body<ApiResponse<LoginResponse>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPokemon(
        token: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<PokemonListData> {
        return try {
            val response = client.get("$baseUrl/api/pokemon") {
                bearerAuth(token)
                parameter("page", page)
                parameter("pageSize", pageSize)
            }
            val apiResponse = response.body<ApiResponse<PokemonListData>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Failed to fetch Pokémon"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPokemonDetail(token: String, id: Int): Result<PokemonDetail> {
        return try {
            val response = client.get("$baseUrl/api/pokemon/$id") {
                bearerAuth(token)
            }
            val apiResponse = response.body<ApiResponse<PokemonDetail>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Pokémon not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPokemon(
        token: String,
        query: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<PokemonListData> {
        return try {
            val response = client.get("$baseUrl/api/pokemon/search") {
                bearerAuth(token)
                parameter("q", query)
                parameter("page", page)
                parameter("pageSize", pageSize)
            }
            val apiResponse = response.body<ApiResponse<PokemonListData>>()
            if (apiResponse.success && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse.message ?: "Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
