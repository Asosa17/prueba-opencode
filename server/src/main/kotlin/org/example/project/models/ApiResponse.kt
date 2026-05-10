package org.example.project.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = true,
    val error: Boolean = false,
    val message: String? = null,
    val data: T? = null
) {
    companion object {
        fun <T> ok(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun error(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, error = true, message = message)
    }
}
