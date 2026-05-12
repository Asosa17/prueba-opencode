package org.example.project.services

import at.favre.lib.crypto.bcrypt.BCrypt
import org.example.project.database.DatabaseFactory
import org.example.project.models.UserPrincipal

class AuthService {
    private val dataSource = DatabaseFactory.getDataSource()

    fun findByUsername(username: String): UserPrincipal? {
        dataSource.connection.use { conn ->
            val sql = "SELECT id, username, password_hash, pokemon_type_id, role FROM users WHERE LOWER(username) = LOWER(?)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, username)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return UserPrincipal(
                        id = rs.getInt("id"),
                        username = rs.getString("username"),
                        pokemonTypeId = rs.getInt("pokemon_type_id").takeIf { !rs.wasNull() },
                        role = rs.getString("role") ?: "user"
                    )
                }
            }
        }
    }

    fun findByUsernameWithHash(username: String): Pair<UserPrincipal, String>? {
        dataSource.connection.use { conn ->
            val sql = "SELECT id, username, password_hash, pokemon_type_id, role FROM users WHERE LOWER(username) = LOWER(?)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, username)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    val user = UserPrincipal(
                        id = rs.getInt("id"),
                        username = rs.getString("username"),
                        pokemonTypeId = rs.getInt("pokemon_type_id").takeIf { !rs.wasNull() },
                        role = rs.getString("role") ?: "user"
                    )
                    return Pair(user, rs.getString("password_hash"))
                }
            }
        }
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
