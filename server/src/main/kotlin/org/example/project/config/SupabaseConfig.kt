package org.example.project.config

import java.io.File
import java.io.FileInputStream
import java.util.Properties

object SupabaseConfig {
    val dbUrl: String by lazy { getEnv("SUPABASE_DB_URL") }
    val dbUser: String by lazy { getEnv("SUPABASE_DB_USER") }
    val dbPassword: String by lazy { getEnv("SUPABASE_DB_PASSWORD") }
    val dbPoolSize: Int by lazy { getEnv("DB_POOL_SIZE", "10").toInt() }
    val dbMaxLifetimeMs: Long by lazy { getEnv("DB_MAX_LIFETIME_MS", "1800000").toLong() }

    private val envProps: Properties by lazy {
        val props = Properties()
        val envFile = File(".env")
        val serverEnvFile = File("server/.env")
        val file = when {
            envFile.exists() -> envFile
            serverEnvFile.exists() -> serverEnvFile
            else -> null
        }
        file?.let {
            FileInputStream(it).use { stream -> props.load(stream) }
        }
        props
    }

    private fun getEnv(key: String, default: String? = null): String {
        System.getenv(key)?.let { return it }
        envProps.getProperty(key)?.let { return it }
        return requireNotNull(default) { "Missing required environment variable: $key" }
    }
}
